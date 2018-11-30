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

import org.mybatis.scripting.thymeleaf.expression.MyBatisExpression;
import org.mybatis.scripting.thymeleaf.processor.MyBatisBindTagProcessor;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The Dialect for integrating with MyBatis.
 * <br>
 * This dialect provides following features.
 *
 * <ul>
 *   <li>{@code #mybatis} : {@link MyBatisExpression}</li>
 *   <li>{@code mybatis:bind} : {@link MyBatisBindTagProcessor}</li>
 * </ul>
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
public class MyBatisDialect extends AbstractProcessorDialect implements IExpressionObjectDialect {

  private static final MyBatisExpression.Builder expressionBuilder = MyBatisExpression.newBuilder();

  /**
   * Default constructor.
   */
  public MyBatisDialect() {
    this("mybatis");
  }

  /**
   * Constructor that can be specified the dialect prefix.
   *
   * @param prefix A dialect prefix(also use as expression object name)
   */
  public MyBatisDialect(String prefix) {
    super("MyBatis Dialect", prefix, StandardDialect.PROCESSOR_PRECEDENCE);
  }

  /**
   * Set an escape character for wildcard of LIKE.
   * <br>
   * The default value is {@code '\'} (backslash)
   * @param escapeChar A escape character
   */
  public void setLikeEscapeChar(Character escapeChar) {
    expressionBuilder.likeEscapeChar(escapeChar);
  }

  /**
   * Set a format of escape clause.
   * <br>
   * The default value is {@code " ESCAPE '%s' "}.
   *
   * @param escapeClauseFormat a format of escape clause
   */
  public void setLikeEscapeClauseFormat(String escapeClauseFormat) {
    expressionBuilder.likeEscapeClauseFormat(escapeClauseFormat);
  }

  /**
   * Set additional escape target characters(custom wildcard characters) for LIKE condition.
   * <br>
   * The default value is nothing.
   *
   * @param additionalEscapeTargetChars escape target characters(custom wildcard characters)
   */
  public void setLikeAdditionalEscapeTargetChars(Set<Character> additionalEscapeTargetChars) {
    expressionBuilder.likeAdditionalEscapeTargetChars(additionalEscapeTargetChars);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<IProcessor> getProcessors(String dialectPrefix) {
    return new HashSet<>(Arrays.asList(
        new MyBatisBindTagProcessor(TemplateMode.TEXT, dialectPrefix)
        , new MyBatisBindTagProcessor(TemplateMode.CSS, dialectPrefix)
    ));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IExpressionObjectFactory getExpressionObjectFactory() {
    return new MyBatisExpressionObjectFactory();
  }

  private class MyBatisExpressionObjectFactory implements IExpressionObjectFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAllExpressionObjectNames() {
      return Collections.singleton(getPrefix());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object buildObject(IExpressionContext context, String expressionObjectName) {
      return expressionBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCacheable(String expressionObjectName) {
      return true;
    }

  }

}
