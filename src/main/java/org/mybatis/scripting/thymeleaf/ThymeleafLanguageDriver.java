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

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * The {@code LanguageDriver} for integrating with Thymeleaf.
 * <p>
 * If you want to customize a default {@code TemplateEngine},
 * you can configure some property using mybatis-thymeleaf.properties.
 * Also, you can change the properties file that will read using system property(-Dmybatis-thymeleaf.config=...).
 * </p>
 * <br/>
 * Supported properties are as follows:
 * <ul>
 *   <li>template.use-2way: Whether use the 2-way SQL. Default is true</li>
 *   <li>template.cache.enabled: Whether use the cache feature. Default is true</li>
 *   <li>template.cache.ttl: The cache TTL for resolved templates. Default is null(no TTL)</li>
 *   <li>template.file.character-encoding: The character encoding for reading template resources. Default is 'UTF-8'</li>
 *   <li>template.file.base-dir: The base directory for reading template resources. Default is '/'(just under class path)</li>
 *   <li>template.file.patterns: The patterns for reading as template resources. Default is '*.sql'</li>
 *   <li>template.customizer: The implementation class for customizing a default {@code TemplateEngine} instanced by the MyBatis Thymeleaf.</li>
 * </ul>
 * </p>
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
public class ThymeleafLanguageDriver implements LanguageDriver {

  private final Properties properties = new Properties();

  private final ITemplateEngine templateEngine;

  /**
   * Constructor for creating instance with default {@code TemplateEngine}.
   */
  public ThymeleafLanguageDriver() {
    this.templateEngine = createDefaultTemplateEngine();
  }

  /**
   * Constructor for creating instance with user defined {@code ITemplateEngine}.
   *
   * @param templateEngine A user defined {@code ITemplateEngine} instance
   */
  public ThymeleafLanguageDriver(ITemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  private ITemplateEngine createDefaultTemplateEngine() {
    try (InputStream in = Resources.getResourceAsStream(
        System.getProperty("mybatis-thymeleaf.config", "mybatis-thymeleaf.properties"))) {
      if (in != null) {
        properties.load(in);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    TemplateMode mode = Optional.ofNullable(properties.getProperty("template.use-2way"))
        .map(String::trim).map(Boolean::valueOf).orElse(Boolean.TRUE) ? TemplateMode.CSS : TemplateMode.TEXT;

    boolean cacheEnabled = Optional.ofNullable(properties.getProperty("template.cache.enabled"))
        .map(String::trim).map(Boolean::parseBoolean).orElse(AbstractConfigurableTemplateResolver.DEFAULT_CACHEABLE);

    Long cacheTtl = Optional.ofNullable(properties.getProperty("template.cache.ttl"))
        .map(String::trim).map(Long::parseLong).orElse(AbstractConfigurableTemplateResolver.DEFAULT_CACHE_TTL_MS);

    String characterEncoding = Optional.ofNullable(properties.getProperty("template.file.character-encoding"))
        .map(String::trim).orElse(StandardCharsets.UTF_8.name());

    String baseDir = Optional.ofNullable(properties.getProperty("template.file.base-dir"))
        .map(String::trim).orElse("/");

    Set<String> patterns = new LinkedHashSet<>(Arrays.asList(Optional.ofNullable(
        properties.getProperty("template.file.patterns")).orElse("*.sql").trim().split(",")));

    TemplateEngineCustomizer customizer = Optional.ofNullable(properties.getProperty("template.customizer"))
        .map(String::trim).map(v -> {
          try {
            return Resources.classForName(v);
          } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
          }
        }).map(v -> {
          try {
            return v.getConstructor().newInstance();
          } catch (InstantiationException | IllegalAccessException |
              InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Cannot create an instance for class: " + v, e);
          }
        }).map(TemplateEngineCustomizer.class::cast).orElse(TemplateEngineCustomizer.DEFAULT);

    ClassLoaderTemplateResolver classLoaderTemplateResolver = new ClassLoaderTemplateResolver();
    classLoaderTemplateResolver.setOrder(1);
    classLoaderTemplateResolver.setTemplateMode(mode);
    classLoaderTemplateResolver.setResolvablePatterns(patterns);
    classLoaderTemplateResolver.setCharacterEncoding(characterEncoding);
    classLoaderTemplateResolver.setCacheable(cacheEnabled);
    classLoaderTemplateResolver.setCacheTTLMs(cacheTtl);
    classLoaderTemplateResolver.setPrefix(baseDir);

    StringTemplateResolver stringTemplateResolver = new StringTemplateResolver();
    stringTemplateResolver.setOrder(2);
    stringTemplateResolver.setTemplateMode(mode);

    TemplateEngine templateEngine = new TemplateEngine();
    templateEngine.addTemplateResolver(classLoaderTemplateResolver);
    templateEngine.addTemplateResolver(stringTemplateResolver);

    customizer.accept(templateEngine);
    return templateEngine;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    return new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
    return createSqlSource(configuration, script.getNode().getTextContent(), parameterType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
    return new ThymeleafSqlSource(configuration, templateEngine, script.trim());
  }

}
