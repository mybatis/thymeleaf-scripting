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
package org.mybatis.scripting.thymeleaf.expression;

import org.thymeleaf.engine.IterationStatusVar;

/**
 * The expression utility object that provide helper method for generating SQL.
 * <br>
 * This object can be access using {@code #mybatis}) as expression utility object.
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
public class MyBatisExpression {

  /**
   * Return the comma if a current iteration status is not first.
   *
   * @param iterationStatus A current iteration status
   * @return If iteration status is not first, return comma and otherwise return empty string
   */
  public String commaIfNotFirst(IterationStatusVar iterationStatus) {
    return iterationStatus.isFirst() ? "" : ",";
  }

  /**
   * Return the comma if a current iteration status is not last.
   *
   * @param iterationStatus A current iteration status
   * @return If iteration status is not last, return comma and otherwise return empty string
   */
  public String commaIfNotLast(IterationStatusVar iterationStatus) {
    return iterationStatus.isLast() ? "" : ",";
  }

}
