/**
 *    Copyright 2018 the original author or authors.
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

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.session.Configuration;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@code SqlSource} for integrating with Thymeleaf.
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
class ThymeleafSqlSource implements SqlSource {

  private final Configuration configuration;
  private final ITemplateEngine templateEngine;
  private final AtomicReference<MetaClass> parameterTypeMetaClass = new AtomicReference<>();
  private final SqlSourceBuilder sqlSourceBuilder;
  private final String sqlTemplate;

  /**
   * Constructor for for integrating with template engine provide by Thymeleaf.
   * @param configuration A configuration instance of MyBatis
   * @param templateEngine A template engine provide by Thymeleaf
   * @param sqlTemplate A template string of SQL (inline SQL or template file path)
   */
  ThymeleafSqlSource(Configuration configuration, ITemplateEngine templateEngine, String sqlTemplate) {
    this.configuration = configuration;
    this.templateEngine = templateEngine;
    this.sqlTemplate = sqlTemplate;
    this.sqlSourceBuilder = new SqlSourceBuilder(configuration);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    DynamicContext dynamicContext = new DynamicContext(configuration, parameterObject);
    IContext context;
    if (parameterObject instanceof Map) {
      @SuppressWarnings(value = "unchecked")
      Map<String, Object> parameterMap = (Map<String, Object>) parameterObject;
      parameterMap.putAll(dynamicContext.getBindings());
      context = new Context(Locale.getDefault(), parameterMap);
    } else {
      context = createMetaClassBasedContext(parameterObject, parameterType, dynamicContext);
    }

    String sql = templateEngine.process(sqlTemplate, context);

    System.out.println(sql);

    SqlSource sqlSource = sqlSourceBuilder.parse(sql, parameterType, dynamicContext.getBindings());
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    dynamicContext.getBindings().forEach(boundSql::setAdditionalParameter);
    return boundSql;
  }

  private IContext createMetaClassBasedContext(Object parameterObject, Class<?> parameterType, DynamicContext dynamicContext) {
    MetaClass metaClass = parameterTypeMetaClass.updateAndGet(
        v -> v == null ? MetaClass.forClass(parameterType, configuration.getReflectorFactory()) : v);
    return new MetaClassBasedThymeleafContext(parameterObject, metaClass, parameterType, dynamicContext);
  }

  private static class MetaClassBasedThymeleafContext implements IContext {

    private final Object parameterObject;
    private final MetaClass parameterMetaClass;
    private final Class<?> parameterType;
    private final DynamicContext dynamicContext;
    private final Set<String> variableNames;

    private MetaClassBasedThymeleafContext(Object parameterObject, MetaClass parameterMetaClass, Class<?> parameterType, DynamicContext dynamicContext) {
      this.parameterObject = parameterObject;
      this.parameterMetaClass = parameterMetaClass;
      this.parameterType = parameterType;
      this.dynamicContext = dynamicContext;
      this.variableNames = new HashSet<>(Arrays.asList(parameterMetaClass.getGetterNames()));
      variableNames.addAll(dynamicContext.getBindings().keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Locale getLocale() {
      return Locale.getDefault();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsVariable(String name) {
      return variableNames.contains(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getVariableNames() {
      return variableNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getVariable(String name) {
      if (dynamicContext.getBindings().containsKey(name)) {
        return dynamicContext.getBindings().get(name);
      }
      try {
        return parameterMetaClass.getGetInvoker(name).invoke(parameterObject, null);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new IllegalStateException(String.format("Cannot get a value for property named '%s' in '%s'", name, parameterType), e);
      }
    }

  }

}
