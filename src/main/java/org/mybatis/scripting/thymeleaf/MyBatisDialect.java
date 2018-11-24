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

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.engine.IterationStatusVar;
import org.thymeleaf.expression.IExpressionObjectFactory;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;

import java.util.Collections;
import java.util.Set;

/**
 * The Dialect for integrating with MyBatis.
 * <br>
 * This dialect provides the {@link MyBatisExpressionUtilityObject}.
 * It can be access using {@code #mybatis}) as expression utility object.
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
    return Collections.emptySet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IExpressionObjectFactory getExpressionObjectFactory() {
    return expressionObjectFactory;
  }

  private static class MyBatisExpressionObjectFactory implements IExpressionObjectFactory {

    private static final MyBatisExpressionUtilityObject EXPRESSION_OBJECT = new MyBatisExpressionUtilityObject();
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

  /**
   * The expression utility object that provide helper method for generating SQL.
   */
  public static class MyBatisExpressionUtilityObject {

    /**
     * Return the comma if a current iteration status is not first.
     *
     * @param iterationStatus A current iteration status
     * @return If iteration status is not first, return comma and otherwise return empty string
     */
    public String commaIfNotFirst(IterationStatusVar iterationStatus) {
      return !iterationStatus.isFirst() ? "," : "";
    }

    /**
     * Return the comma if a current iteration status is not last.
     *
     * @param iterationStatus A current iteration status
     * @return If iteration status is not last, return comma and otherwise return empty string
     */
    public String commaIfNotLast(IterationStatusVar iterationStatus) {
      return !iterationStatus.isLast() ? "," : "";
    }

  }

}
