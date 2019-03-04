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
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.mybatis.scripting.thymeleaf.expression.MyBatisExpression;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

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
 * <li>use-2way:
 * Whether use the 2-way SQL. Default is true</li>
 * <li>customizer:
 * The implementation class for customizing a default {@code TemplateEngine}
 * instanced by the MyBatis Thymeleaf.</li>
 * <li>cache.enabled:
 * Whether use the cache feature. Default is true</li>
 * <li>cache.ttl:
 * The cache TTL for resolved templates. Default is null(no TTL)</li>
 * <li>file.character-encoding:
 * The character encoding for reading template resources. Default is 'UTF-8'</li>
 * <li>file.base-dir:
 * The base directory for reading template resources. Default is ''(just under class path)</li>
 * <li>file.patterns:
 * The patterns for reading as template resources. Default is '*.sql'</li>
 * <li>dialect.like.escape-char:
 * The escape character for wildcard of LIKE. Default is {@code '\'} (backslash)</li>
 * <li>dialect.like.escape-clause-format:
 * The format of escape clause. Default is {@code " ESCAPE '%s' "}</li>
 * <li>dialect.like.additional-escape-target-chars:
 * The additional escape target characters(custom wildcard characters) for LIKE condition. Default is nothing</li>
 * </ul>
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 * @see Builder
 */
public class ThymeleafLanguageDriver implements LanguageDriver {

  private static final String KEY_CONFIG_FILE = "mybatis-thymeleaf.config.file";
  private static final String KEY_CONFIG_ENCODING = "mybatis-thymeleaf.config.encoding";
  private static final String KEY_USE_2WAY = "use-2way";
  private static final String KEY_CACHE_ENABLED = "cache.enabled";
  private static final String KEY_CACHE_TTL = "cache.ttl";
  private static final String KEY_FILE_CHARACTER_ENCODING = "file.character-encoding";
  private static final String KEY_FILE_BASE_DIR = "file.base-dir";
  private static final String KEY_FILE_PATTERNS = "file.patterns";
  private static final String KEY_DIALECT_LIKE_ESCAPE_CHAR = "dialect.like.escape-char";
  private static final String KEY_DIALECT_LIKE_ESCAPE_CLAUSE_FORMAT = "dialect.like.escape-clause-format";
  private static final String KEY_DIALECT_LIKE_ESCAPE_TARGET_CHARS = "dialect.like.additional-escape-target-chars";
  private static final String KEY_CUSTOMIZER = "customizer";

  private final Properties configurationProperties = new Properties();
  private final ITemplateEngine templateEngine;

  /**
   * Constructor for creating instance with default {@code TemplateEngine}.
   */
  public ThymeleafLanguageDriver() {
    this.templateEngine = createDefaultTemplateEngine(new Properties());
  }

  /**
   * Constructor for creating instance with user specified {@code Properties}.
   *
   * @param customProperties A user defined {@code ITemplateEngine} instance
   */
  protected ThymeleafLanguageDriver(Properties customProperties) {
    this.templateEngine = createDefaultTemplateEngine(customProperties);
  }

  /**
   * Constructor for creating instance with user defined {@code ITemplateEngine}.
   *
   * @param templateEngine A user defined {@code ITemplateEngine} instance
   */
  protected ThymeleafLanguageDriver(ITemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  private ITemplateEngine createDefaultTemplateEngine(Properties customProperties) {
    loadConfigurationProperties(customProperties);

    // Create an MyBatisDialect instance
    MyBatisDialect dialect = new MyBatisDialect();
    Character likeEscapeChar = Optional.ofNullable(configurationProperties.getProperty(KEY_DIALECT_LIKE_ESCAPE_CHAR))
        .map(String::trim).filter(v -> v.length() == 1).map(v -> v.charAt(0)).orElse(null);
    String likeEscapeClauseFormat =
        Optional.ofNullable(configurationProperties.getProperty(KEY_DIALECT_LIKE_ESCAPE_CLAUSE_FORMAT))
            .map(String::trim).orElse(null);
    Set<Character> likeAdditionalEscapeTargetChars =
        Stream.of(configurationProperties.getProperty(KEY_DIALECT_LIKE_ESCAPE_TARGET_CHARS, "")
            .split(",")).map(String::trim).filter(v -> v.length() == 1).map(v -> v.charAt(0))
            .collect(Collectors.toSet());
    dialect.setLikeEscapeChar(likeEscapeChar);
    dialect.setLikeEscapeClauseFormat(likeEscapeClauseFormat);
    dialect.setLikeAdditionalEscapeTargetChars(likeAdditionalEscapeTargetChars);

    // Create an ClassLoaderTemplateResolver instance
    ClassLoaderTemplateResolver classLoaderTemplateResolver = new ClassLoaderTemplateResolver();
    TemplateMode mode = Optional.ofNullable(configurationProperties.getProperty(KEY_USE_2WAY))
        .map(String::trim).map(Boolean::valueOf).orElse(Boolean.TRUE) ? TemplateMode.CSS : TemplateMode.TEXT;
    boolean cacheEnabled = Optional.ofNullable(configurationProperties.getProperty(KEY_CACHE_ENABLED))
        .map(String::trim).map(Boolean::parseBoolean).orElse(AbstractConfigurableTemplateResolver.DEFAULT_CACHEABLE);
    Long cacheTtl = Optional.ofNullable(configurationProperties.getProperty(KEY_CACHE_TTL))
        .map(String::trim).map(Long::parseLong).orElse(AbstractConfigurableTemplateResolver.DEFAULT_CACHE_TTL_MS);
    String characterEncoding = Optional.ofNullable(configurationProperties.getProperty(KEY_FILE_CHARACTER_ENCODING))
        .map(String::trim).orElse(StandardCharsets.UTF_8.name());
    String fileBaseDir = Optional.ofNullable(configurationProperties.getProperty(KEY_FILE_BASE_DIR))
        .map(String::trim).orElse("");
    Set<String> filePatterns =
        Stream.of(configurationProperties.getProperty(KEY_FILE_PATTERNS, "*.sql").split(","))
            .map(String::trim).collect(Collectors.toSet());
    classLoaderTemplateResolver.setOrder(1);
    classLoaderTemplateResolver.setTemplateMode(mode);
    classLoaderTemplateResolver.setResolvablePatterns(filePatterns);
    classLoaderTemplateResolver.setCharacterEncoding(characterEncoding);
    classLoaderTemplateResolver.setCacheable(cacheEnabled);
    classLoaderTemplateResolver.setCacheTTLMs(cacheTtl);
    classLoaderTemplateResolver.setPrefix(fileBaseDir);

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
    final TemplateEngineCustomizer customizer = Optional.ofNullable(configurationProperties.getProperty(KEY_CUSTOMIZER))
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
    customizer.accept(targetTemplateEngine);

    return targetTemplateEngine;
  }

  private void loadConfigurationProperties(Properties customProperties) {
    configurationProperties.putAll(customProperties);
    InputStream in;
    try {
      in = Resources.getResourceAsStream(System.getProperty(KEY_CONFIG_FILE, "mybatis-thymeleaf.properties"));
    } catch (IOException e) {
      in = null;
    }
    if (in != null) {
      Charset encoding = Optional.ofNullable(System.getProperty(KEY_CONFIG_ENCODING))
          .map(Charset::forName)
          .orElse(StandardCharsets.UTF_8);
      try (InputStreamReader inReader = new InputStreamReader(in, encoding);
           BufferedReader bufReader = new BufferedReader(inReader)) {
        configurationProperties.load(bufReader);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
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

  /**
   * Creates a new builder instance for {@link MyBatisExpression}.
   * @return a new builder instance
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * Builder class for {@link ThymeleafLanguageDriver}.
   */
  public static class Builder {

    private final Properties customProperties = new Properties();
    private ITemplateEngine templateEngine;

    private Builder() {
      // NOP
    }

    /**
     * Whether use the 2-way SQL.
     *
     * @param user2way If use the 2-way SQL, set {@code true}
     * @return a self instance
     */
    public Builder use2way(boolean user2way) {
      customProperties.setProperty(KEY_USE_2WAY, String.valueOf(user2way));
      return this;
    }

    /**
     * Whether use the cache feature.
     *
     * @param cacheEnabled If use cache feature, set {@code true}
     * @return a self instance
     */
    public Builder cacheEnabled(boolean cacheEnabled) {
      customProperties.setProperty(KEY_CACHE_ENABLED, String.valueOf(cacheEnabled));
      return this;
    }

    /**
     * The cache TTL for resolved templates.
     *
     * @param ttl TTL (millisecond)
     * @return a self instance
     */
    public Builder cacheTtl(long ttl) {
      customProperties.setProperty(KEY_CACHE_TTL, String.valueOf(ttl));
      return this;
    }

    /**
     * The character encoding for reading template resources.
     *
     * @param charset encoding charset
     * @return a self instance
     */
    public Builder fileCharacterEncoding(Charset charset) {
      Optional.ofNullable(charset)
          .ifPresent(v -> customProperties.setProperty(KEY_FILE_CHARACTER_ENCODING, v.toString()));
      return this;
    }

    /**
     * The base directory for reading template resources.
     *
     * @param dir base directory path
     * @return a self instance
     */
    public Builder fileBaseDir(String dir) {
      Optional.ofNullable(dir)
          .ifPresent(v -> customProperties.setProperty(KEY_FILE_BASE_DIR, v));
      return this;
    }

    /**
     * The patterns for reading as template resources.
     *
     * @param patterns patterns for reading as template resources
     * @return a self instance
     */
    public Builder filePatterns(String... patterns) {
      customProperties.setProperty(KEY_FILE_PATTERNS, String.join(",", patterns));
      return this;
    }

    /**
     * The escape character for wildcard of LIKE.
     *
     * @param escapeChar escape character
     * @return a self instance
     */
    public Builder dialectLikeEscapeChar(char escapeChar) {
      customProperties.setProperty(KEY_DIALECT_LIKE_ESCAPE_CHAR, String.valueOf(escapeChar));
      return this;
    }

    /**
     * The format of escape clause.
     *
     * @param escapeClauseFormat format of escape clause
     * @return a self instance
     */
    public Builder dialectLikeEscapeClauseFormat(String escapeClauseFormat) {
      Optional.ofNullable(escapeClauseFormat)
          .ifPresent(v -> customProperties.setProperty(KEY_DIALECT_LIKE_ESCAPE_CLAUSE_FORMAT, v));
      return this;
    }

    /**
     * The additional escape target characters(custom wildcard characters) for LIKE condition.
     *
     * @param targetChars format of escape clause
     * @return a self instance
     */
    public Builder dialectLikeAdditionalEscapeTargetChars(char... targetChars) {
      StringJoiner joiner = new StringJoiner(",");
      joiner.setEmptyValue("");
      for (char targetChar : targetChars) {
        joiner.add(String.valueOf(targetChar));
      }
      customProperties.setProperty(KEY_DIALECT_LIKE_ESCAPE_TARGET_CHARS, joiner.toString());
      return this;
    }

    /**
     * The implementation class for customizing a default {@code TemplateEngine}.
     *
     * @param customizer customizer class
     * @return a self instance
     */
    public Builder customizer(Class<? extends TemplateEngineCustomizer> customizer) {
      Optional.ofNullable(customizer)
          .ifPresent(v -> customProperties.setProperty(KEY_CUSTOMIZER, customizer.getName()));
      return this;
    }

    /**
     * A user defined {@code ITemplateEngine} instance.
     *
     * @param templateEngine user defined {@code ITemplateEngine} instance
     * @return a self instance
     */
    public Builder templateEngine(ITemplateEngine templateEngine) {
      this.templateEngine = templateEngine;
      return this;
    }

    /**
     * Build a {@link ThymeleafLanguageDriver} instance.
     *
     * @return a {@link ThymeleafLanguageDriver} instance
     */
    public ThymeleafLanguageDriver build() {
      if (templateEngine == null) {
        return new ThymeleafLanguageDriver(customProperties);
      } else {
        return new ThymeleafLanguageDriver(templateEngine);
      }
    }

  }

}
