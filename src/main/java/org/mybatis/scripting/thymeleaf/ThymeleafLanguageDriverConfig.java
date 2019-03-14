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
import java.io.UncheckedIOException;
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

  {
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();
    converters.put(boolean.class, v -> Boolean.valueOf(v.trim()));
    converters.put(String.class, String::trim);
    converters.put(Character[].class, v -> Stream.of(v.split(","))
        .map(String::trim).filter(e -> e.length() == 1).map(e -> e.charAt(0)).toArray(Character[]::new));
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

  public boolean isUse2way() {
    return use2way;
  }

  public void setUse2way(boolean use2way) {
    this.use2way = use2way;
  }

  public Class<? extends TemplateEngineCustomizer> getCustomizer() {
    return customizer;
  }

  public void setCustomizer(Class<? extends TemplateEngineCustomizer> customizer) {
    this.customizer = customizer;
  }

  public TemplateFileConfig getTemplateFile() {
    return templateFile;
  }

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
     * The patterns for reading as template resource file.
     * (Can specify multiple patterns using comma(",") as separator character)
     */
    private String[] patterns = {"*.sql"};

    /**
     * Whether use the cache feature when load template resource file.
     */
    private boolean cacheEnabled = true;

    /**
     * The cache TTL(millisecond) for resolved templates.
     */
    private Long cacheTtl;

    public Charset getEncoding() {
      return encoding;
    }

    public void setEncoding(Charset encoding) {
      this.encoding = encoding;
    }

    public String getBaseDir() {
      return baseDir;
    }

    public void setBaseDir(String baseDir) {
      this.baseDir = baseDir;
    }

    public String[] getPatterns() {
      return patterns;
    }

    public void setPatterns(String... patterns) {
      this.patterns = patterns;
    }

    public boolean isCacheEnabled() {
      return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
      this.cacheEnabled = cacheEnabled;
    }

    public Long getCacheTtl() {
      return cacheTtl;
    }

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
     * Additional escape target characters(custom wildcard characters) for LIKE condition.
     * (Can specify multiple characters using comma(",") as separator character)
     */
    private Character[] likeAdditionalEscapeTargetChars;

    public String getPrefix() {
      return prefix;
    }

    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }

    public Character getLikeEscapeChar() {
      return likeEscapeChar;
    }

    public void setLikeEscapeChar(Character likeEscapeChar) {
      this.likeEscapeChar = likeEscapeChar;
    }

    public String getLikeEscapeClauseFormat() {
      return likeEscapeClauseFormat;
    }

    public void setLikeEscapeClauseFormat(String likeEscapeClauseFormat) {
      this.likeEscapeClauseFormat = likeEscapeClauseFormat;
    }

    public Character[] getLikeAdditionalEscapeTargetChars() {
      return likeAdditionalEscapeTargetChars;
    }

    public void setLikeAdditionalEscapeTargetChars(Character... likeAdditionalEscapeTargetChars) {
      this.likeAdditionalEscapeTargetChars = likeAdditionalEscapeTargetChars;
    }

  }

  /**
   * Create an instance from default properties file.
   * <br>
   * If you want to customize a default {@code TemplateEngine},
   * you can configure some property using mybatis-thymeleaf.properties that encoded by UTF-8.
   * Also, you can change the properties file that will read using system property
   * (-Dmybatis-thymeleaf.config.file=... -Dmybatis-thymeleaf.config.encoding=...).
   * <br>
   * Supported properties are as follows:
   * <ul>
   * <li>use-2way:
   * Whether use the 2-way SQL. Default is {@code true}</li>
   * <li>customizer:
   * The implementation class for customizing a default {@code TemplateEngine}
   * instanced by the MyBatis Thymeleaf.</li>
   * <li>template-file.cache-enabled:
   * Whether use the cache feature. Default is {@code true}</li>
   * <li>template-file.cache-ttl:
   * The cache TTL for resolved templates. Default is {@code null}(no TTL)</li>
   * <li>template-file.encoding:
   * The character encoding for reading template resources. Default is {@code "UTF-8"}</li>
   * <li>template-file.base-dir:
   * The base directory for reading template resources. Default is {@code ""}(just under class path)</li>
   * <li>template-file.patterns:
   * The patterns for reading as template resources. Default is {@code "*.sql"}</li>
   * <li>dialect.prefix:
   * The prefix name of dialect provided by this project. Default is {@code "mb"}</li>
   * <li>dialect.like-escape-char:
   * The escape character for wildcard of LIKE. Default is {@code '\'} (backslash)</li>
   * <li>dialect.like-escape-clause-format:
   * The format of escape clause. Default is {@code "ESCAPE '%s'"}</li>
   * <li>dialect.like-additional-escape-target-chars:
   * The additional escape target characters(custom wildcard characters) for LIKE condition. Default is nothing</li>
   * </ul>
   *
   * @return a configuration instance
   */
  public static ThymeleafLanguageDriverConfig newInstance() {
    return newInstance(loadDefaultProperties());
  }

  /**
   * Create an instance from specified properties file.
   * <br>
   * you can configure some property using specified properties file that encoded by UTF-8.
   * Also, you can change file encoding that will read using system property
   * (-Dmybatis-thymeleaf.config.encoding=...).
   *
   * @param resourcePath A property file resource path
   * @return a configuration instance
   * @see #newInstance
   */
  public static ThymeleafLanguageDriverConfig newInstance(String resourcePath) {
    Properties properties = loadDefaultProperties();
    properties.putAll(loadProperties(resourcePath));
    return newInstance(properties);
  }

  /**
   * Create an instance from specified properties.
   *
   * @param customProperties custom configuration properties
   * @return a configuration instance
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
   * @param customizer baseline customizer
   * @return a configuration instance
   */
  public static ThymeleafLanguageDriverConfig newInstance(Consumer<ThymeleafLanguageDriverConfig> customizer) {
    ThymeleafLanguageDriverConfig config = new ThymeleafLanguageDriverConfig();
    customizer.accept(config);
    override(config, loadDefaultProperties());
    return config;
  }

  private static void override(ThymeleafLanguageDriverConfig config, Properties properties) {
    MetaObject metaObject = MetaObject.forObject(config, new DefaultObjectFactory(),
        new DefaultObjectWrapperFactory(), new DefaultReflectorFactory());
    properties.forEach((key, value) -> {
      String propertyPath = StringUtils.unCapitalize(
          StringUtils.capitalizeWords(key, "-").replaceAll("-", ""));
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
      Charset encoding = Optional.ofNullable(System.getProperty(PROPERTY_KEY_CONFIG_ENCODING))
          .map(Charset::forName)
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
