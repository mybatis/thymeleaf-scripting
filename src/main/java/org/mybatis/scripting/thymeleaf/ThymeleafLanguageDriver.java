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

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.mybatis.scripting.thymeleaf.support.TemplateFilePathProvider;
import org.thymeleaf.ITemplateEngine;

/**
 * The {@code LanguageDriver} for integrating with Thymeleaf.
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
public class ThymeleafLanguageDriver implements LanguageDriver {

  private final SqlGenerator sqlGenerator;

  /**
   * Constructor for creating instance with default {@code TemplateEngine}.
   */
  public ThymeleafLanguageDriver() {
    this.sqlGenerator = configure(new SqlGenerator(ThymeleafLanguageDriverConfig.newInstance()));
  }

  /**
   * Constructor for creating instance with user specified {@code Properties}.
   *
   * @param config
   *          A user defined {@code ITemplateEngine} instance
   */
  public ThymeleafLanguageDriver(ThymeleafLanguageDriverConfig config) {
    this.sqlGenerator = configure(new SqlGenerator(config));
    TemplateFilePathProvider.setLanguageDriverConfig(config);
  }

  /**
   * Constructor for creating instance with user defined {@code ITemplateEngine}.
   *
   * @param templateEngine
   *          A user defined {@code ITemplateEngine} instance
   */
  public ThymeleafLanguageDriver(ITemplateEngine templateEngine) {
    this.sqlGenerator = configure(new SqlGenerator(templateEngine));
  }

  private SqlGenerator configure(SqlGenerator sqlGenerator) {
    sqlGenerator.setContextFactory(new ThymeleafSqlSource.ContextFactory());
    return sqlGenerator;
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
    return new ThymeleafSqlSource(configuration, sqlGenerator, script.trim(), parameterType);
  }

}
