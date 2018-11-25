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

  private final IExpressionObjectFactory expressionObjectFactory;

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
    this.expressionObjectFactory = new MyBatisExpressionObjectFactory(getPrefix());
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
    return expressionObjectFactory;
  }

  private static class MyBatisExpressionObjectFactory implements IExpressionObjectFactory {

    private static final MyBatisExpression EXPRESSION_OBJECT = new MyBatisExpression();
    private final Set<String> expressionNames;

    private MyBatisExpressionObjectFactory(String expressionName) {
      this.expressionNames = Collections.singleton(expressionName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAllExpressionObjectNames() {
      return expressionNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object buildObject(IExpressionContext context, String expressionObjectName) {
      return EXPRESSION_OBJECT;
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
