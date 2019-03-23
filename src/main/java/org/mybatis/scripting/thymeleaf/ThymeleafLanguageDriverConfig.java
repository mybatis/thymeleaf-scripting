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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.thymeleaf.util.StringUtils;

/**
 * Configuration class for {@link ThymeleafLanguageDriver}.
 *
 * @author Kazuki Shimizu
 * @since 1.0.0
 */
public class ThymeleafLanguageDriverConfig {

  private static final String PROPERTY_KEY_CONFIG_FILE = "mybatis-thymeleaf.config.file";
  private static final String PROPERTY_KEY_CONFIG_ENCODING = "mybatis-thymeleaf.config.encoding";
  private static final String DEFAULT_PROPERTIES_FILE = "mybatis-thymeleaf.properties";
  private static Map<Class<?>, Function<String, Object>> TYPE_CONVERTERS;

  static {
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();
    converters.put(boolean.class, v -> Boolean.valueOf(v.trim()));
    converters.put(String.class, String::trim);
    converters.put(Character[].class, v -> Stream.of(v.split(",")).map(String::trim).filter(e -> e.length() == 1)
        .map(e -> e.charAt(0)).toArray(Character[]::new));
    converters.put(Character.class, v -> v.trim().charAt(0));
    converters.put(Charset.class, v -> Charset.forName(v.trim()));
    converters.put(Long.class, v -> Long.valueOf(v.trim()));
    converters.put(String[].class, v -> Stream.of(v.split(",")).map(String::trim).toArray(String[]::new));
    converters.put(Class.class, ThymeleafLanguageDriverConfig::classForName);
    TYPE_CONVERTERS = Collections.unmodifiableMap(converters);
  }

  /**
   * Whether use the 2-way SQL feature.
   */
  private boolean use2way = true;

  /**
   * The interface for customizing a default TemplateEngine instanced by the mybatis-thymeleaf.
   */
  private Class<? extends TemplateEngineCustomizer> customizer;

  /**
   * Template file configuration.
   */
  private final TemplateFileConfig templateFile = new TemplateFileConfig();

  /**
   * Dialect configuration.
   */
  private final DialectConfig dialect = new DialectConfig();

  /**
   * Get whether use the 2-way SQL feature.
   * <p>
   * Default is {@code true}.
   * </p>
   * 
   * @return If use the 2-way SQL feature, return {@code true}
   */
  public boolean isUse2way() {
    return use2way;
  }

  /**
   * Set whether use the 2-way SQL feature.
   *
   * @param use2way
   *          If use the 2-way SQL feature, set {@code true}
   */
  public void setUse2way(boolean use2way) {
    this.use2way = use2way;
  }

  /**
   * Get the interface for customizing a default TemplateEngine instanced by the mybatis-thymeleaf.
   * <p>
   * Default is {@code null}.
   * </p>
   * 
   * @return the interface for customizing a default TemplateEngine
   */
  public Class<? extends TemplateEngineCustomizer> getCustomizer() {
    return customizer;
  }

  /**
   * Set the interface for customizing a default TemplateEngine instanced by the mybatis-thymeleaf.
   *
   * @param customizer
   *          the interface for customizing a default TemplateEngine
   */
  public void setCustomizer(Class<? extends TemplateEngineCustomizer> customizer) {
    this.customizer = customizer;
  }

  /**
   * Get a template file configuration.
   *
   * @return a template file configuration
   */
  public TemplateFileConfig getTemplateFile() {
    return templateFile;
  }

  /**
   * Get a dialect configuration.
   *
   * @return a dialect configuration
   */
  public DialectConfig getDialect() {
    return dialect;
  }

  /**
   * Template file configuration.
   *
   * @since 1.0.0
   */
  public static class TemplateFileConfig {

    /**
     * The character encoding for reading template resource file.
     */
    private Charset encoding = StandardCharsets.UTF_8;

    /**
     * The base directory for reading template resource file.
     */
    private String baseDir = "";

    /**
     * The patterns for reading as template resource file. (Can specify multiple patterns using comma(",") as separator
     * character)
     */
    private String[] patterns = { "*.sql" };

    /**
     * Whether use the cache feature when load template resource file.
     */
    private boolean cacheEnabled = true;

    /**
     * The cache TTL(millisecond) for resolved templates.
     */
    private Long cacheTtl;

    /**
     * Get the character encoding for reading template resource file.
     * <p>
     * Default is {@code UTF-8}.
     * </p>
     * 
     * @return the character encoding for reading template resource file
     */
    public Charset getEncoding() {
      return encoding;
    }

    /**
     * Set the character encoding for reading template resource file.
     *
     * @param encoding
     *          the character encoding for reading template resource file
     */
    public void setEncoding(Charset encoding) {
      this.encoding = encoding;
    }

    /**
     * Get the base directory for reading template resource file.
     * <p>
     * Default is {@code ""}(none).
     * </p>
     *
     * @return the base directory for reading template resource file
     */
    public String getBaseDir() {
      return baseDir;
    }

    /**
     * Set the base directory for reading template resource file.
     *
     * @param baseDir
     *          the base directory for reading template resource file
     */
    public void setBaseDir(String baseDir) {
      this.baseDir = baseDir;
    }

    /**
     * Get patterns for reading as template resource file.
     * <p>
     * Default is {@code "*.sql"}.
     * </p>
     * 
     * @return patterns for reading as template resource file
     */
    public String[] getPatterns() {
      return patterns;
    }

    /**
     * Set patterns for reading as template resource file.
     *
     * @param patterns
     *          patterns for reading as template resource file
     */
    public void setPatterns(String... patterns) {
      this.patterns = patterns;
    }

    /**
     * Get whether use the cache feature when load template resource file.
     * <p>
     * Default is {@code true}.
     * </p>
     * 
     * @return If use th cache feature, return {@code true}
     */
    public boolean isCacheEnabled() {
      return cacheEnabled;
    }

    /**
     * Set whether use the cache feature when load template resource file.
     *
     * @param cacheEnabled
     *          If use th cache feature, set {@code true}
     */
    public void setCacheEnabled(boolean cacheEnabled) {
      this.cacheEnabled = cacheEnabled;
    }

    /**
     * Get the cache TTL(millisecond) for resolved templates.
     * <p>
     * Default is {@code null}(indicate to use default value of Thymeleaf).
     * </p>
     * 
     * @return the cache TTL(millisecond) for resolved templates
     */
    public Long getCacheTtl() {
      return cacheTtl;
    }

    /**
     * Set the cache TTL(millisecond) for resolved templates.
     *
     * @param cacheTtl
     *          the cache TTL(millisecond) for resolved templates
     */
    public void setCacheTtl(Long cacheTtl) {
      this.cacheTtl = cacheTtl;
    }

  }

  /**
   * Dialect configuration.
   *
   * @since 1.0.0
   */
  public static class DialectConfig {

    /**
     * The prefix name of dialect provided by this project.
     */
    private String prefix = "mb";

    /**
     * The escape character for wildcard of LIKE condition.
     */
    private Character likeEscapeChar = '\\';

    /**
     * The format of escape clause for LIKE condition (Can specify format that can be allowed by String#format method).
     */
    private String likeEscapeClauseFormat = "ESCAPE '%s'";

    /**
     * Additional escape target characters(custom wildcard characters) for LIKE condition. (Can specify multiple
     * characters using comma(",") as separator character)
     */
    private Character[] likeAdditionalEscapeTargetChars;

    /**
     * Get the prefix name of dialect provided by this project.
     * <p>
     * Default is {@code "mb"}.
     * </p>
     * 
     * @return the prefix name of dialect
     */
    public String getPrefix() {
      return prefix;
    }

    /**
     * Set the prefix name of dialect provided by this project.
     *
     * @param prefix
     *          the prefix name of dialect
     */
    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }

    /**
     * Get the escape character for wildcard of LIKE condition.
     * <p>
     * Default is {@code '\'}.
     * </p>
     * 
     * @return the escape character for wildcard
     */
    public Character getLikeEscapeChar() {
      return likeEscapeChar;
    }

    /**
     * Set the escape character for wildcard of LIKE condition.
     *
     * @param likeEscapeChar
     *          the escape character for wildcard
     */
    public void setLikeEscapeChar(Character likeEscapeChar) {
      this.likeEscapeChar = likeEscapeChar;
    }

    /**
     * Get the format of escape clause for LIKE condition.
     * <p>
     * Can specify format that can be allowed by String#format method. Default is {@code "ESCAPE '%s'"}.
     * </p>
     * 
     * @return the format of escape clause for LIKE condition
     */
    public String getLikeEscapeClauseFormat() {
      return likeEscapeClauseFormat;
    }

    /**
     * Set the format of escape clause for LIKE condition.
     *
     * @param likeEscapeClauseFormat
     *          the format of escape clause for LIKE condition
     */
    public void setLikeEscapeClauseFormat(String likeEscapeClauseFormat) {
      this.likeEscapeClauseFormat = likeEscapeClauseFormat;
    }

    /**
     * Get additional escape target characters(custom wildcard characters) for LIKE condition.
     * <p>
     * Can specify multiple characters using comma(",") as separator character. Default is empty(none).
     * </p>
     * 
     * @return additional escape target characters(custom wildcard characters)
     */
    public Character[] getLikeAdditionalEscapeTargetChars() {
      return likeAdditionalEscapeTargetChars;
    }

    /**
     * Set additional escape target characters(custom wildcard characters) for LIKE condition.
     *
     * @param likeAdditionalEscapeTargetChars
     *          additional escape target characters(custom wildcard characters)
     */
    public void setLikeAdditionalEscapeTargetChars(Character... likeAdditionalEscapeTargetChars) {
      this.likeAdditionalEscapeTargetChars = likeAdditionalEscapeTargetChars;
    }

  }

  /**
   * Create an instance from default properties file. <br>
   * If you want to customize a default {@code TemplateEngine}, you can configure some property using
   * mybatis-thymeleaf.properties that encoded by UTF-8. Also, you can change the properties file that will read using
   * system property (-Dmybatis-thymeleaf.config.file=... -Dmybatis-thymeleaf.config.encoding=...). <br>
   * Supported properties are as follows:
   * <table border="1">
   * <caption>Supported properties</caption>
   * <tr>
   * <th>Property Key</th>
   * <th>Description</th>
   * <th>Default</th>
   * </tr>
   * <tr>
   * <th colspan="3">General configuration</th>
   * </tr>
   * <tr>
   * <td>use-2way</td>
   * <td>Whether use the 2-way SQL</td>
   * <td>{@code true}</td>
   * </tr>
   * <tr>
   * <td>customizer</td>
   * <td>The implementation class for customizing a default {@code TemplateEngine} instanced by the MyBatis Thymeleaf
   * </td>
   * <td>None</td>
   * </tr>
   * <tr>
   * <th colspan="3">Template file configuration</th>
   * </tr>
   * <tr>
   * <td>template-file.cache-enabled</td>
   * <td>Whether use the cache feature</td>
   * <td>{@code true}</td>
   * </tr>
   * <tr>
   * <td>template-file.cache-ttl</td>
   * <td>The cache TTL for resolved templates</td>
   * <td>None(use default value of Thymeleaf)</td>
   * </tr>
   * <tr>
   * <td>template-file.encoding</td>
   * <td>The character encoding for reading template resources</td>
   * <td>{@code "UTF-8"}</td>
   * </tr>
   * <tr>
   * <td>template-file.base-dir</td>
   * <td>The base directory for reading template resources</td>
   * <td>None(just under class path)</td>
   * </tr>
   * <tr>
   * <td>template-file.patterns</td>
   * <td>The patterns for reading as template resources</td>
   * <td>{@code "*.sql"}</td>
   * </tr>
   * <tr>
   * <th colspan="3">Dialect configuration</th>
   * </tr>
   * <tr>
   * <td>dialect.prefix</td>
   * <td>The prefix name of dialect provided by this project</td>
   * <td>{@code "mb"}</td>
   * </tr>
   * <tr>
   * <td>dialect.like-escape-char</td>
   * <td>The escape character for wildcard of LIKE</td>
   * <td>{@code '\'} (backslash)</td>
   * </tr>
   * <tr>
   * <td>dialect.like-escape-clause-format</td>
   * <td>The format of escape clause</td>
   * <td>{@code "ESCAPE '%s'"}</td>
   * </tr>
   * <tr>
   * <td>dialect.like-additional-escape-target-chars</td>
   * <td>The additional escape target characters(custom wildcard characters) for LIKE condition</td>
   * <td>None</td>
   * </tr>
   * </table>
   *
   * @return a configuration instance
   */
  public static ThymeleafLanguageDriverConfig newInstance() {
    return newInstance(loadDefaultProperties());
  }

  /**
   * Create an instance from specified properties file. <br>
   * you can configure some property using specified properties file that encoded by UTF-8. Also, you can change file
   * encoding that will read using system property (-Dmybatis-thymeleaf.config.encoding=...).
   *
   * @param resourcePath
   *          A property file resource path
   * @return a configuration instance
   * @see #newInstance()
   */
  public static ThymeleafLanguageDriverConfig newInstance(String resourcePath) {
    Properties properties = loadDefaultProperties();
    properties.putAll(loadProperties(resourcePath));
    return newInstance(properties);
  }

  /**
   * Create an instance from specified properties.
   *
   * @param customProperties
   *          custom configuration properties
   * @return a configuration instance
   * @see #newInstance()
   */
  public static ThymeleafLanguageDriverConfig newInstance(Properties customProperties) {
    ThymeleafLanguageDriverConfig config = new ThymeleafLanguageDriverConfig();
    Properties properties = loadDefaultProperties();
    Optional.ofNullable(customProperties).ifPresent(properties::putAll);
    override(config, properties);
    return config;
  }

  /**
   * Create an instance using specified customizer and override using a default properties file.
   *
   * @param customizer
   *          baseline customizer
   * @return a configuration instance
   * @see #newInstance()
   */
  public static ThymeleafLanguageDriverConfig newInstance(Consumer<ThymeleafLanguageDriverConfig> customizer) {
    ThymeleafLanguageDriverConfig config = new ThymeleafLanguageDriverConfig();
    customizer.accept(config);
    override(config, loadDefaultProperties());
    return config;
  }

  private static void override(ThymeleafLanguageDriverConfig config, Properties properties) {
    MetaObject metaObject = MetaObject.forObject(config, new DefaultObjectFactory(), new DefaultObjectWrapperFactory(),
        new DefaultReflectorFactory());
    properties.forEach((key, value) -> {
      String propertyPath = StringUtils.unCapitalize(StringUtils.capitalizeWords(key, "-").replaceAll("-", ""));
      Optional.ofNullable(value).ifPresent(v -> {
        Object convertedValue = TYPE_CONVERTERS.get(metaObject.getSetterType(propertyPath)).apply(value.toString());
        metaObject.setValue(propertyPath, convertedValue);
      });
    });
  }

  private static Properties loadDefaultProperties() {
    return loadProperties(System.getProperty(PROPERTY_KEY_CONFIG_FILE, DEFAULT_PROPERTIES_FILE));
  }

  private static Properties loadProperties(String resourcePath) {
    Properties properties = new Properties();
    InputStream in;
    try {
      in = Resources.getResourceAsStream(resourcePath);
    } catch (IOException e) {
      in = null;
    }
    if (in != null) {
      Charset encoding = Optional.ofNullable(System.getProperty(PROPERTY_KEY_CONFIG_ENCODING)).map(Charset::forName)
          .orElse(StandardCharsets.UTF_8);
      try (InputStreamReader inReader = new InputStreamReader(in, encoding);
          BufferedReader bufReader = new BufferedReader(inReader)) {
        properties.load(bufReader);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    return properties;
  }

  private static Class<?> classForName(String value) {
    try {
      return Resources.classForName(value.trim());
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

}
