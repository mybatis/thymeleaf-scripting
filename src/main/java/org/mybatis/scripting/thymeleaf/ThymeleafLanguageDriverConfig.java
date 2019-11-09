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

import java.util.Properties;
import java.util.function.Consumer;

/**
 * Configuration class for {@link ThymeleafLanguageDriver}.
 *
 * @author Kazuki Shimizu
 * @since 1.0.0
 */
public class ThymeleafLanguageDriverConfig extends SqlGeneratorConfig {

  /**
   * Template file configuration.
   */
  private final TemplateFileConfig templateFile = new TemplateFileConfig();

  @Override
  public TemplateFileConfig getTemplateFile() {
    return templateFile;
  }

  /**
   * Template file configuration for language driver of the MyBatis.
   *
   * @since 1.0.2
   */
  public static class TemplateFileConfig extends SqlGeneratorConfig.TemplateFileConfig {

    /**
     * The template file path provider configuration.
     */
    private final PathProviderConfig pathProvider = new PathProviderConfig();

    /**
     * Get the template file path provider configuration.
     *
     * @return the template file path provider configuration
     * @since 1.0.1
     */
    public PathProviderConfig getPathProvider() {
      return pathProvider;
    }

    /**
     * The template file path provider configuration.
     *
     * @since 1.0.1
     */
    public static class PathProviderConfig {

      /**
       * The prefix for adding to template file path.
       */
      private String prefix = "";

      /**
       * Whether includes package path part.
       */
      private boolean includesPackagePath = true;

      /**
       * Whether separate directory per mapper.
       */
      private boolean separateDirectoryPerMapper = true;

      /**
       * Whether includes mapper name into file name when separate directory per mapper.
       */
      private boolean includesMapperNameWhenSeparateDirectory = true;

      /**
       * Whether cache a resolved template file path.
       */
      private boolean cacheEnabled = true;

      /**
       * Get a prefix for adding to template file path.
       * <p>
       * Default is {@code ""}.
       * </p>
       *
       * @return a prefix for adding to template file path
       */
      public String getPrefix() {
        return prefix;
      }

      /**
       * Set the prefix for adding to template file path.
       *
       * @param prefix
       *          The prefix for adding to template file path
       */
      public void setPrefix(String prefix) {
        this.prefix = prefix;
      }

      /**
       * Get whether includes package path part.
       * <p>
       * Default is {@code true}.
       * </p>
       *
       * @return If includes package path, return {@code true}
       */
      public boolean isIncludesPackagePath() {
        return includesPackagePath;
      }

      /**
       * Set whether includes package path part.
       *
       * @param includesPackagePath
       *          If want to includes, set {@code true}
       */
      public void setIncludesPackagePath(boolean includesPackagePath) {
        this.includesPackagePath = includesPackagePath;
      }

      /**
       * Get whether separate directory per mapper.
       *
       * @return If separate directory per mapper, return {@code true}
       */
      public boolean isSeparateDirectoryPerMapper() {
        return separateDirectoryPerMapper;
      }

      /**
       * Set whether separate directory per mapper.
       * <p>
       * Default is {@code true}.
       * </p>
       *
       * @param separateDirectoryPerMapper
       *          If want to separate directory, set {@code true}
       */
      public void setSeparateDirectoryPerMapper(boolean separateDirectoryPerMapper) {
        this.separateDirectoryPerMapper = separateDirectoryPerMapper;
      }

      /**
       * Get whether includes mapper name into file name when separate directory per mapper.
       * <p>
       * Default is {@code true}.
       * </p>
       *
       * @return If includes mapper name, return {@code true}
       */
      public boolean isIncludesMapperNameWhenSeparateDirectory() {
        return includesMapperNameWhenSeparateDirectory;
      }

      /**
       * Set whether includes mapper name into file name when separate directory per mapper.
       * <p>
       * Default is {@code true}.
       * </p>
       *
       * @param includesMapperNameWhenSeparateDirectory
       *          If want to includes, set {@code true}
       */
      public void setIncludesMapperNameWhenSeparateDirectory(boolean includesMapperNameWhenSeparateDirectory) {
        this.includesMapperNameWhenSeparateDirectory = includesMapperNameWhenSeparateDirectory;
      }

      /**
       * Get whether cache a resolved template file path.
       * <p>
       * Default is {@code true}.
       * </p>
       *
       * @return If cache a resolved template file path, return {@code true}
       */
      public boolean isCacheEnabled() {
        return cacheEnabled;
      }

      /**
       * Set whether cache a resolved template file path.
       *
       * @param cacheEnabled
       *          If want to cache, set {@code true}
       */
      public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
      }

    }

  }

  /**
   * Create an instance from default properties file. <br>
   * If you want to customize a default {@code TemplateEngine}, you can configure some property using
   * mybatis-thymeleaf.properties that encoded by UTF-8. Also, you can change the properties file that will read using
   * system property (-Dmybatis-thymeleaf.config.file=... -Dmybatis-thymeleaf.config.encoding=...). <br>
   * About supported common properties see {@link SqlGeneratorConfig#newInstance()}. Supported specific properties are
   * as follows:
   * <table border="1">
   * <caption>Supported properties</caption>
   * <tr>
   * <th>Property Key</th>
   * <th>Description</th>
   * <th>Default</th>
   * </tr>
   * <tr>
   * <th colspan="3">Template file path provider configuration(TemplateFilePathProvider)</th>
   * </tr>
   * <tr>
   * <td>template-file.path-provider.prefix</td>
   * <td>The prefix for adding to template file path</td>
   * <td>{@code ""}</td>
   * </tr>
   * <tr>
   * <td>template-file.path-provider.includes-package-path</td>
   * <td>Whether includes package path part</td>
   * <td>{@code true}</td>
   * </tr>
   * <tr>
   * <td>template-file.path-provider.separate-directory-per-mapper</td>
   * <td>Whether separate directory per mapper</td>
   * <td>{@code true}</td>
   * </tr>
   * <tr>
   * <td>template-file.path-provider.includes-mapper-name-when-separate-directory</td>
   * <td>Whether includes mapper name into file name when separate directory per mapper</td>
   * <td>{@code true}</td>
   * </tr>
   * <tr>
   * <td>template-file.path-provider.cache-enabled</td>
   * <td>Whether cache a resolved template file path</td>
   * <td>{@code true}</td>
   * </tr>
   * </table>
   *
   * @return a configuration instance
   * @see SqlGeneratorConfig#newInstance()
   */
  public static ThymeleafLanguageDriverConfig newInstance() {
    ThymeleafLanguageDriverConfig config = new ThymeleafLanguageDriverConfig();
    applyDefaultProperties(config);
    return config;
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
   * @see SqlGeneratorConfig#newInstance()
   */
  public static ThymeleafLanguageDriverConfig newInstance(String resourcePath) {
    ThymeleafLanguageDriverConfig config = new ThymeleafLanguageDriverConfig();
    applyResourcePath(config, resourcePath);
    return config;
  }

  /**
   * Create an instance from specified properties.
   *
   * @param customProperties
   *          custom configuration properties
   * @return a configuration instance
   * @see #newInstance()
   * @see SqlGeneratorConfig#newInstance()
   */
  public static ThymeleafLanguageDriverConfig newInstance(Properties customProperties) {
    ThymeleafLanguageDriverConfig config = new ThymeleafLanguageDriverConfig();
    applyProperties(config, customProperties);
    return config;
  }

  /**
   * Create an instance using specified customizer and override using a default properties file.
   *
   * @param customizer
   *          baseline customizer
   * @return a configuration instance
   * @see #newInstance()
   * @see SqlGeneratorConfig#newInstance()
   */
  public static ThymeleafLanguageDriverConfig newInstance(Consumer<ThymeleafLanguageDriverConfig> customizer) {
    ThymeleafLanguageDriverConfig config = new ThymeleafLanguageDriverConfig();
    customizer.accept(config);
    applyDefaultProperties(config);
    return config;
  }

}
