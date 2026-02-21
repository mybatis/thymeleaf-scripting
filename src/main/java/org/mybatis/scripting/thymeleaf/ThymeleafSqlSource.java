/*
 *    Copyright 2018-2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.scripting.thymeleaf;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.ibatis.builder.ParameterMappingTokenHandler;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.session.Configuration;
import org.thymeleaf.context.IContext;

/**
 * The {@code SqlSource} for integrating with Thymeleaf.
 *
 * @author Kazuki Shimizu
 *
 * @version 1.0.0
 *
 * @see ThymeleafLanguageDriver
 */
class ThymeleafSqlSource implements SqlSource {

  private static class TemporaryTakeoverKeys {
    private static final String CONFIGURATION = "__configuration__";
    private static final String DYNAMIC_CONTEXT = "__dynamicContext__";
    private static final String PROCESSING_PARAMETER_TYPE = "__processingParameterType__";
  }

  private final Configuration configuration;
  private final SqlGenerator sqlGenerator;
  private final String sqlTemplate;
  private final Class<?> parameterType;

  /**
   * Constructor for for integrating with template engine provide by Thymeleaf.
   *
   * @param configuration
   *          A configuration instance of MyBatis
   * @param sqlGenerator
   *          A sql generator using the Thymeleaf feature
   * @param sqlTemplate
   *          A template string of SQL (inline SQL or template file path)
   * @param parameterType
   *          A parameter type that specified at mapper method argument or xml element
   */
  ThymeleafSqlSource(Configuration configuration, SqlGenerator sqlGenerator, String sqlTemplate,
      Class<?> parameterType) {
    this.configuration = configuration;
    this.sqlGenerator = sqlGenerator;
    this.sqlTemplate = sqlTemplate;
    this.parameterType = parameterType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    Class<?> processingParameterType;
    if (parameterType == null) {
      processingParameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    } else {
      processingParameterType = parameterType;
    }

    Map<String, Object> bindings = new HashMap<>();
    bindings.put(DynamicContext.PARAMETER_OBJECT_KEY, parameterObject);
    bindings.put(DynamicContext.DATABASE_ID_KEY, configuration.getDatabaseId());

    Map<String, Object> customVariables = bindings;
    customVariables.put(TemporaryTakeoverKeys.CONFIGURATION, configuration);
    customVariables.put(TemporaryTakeoverKeys.DYNAMIC_CONTEXT, bindings);
    customVariables.put(TemporaryTakeoverKeys.PROCESSING_PARAMETER_TYPE, processingParameterType);
    String sql = sqlGenerator.generate(sqlTemplate, parameterObject, bindings::put, customVariables);

    SqlSource sqlSource = parse(configuration, sql, parameterObject, bindings);
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    bindings.forEach(boundSql::setAdditionalParameter);

    return boundSql;
  }

  private static SqlSource parse(Configuration configuration, String originalSql, Object parameterObject,
      Map<String, Object> additionalParameters) {
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    List<ParameterMapping> parameterMappings = new ArrayList<>();
    ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(parameterMappings, configuration,
        parameterObject, parameterType, additionalParameters, true);
    GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
    return SqlSourceBuilder.buildSqlSource(configuration, parser.parse(originalSql), parameterMappings);
  }

  /**
   * The factory class for Thymeleaf's context.
   *
   * @since 1.0.2
   */
  static class ContextFactory implements BiFunction<Object, Map<String, Object>, IContext> {
    /**
     * {@inheritDoc}
     */
    @Override
    public IContext apply(Object parameter, Map<String, Object> customVariable) {
      Configuration configuration = (Configuration) customVariable.remove(TemporaryTakeoverKeys.CONFIGURATION);
      Map<String, Object> bindings = (Map<String, Object>) customVariable.remove(TemporaryTakeoverKeys.DYNAMIC_CONTEXT);
      Class<?> processingParameterType = (Class<?>) customVariable
          .remove(TemporaryTakeoverKeys.PROCESSING_PARAMETER_TYPE);
      MyBatisBindingContext bindingContext = new MyBatisBindingContext(
          parameter != null && configuration.getTypeHandlerRegistry().hasTypeHandler(processingParameterType));
      bindings.put(MyBatisBindingContext.CONTEXT_VARIABLE_NAME, bindingContext);
      IContext context;
      if (parameter instanceof Map) {
        @SuppressWarnings(value = "unchecked")
        Map<String, Object> map = (Map<String, Object>) parameter;
        context = new MapBasedContext(map, bindings, configuration.getVariables());
      } else {
        MetaClass metaClass = MetaClass.forClass(processingParameterType, configuration.getReflectorFactory());
        context = new MetaClassBasedContext(parameter, metaClass, processingParameterType, bindings,
            configuration.getVariables());
      }
      return context;
    }
  }

  private abstract static class AbstractContext implements IContext {

    private final Map<String, Object> dynamicContext;
    private final Properties configurationProperties;
    private final Set<String> variableNames;

    private AbstractContext(Map<String, Object> dynamicContext, Properties configurationProperties) {
      this.dynamicContext = dynamicContext;
      this.configurationProperties = configurationProperties;
      this.variableNames = new HashSet<>();
      addVariableNames(dynamicContext.keySet());
      Optional.ofNullable(configurationProperties).ifPresent(v -> addVariableNames(v.stringPropertyNames()));
    }

    void addVariableNames(Collection<String> names) {
      variableNames.addAll(names);
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
      if (dynamicContext.containsKey(name)) {
        return dynamicContext.get(name);
      }
      if (configurationProperties != null && configurationProperties.containsKey(name)) {
        return configurationProperties.getProperty(name);
      }
      return getParameterValue(name);
    }

    abstract Object getParameterValue(String name);

  }

  private static class MapBasedContext extends AbstractContext {

    private final Map<String, Object> variables;

    private MapBasedContext(Map<String, Object> parameterMap, Map<String, Object> dynamicContext,
        Properties configurationProperties) {
      super(dynamicContext, configurationProperties);
      this.variables = parameterMap;
      addVariableNames(parameterMap.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getParameterValue(String name) {
      return variables.get(name);
    }

  }

  private static class MetaClassBasedContext extends AbstractContext {

    private final Object parameterObject;
    private final MetaClass parameterMetaClass;
    private final Class<?> parameterType;

    private MetaClassBasedContext(Object parameterObject, MetaClass parameterMetaClass, Class<?> parameterType,
        Map<String, Object> dynamicContext, Properties configurationProperties) {
      super(dynamicContext, configurationProperties);
      this.parameterObject = parameterObject;
      this.parameterMetaClass = parameterMetaClass;
      this.parameterType = parameterType;
      addVariableNames(Arrays.asList(parameterMetaClass.getGetterNames()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getParameterValue(String name) {
      try {
        return parameterMetaClass.getGetInvoker(name).invoke(parameterObject, null);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new IllegalStateException(
            String.format("Cannot get a value for property named '%s' in '%s'", name, parameterType), e);
      }
    }

  }

}
