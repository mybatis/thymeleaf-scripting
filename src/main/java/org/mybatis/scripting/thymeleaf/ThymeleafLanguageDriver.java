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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.mybatis.scripting.thymeleaf.expression.Likes;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * The {@code LanguageDriver} for integrating with Thymeleaf.
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
public class ThymeleafLanguageDriver implements LanguageDriver {

  private final ITemplateEngine templateEngine;

  /**
   * Constructor for creating instance with default {@code TemplateEngine}.
   */
  public ThymeleafLanguageDriver() {
    this.templateEngine = createDefaultTemplateEngine(ThymeleafLanguageDriverConfig.newInstance());
  }

  /**
   * Constructor for creating instance with user specified {@code Properties}.
   *
   * @param config
   *          A user defined {@code ITemplateEngine} instance
   */
  public ThymeleafLanguageDriver(ThymeleafLanguageDriverConfig config) {
    this.templateEngine = createDefaultTemplateEngine(config);
  }

  /**
   * Constructor for creating instance with user defined {@code ITemplateEngine}.
   *
   * @param templateEngine
   *          A user defined {@code ITemplateEngine} instance
   */
  public ThymeleafLanguageDriver(ITemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  private ITemplateEngine createDefaultTemplateEngine(ThymeleafLanguageDriverConfig config) {
    MyBatisDialect dialect = new MyBatisDialect(config.getDialect().getPrefix());
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
    final TemplateEngineCustomizer customizer = Optional.ofNullable(config.getCustomizer()).map(v -> {
      try {
        return v.getConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new IllegalStateException("Cannot create an instance for class: " + v, e);
      }
    }).map(TemplateEngineCustomizer.class::cast).orElse(TemplateEngineCustomizer.BuiltIn.DO_NOTHING);
    customizer.accept(targetTemplateEngine);

    return targetTemplateEngine;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject,
      BoundSql boundSql) {
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
    return new ThymeleafSqlSource(configuration, templateEngine, script.trim(), parameterType);
  }

}
