/*
 *    Copyright 2018-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.mybatis.scripting.thymeleaf.expression.Likes;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * The sql template engine for integrating with Thymeleaf.
 *
 * @author Kazuki Shimizu
 * @version 1.0.2
 */
public class SqlGenerator {

  static class ContextKeys {
    static final String PARAMETER_OBJECT = "_parameter";
  }

  private final ITemplateEngine templateEngine;
  private Map<String, Object> defaultCustomVariables = Collections.emptyMap();
  private PropertyAccessor propertyAccessor = PropertyAccessor.BuiltIn.STANDARD;
  private BiFunction<Object, Map<String, Object>, IContext> contextFactory = DefaultContext::new;

  /**
   * Constructor for creating instance with default {@code TemplateEngine}.
   */
  public SqlGenerator() {
    this.templateEngine = createDefaultTemplateEngine(SqlGeneratorConfig.newInstance());
  }

  /**
   * Constructor for creating instance with user specified {@link SqlGenerator}.
   *
   * @param config
   *          A user defined {@link SqlGeneratorConfig} instance
   */
  public SqlGenerator(SqlGeneratorConfig config) {
    this.templateEngine = createDefaultTemplateEngine(config);
  }

  /**
   * Constructor for creating instance with user defined {@code ITemplateEngine}.
   *
   * @param templateEngine
   *          A user defined {@code ITemplateEngine} instance
   */
  public SqlGenerator(ITemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  /**
   * Set default custom variables.
   *
   * @param defaultCustomVariables
   *          a default custom variables for passing to template engine
   */
  public void setDefaultCustomVariables(Map<String, Object> defaultCustomVariables) {
    this.defaultCustomVariables = Optional.ofNullable(defaultCustomVariables).map(Collections::unmodifiableMap)
        .orElseGet(Collections::emptyMap);
  }

  /**
   * Get specified default custom variables.
   *
   * @return specified default custom variables
   */
  public Map<String, Object> getDefaultCustomVariables() {
    return defaultCustomVariables;
  }

  /**
   * Set a property accessor.
   * <p>
   * Default is {@link PropertyAccessor.BuiltIn#STANDARD}.
   * </p>
   *
   * @param propertyAccessor
   *          a property accessor
   */
  public void setPropertyAccessor(PropertyAccessor propertyAccessor) {
    this.propertyAccessor = Optional.ofNullable(propertyAccessor).orElse(PropertyAccessor.BuiltIn.STANDARD);
  }

  /**
   * Set a factory function for creating instance of custom context.
   *
   * @param contextFactory
   *          a factory function
   */
  void setContextFactory(BiFunction<Object, Map<String, Object>, IContext> contextFactory) {
    this.contextFactory = contextFactory;
  }

  private ITemplateEngine createDefaultTemplateEngine(SqlGeneratorConfig config) {
    MyBatisDialect dialect = new MyBatisDialect(config.getDialect().getPrefix());
    Optional.ofNullable(config.getDialect().getBindVariableRender()).map(SqlGeneratorConfig::newInstanceForType)
        .ifPresent(dialect::setBindVariableRender);
    Likes likes = Likes.newBuilder().escapeChar(config.getDialect().getLikeEscapeChar())
        .escapeClauseFormat(config.getDialect().getLikeEscapeClauseFormat())
        .additionalEscapeTargetChars(config.getDialect().getLikeAdditionalEscapeTargetChars()).build();
    dialect.setLikes(likes);

    // Create an ClassLoaderTemplateResolver instance
    ClassLoaderTemplateResolver classLoaderTemplateResolver = new ClassLoaderTemplateResolver();
    TemplateMode mode = config.isUse2way() ? TemplateMode.CSS : TemplateMode.TEXT;
    classLoaderTemplateResolver.setOrder(1);
    classLoaderTemplateResolver.setTemplateMode(mode);
    classLoaderTemplateResolver
        .setResolvablePatterns(Arrays.stream(config.getTemplateFile().getPatterns()).collect(Collectors.toSet()));
    classLoaderTemplateResolver.setCharacterEncoding(config.getTemplateFile().getEncoding().name());
    classLoaderTemplateResolver.setCacheable(config.getTemplateFile().isCacheEnabled());
    classLoaderTemplateResolver.setCacheTTLMs(config.getTemplateFile().getCacheTtl());
    classLoaderTemplateResolver.setPrefix(config.getTemplateFile().getBaseDir());

    // Create an StringTemplateResolver instance
    StringTemplateResolver stringTemplateResolver = new StringTemplateResolver();
    stringTemplateResolver.setOrder(2);
    stringTemplateResolver.setTemplateMode(mode);

    // Create an TemplateEngine instance
    TemplateEngine targetTemplateEngine = new TemplateEngine();
    targetTemplateEngine.addTemplateResolver(classLoaderTemplateResolver);
    targetTemplateEngine.addTemplateResolver(stringTemplateResolver);
    targetTemplateEngine.addDialect(dialect);
    targetTemplateEngine.setEngineContextFactory(
        new MyBatisIntegratingEngineContextFactory(targetTemplateEngine.getEngineContextFactory()));

    // Create an TemplateEngineCustomizer instance and apply
    Optional.ofNullable(config.getCustomizer()).map(SqlGeneratorConfig::newInstanceForType)
        .ifPresent(x -> x.accept(targetTemplateEngine));

    return targetTemplateEngine;
  }

  /**
   * Generate a sql using Thymeleaf template engine.
   *
   * @param sqlTemplate
   *          a template SQL
   * @param parameter
   *          a parameter object
   * @return a processed SQL by template engine
   */
  public String generate(CharSequence sqlTemplate, Object parameter) {
    return generate(sqlTemplate, parameter, null, null);
  }

  /**
   * Generate a sql using Thymeleaf template engine.
   *
   * @param sqlTemplate
   *          a template SQL
   * @param parameter
   *          a parameter object
   * @param customBindVariableBinder
   *          a binder for a custom bind variable that generated with {@code mb:bind} or {@code mb:param}
   * @return a processed SQL by template engine
   */
  public String generate(CharSequence sqlTemplate, Object parameter,
      BiConsumer<String, Object> customBindVariableBinder) {
    return generate(sqlTemplate, parameter, customBindVariableBinder, null);
  }

  /**
   * Generate a sql using Thymeleaf template engine.
   *
   * @param sqlTemplate
   *          a template SQL
   * @param parameter
   *          a parameter object
   * @param customVariables
   *          a custom variables for passing to template engine
   * @return a processed SQL by template engine
   */
  public String generate(CharSequence sqlTemplate, Object parameter, Map<String, Object> customVariables) {
    return generate(sqlTemplate, parameter, null, customVariables);
  }

  /**
   * Generate a sql using Thymeleaf template engine.
   *
   * @param sqlTemplate
   *          a template SQL
   * @param parameter
   *          a parameter object
   * @param customBindVariableBinder
   *          a binder for a custom bind variable that generated with {@code mb:bind} or {@code mb:param}
   * @param customVariables
   *          a custom variables for passing to template engine
   * @return a processed SQL by template engine
   */
  public String generate(CharSequence sqlTemplate, Object parameter,
      BiConsumer<String, Object> customBindVariableBinder, Map<String, Object> customVariables) {

    Map<String, Object> processingCustomVariables = new HashMap<>(defaultCustomVariables);
    Optional.ofNullable(customVariables).ifPresent(processingCustomVariables::putAll);

    IContext context = contextFactory.apply(parameter, processingCustomVariables);
    String sql = templateEngine.process(sqlTemplate.toString(), context);

    MyBatisBindingContext bindingContext = MyBatisBindingContext.load(context);
    if (bindingContext != null && customBindVariableBinder != null) {
      bindingContext.getCustomBindVariables().forEach(customBindVariableBinder);
    }

    return sql;
  }

  private class DefaultContext implements IContext {

    private final Object parameter;
    private final Map<String, Object> mapParameter;
    private final Set<String> propertyNames = new HashSet<>();
    private final Map<String, Object> customVariables;

    private DefaultContext(Object parameter, Map<String, Object> customVariables) {
      this.parameter = parameter;
      boolean fallback;
      if (parameter instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) parameter;
        propertyNames.addAll(map.keySet());
        this.mapParameter = map;
        fallback = false;
      } else {
        this.mapParameter = null;
        if (parameter != null) {
          propertyNames.addAll(propertyAccessor.getPropertyNames(parameter.getClass()));
        }
        fallback = propertyNames.isEmpty();
      }
      MyBatisBindingContext bindingContext = new MyBatisBindingContext(fallback);
      this.customVariables = customVariables;
      customVariables.put(MyBatisBindingContext.CONTEXT_VARIABLE_NAME, bindingContext);
      customVariables.put(ContextKeys.PARAMETER_OBJECT, parameter);
    }

    @Override
    public Locale getLocale() {
      return Locale.getDefault();
    }

    @Override
    public boolean containsVariable(String name) {
      return customVariables.containsKey(name) || propertyNames.contains(name);
    }

    @Override
    public Set<String> getVariableNames() {
      Set<String> variableNames = new HashSet<>(customVariables.keySet());
      variableNames.addAll(propertyNames);
      return variableNames;
    }

    @Override
    public Object getVariable(String name) {
      if (customVariables.containsKey(name)) {
        return customVariables.get(name);
      }
      if (mapParameter == null) {
        return propertyAccessor.getPropertyValue(parameter, name);
      } else {
        return mapParameter.get(name);
      }
    }

  }

}
