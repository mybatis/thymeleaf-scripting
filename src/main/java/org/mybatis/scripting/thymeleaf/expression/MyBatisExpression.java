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
package org.mybatis.scripting.thymeleaf.expression;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

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

  private char escapeChar = '\\';

  private Set<Character> additionalEscapeTargetChars = Collections.emptySet();

  private Function<Character, String> escapeClauseSupplier = targetEscapeChar -> " ESCAPE '" + targetEscapeChar + "' ";

  /**
   * Construct new instance that corresponds with specified configuration.
   */
  private MyBatisExpression() {
    // NOP
  }

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

  /**
   * Escape for LIKE condition value.
   * <br>
   * By default configuration, this method escape the {@code "%"} and {@code "_"} using {@code "\"}.
   * @param value A target condition value
   * @return A escaped value
   */
  public String escapeLikeWildcard(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(value.length() + 16);
    for (char c : value.toCharArray()) {
      if (c == escapeChar || c == '%' || c == '_' || additionalEscapeTargetChars.contains(c)) {
        sb.append(escapeChar);
      }
      sb.append(c);
    }
    return sb.toString();
  }

  /**
   * Return a escape clause string of LIKE.
   * <br>
   * By default configuration, this method return {@code " ESCAPE '\' "}.
   * @return A escape clause string of LIKE
   */
  public String likeEscapeClause() {
    return escapeClauseSupplier.apply(escapeChar);
  }

  /**
   * Creates a new builder instance for {@link MyBatisExpression}.
   * @return a new builder instance
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * The builder class for {@link MyBatisExpression}.
   */
  public static class Builder {

    private final MyBatisExpression expression = new MyBatisExpression();

    private Builder() {
      // NOP
    }

    /**
     * Set an escape character for wildcard of LIKE.
     * <br>
     * The default value is {@code '\'} (backslash)
     * @param escapeChar A escape character
     * @return A self instance
     */
    public Builder likeEscapeChar(Character escapeChar) {
      Optional.ofNullable(escapeChar).ifPresent(v -> expression.escapeChar = v);
      return this;
    }

    /**
     * Set additional escape target characters(custom wildcard characters) for LIKE condition.
     * <br>
     * The default value is nothing.
     *
     * @param additionalEscapeTargetChars escape target characters(custom wildcard characters)
     * @return A self instance
     */
    public Builder likeAdditionalEscapeTargetChars(Set<Character> additionalEscapeTargetChars) {
      Optional.ofNullable(additionalEscapeTargetChars).ifPresent(v -> expression.additionalEscapeTargetChars = v);
      return this;
    }

    /**
     * Set a format of escape clause.
     * <br>
     * The default value is {@code " ESCAPE '%s' "}.
     *
     * @param escapeClauseFormat a format of escape clause
     * @return A self instance
     */
    public Builder likeEscapeClauseFormat(String escapeClauseFormat) {
      Optional.ofNullable(escapeClauseFormat)
          .ifPresent(v -> expression.escapeClauseSupplier = escapeChar -> String.format(v, escapeChar));
      return this;
    }

    /**
     * Return a {@link MyBatisExpression} instance .
     * @return A {@link MyBatisExpression} instance corresponding with specified option
     */
    public MyBatisExpression build() {
      return expression;
    }

  }

}
