/**
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The {@code LanguageDriver} for integrating with Thymeleaf.
 * <br>
 * If you want to customize a default {@code TemplateEngine},
 * you can configure some property using mybatis-thymeleaf.properties that encoded by UTF-8.
 * Also, you can change the properties file that will read using system property
 * (-Dmybatis-thymeleaf.config.file=... -Dmybatis-thymeleaf.config.encoding=...).
 * <br>
 * Supported properties are as follows:
 * <ul>
 *   <li>use-2way:
 *            Whether use the 2-way SQL. Default is true</li>
 *   <li>customizer:
 *            The implementation class for customizing a default {@code TemplateEngine}
 *            instanced by the MyBatis Thymeleaf.</li>
 *   <li>cache.enabled:
 *            Whether use the cache feature. Default is true</li>
 *   <li>cache.ttl:
 *            The cache TTL for resolved templates. Default is null(no TTL)</li>
 *   <li>file.character-encoding:
 *            The character encoding for reading template resources. Default is 'UTF-8'</li>
 *   <li>file.base-dir:
 *            The base directory for reading template resources. Default is ''(just under class path)</li>
 *   <li>file.patterns:
 *            The patterns for reading as template resources. Default is '*.sql'</li>
 *   <li>dialect.like.escape-char:
 *            The escape character for wildcard of LIKE. Default is {@code '\'} (backslash)</li>
 *   <li>dialect.like.escape-clause-format:
 *            The format of escape clause. Default is {@code " ESCAPE '%s' "}</li>
 *   <li>dialect.like.additional-escape-target-chars:
 *            The additional escape target characters(custom wildcard characters) for LIKE condition. Default is nothing</li>
 * </ul>
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
        System.getProperty("mybatis-thymeleaf.config.file", "mybatis-thymeleaf.properties"))) {
      if (in != null) {
        Charset encoding = Optional.ofNullable(System.getProperty("mybatis-thymeleaf.config.encoding"))
            .map(Charset::forName)
            .orElse(StandardCharsets.UTF_8);
        try (InputStreamReader inReader = new InputStreamReader(in, encoding);
             BufferedReader bufReader = new BufferedReader(inReader)) {
          properties.load(bufReader);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    TemplateMode mode = Optional.ofNullable(properties.getProperty("use-2way"))
        .map(String::trim).map(Boolean::valueOf).orElse(Boolean.TRUE) ? TemplateMode.CSS : TemplateMode.TEXT;

    boolean cacheEnabled = Optional.ofNullable(properties.getProperty("cache.enabled"))
        .map(String::trim).map(Boolean::parseBoolean).orElse(AbstractConfigurableTemplateResolver.DEFAULT_CACHEABLE);

    Long cacheTtl = Optional.ofNullable(properties.getProperty("cache.ttl"))
        .map(String::trim).map(Long::parseLong).orElse(AbstractConfigurableTemplateResolver.DEFAULT_CACHE_TTL_MS);

    String characterEncoding = Optional.ofNullable(properties.getProperty("file.character-encoding"))
        .map(String::trim).orElse(StandardCharsets.UTF_8.name());

    String fileBaseDir = Optional.ofNullable(properties.getProperty("file.base-dir"))
        .map(String::trim).orElse("");

    Character likeEscapeChar = Optional.ofNullable(properties.getProperty("dialect.like.escape-char"))
        .map(String::trim).filter(v -> v.length() == 1).map(v -> v.charAt(0)).orElse(null);

    String likeEscapeClauseFormat = Optional.ofNullable(properties.getProperty("dialect.like.escape-clause-format"))
        .map(String::trim).orElse(null);

    Set<Character> likeAdditionalEscapeTargetChars =
        Stream.of(properties.getProperty("dialect.like.additional-escape-target-chars", "")
            .split(",")).map(String::trim).filter(v -> v.length() == 1).map(v -> v.charAt(0))
            .collect(Collectors.toSet());

    Set<String> filePatterns =
        Stream.of(properties.getProperty("file.patterns","*.sql").split(","))
            .map(String::trim).collect(Collectors.toSet());

    final TemplateEngineCustomizer customizer = Optional.ofNullable(properties.getProperty("customizer"))
        .map(String::trim).map(v -> {
          try {
            return Resources.classForName(v);
          } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
          }
        }).map(v -> {
          try {
            return v.getConstructor().newInstance();
          } catch (InstantiationException | IllegalAccessException
              | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Cannot create an instance for class: " + v, e);
          }
        }).map(TemplateEngineCustomizer.class::cast).orElse(TemplateEngineCustomizer.BuiltIn.DEFAULT);

    MyBatisDialect dialect = new MyBatisDialect();
    dialect.setLikeEscapeChar(likeEscapeChar);
    dialect.setLikeEscapeClauseFormat(likeEscapeClauseFormat);
    dialect.setLikeAdditionalEscapeTargetChars(likeAdditionalEscapeTargetChars);

    ClassLoaderTemplateResolver classLoaderTemplateResolver = new ClassLoaderTemplateResolver();
    classLoaderTemplateResolver.setOrder(1);
    classLoaderTemplateResolver.setTemplateMode(mode);
    classLoaderTemplateResolver.setResolvablePatterns(filePatterns);
    classLoaderTemplateResolver.setCharacterEncoding(characterEncoding);
    classLoaderTemplateResolver.setCacheable(cacheEnabled);
    classLoaderTemplateResolver.setCacheTTLMs(cacheTtl);
    classLoaderTemplateResolver.setPrefix(fileBaseDir);

    StringTemplateResolver stringTemplateResolver = new StringTemplateResolver();
    stringTemplateResolver.setOrder(2);
    stringTemplateResolver.setTemplateMode(mode);

    TemplateEngine targetTemplateEngine = new TemplateEngine();
    targetTemplateEngine.addTemplateResolver(classLoaderTemplateResolver);
    targetTemplateEngine.addTemplateResolver(stringTemplateResolver);
    targetTemplateEngine.addDialect(dialect);
    targetTemplateEngine.setEngineContextFactory(new MyBatisDelegatingEngineContextFactory(targetTemplateEngine.getEngineContextFactory()));

    customizer.accept(targetTemplateEngine);
    return targetTemplateEngine;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ParameterHandler createParameterHandler(
      MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
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
