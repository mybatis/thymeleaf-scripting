/*
 *    Copyright 2018-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.scripting.thymeleaf;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The interface for accessing a property. <br>
 * If you want to customize a default {@code PropertyAccessor}, you implements class of this interface and you need to
 * specify to a {@link SqlGenerator}. <br>
 *
 * @author Kazuki Shimizu
 * @version 1.0.2
 */
public interface PropertyAccessor {

  /**
   * Get property names of specified type.
   *
   * @param type
   *          a target type
   * @return property names
   */
  Set<String> getPropertyNames(Class<?> type);

  /**
   * Get a property type of specified property.
   *
   * @param type
   *          a target type
   * @param name
   *          a property name
   * @return a property type
   */
  Class<?> getPropertyType(Class<?> type, String name);

  /**
   * Get a property value from specified target object.
   *
   * @param target
   *          a target object
   * @param name
   *          a property name
   * @return a property value
   */
  Object getPropertyValue(Object target, String name);

  /**
   * Set a property value to the specified target object.
   *
   * @param target
   *          a target object
   * @param name
   *          a property name
   * @param value
   *          a property value
   */
  void setPropertyValue(Object target, String name, Object value);

  /**
   * The built-in property accessors.
   */
  enum BuiltIn implements PropertyAccessor {

    /**
     * The implementation using Java Beans API provided by JDK.
     */
    STANDARD(new StandardPropertyAccessor());

    private final PropertyAccessor delegate;

    BuiltIn(PropertyAccessor delegate) {
      this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getPropertyNames(Class<?> type) {
      return delegate.getPropertyNames(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getPropertyType(Class<?> type, String name) {
      return delegate.getPropertyType(type, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getPropertyValue(Object target, String name) {
      return delegate.getPropertyValue(target, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPropertyValue(Object target, String name, Object value) {
      delegate.setPropertyValue(target, name, value);
    }

    static class StandardPropertyAccessor implements PropertyAccessor {

      private static Map<Class<?>, Map<String, PropertyDescriptor>> cache = new ConcurrentHashMap<>();

      /**
       * {@inheritDoc}
       */
      @Override
      public Set<String> getPropertyNames(Class<?> type) {
        return getPropertyDescriptors(type).keySet();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Class<?> getPropertyType(Class<?> type, String name) {
        return Optional.ofNullable(getPropertyDescriptors(type).get(name))
            .orElseThrow(() -> new IllegalArgumentException(String.format(
                "Does not get a property type because property '%s' not found on '%s' class.", name, type.getName())))
            .getPropertyType();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Object getPropertyValue(Object target, String name) {
        try {
          return Optional.ofNullable(getPropertyDescriptors(target.getClass()).get(name))
              .map(PropertyDescriptor::getReadMethod)
              .orElseThrow(() -> new IllegalArgumentException(
                  String.format("Does not get a property value because property '%s' not found on '%s' class.", name,
                      target.getClass().getName())))
              .invoke(target);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new IllegalStateException(e);
        }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void setPropertyValue(Object target, String name, Object value) {
        try {
          Optional.ofNullable(getPropertyDescriptors(target.getClass()).get(name))
              .map(PropertyDescriptor::getWriteMethod)
              .orElseThrow(() -> new IllegalArgumentException(
                  String.format("Does not set a property value because property '%s' not found on '%s' class.", name,
                      target.getClass().getName())))
              .invoke(target, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new IllegalStateException(e);
        }
      }

      /**
       * Clear cache.
       * <p>
       * This method use by internal processing.
       * </p>
       */
      static void clearCache() {
        cache.clear();
      }

      private static Map<String, PropertyDescriptor> getPropertyDescriptors(Class<?> type) {
        return cache.computeIfAbsent(type, key -> {
          try {
            BeanInfo beanInfo = Introspector.getBeanInfo(type);
            return Stream.of(beanInfo.getPropertyDescriptors()).filter(x -> !x.getName().equals("class"))
                .collect(Collectors.toMap(PropertyDescriptor::getName, v -> v));
          } catch (IntrospectionException e) {
            throw new IllegalStateException(e);
          } finally {
            Introspector.flushFromCaches(type);
          }
        });
      }

    }

  }

}
