/*
 *    Copyright 2018-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The expression utility object that provide helper method for like feature. <br>
 * This object can be access using {@code #likes}) as expression utility object.
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
public class Likes {

  private char escapeChar = '\\';

  private Set<Character> additionalEscapeTargetChars = Collections.emptySet();

  private Function<Character, String> escapeClauseSupplier = targetEscapeChar -> "ESCAPE '" + targetEscapeChar + "'";

  /**
   * Construct new instance that corresponds with specified configuration.
   */
  private Likes() {
    // NOP
  }

  /**
   * Escape for LIKE condition value. <br>
   * By default configuration, this method escape the {@code "%"} and {@code "_"} using {@code "\"}.
   *
   * @param value
   *          A target condition value
   * @return A escaped value
   */
  public String escapeWildcard(String value) {
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
   * Return a escape clause string of LIKE. <br>
   * By default configuration, this method return {@code "ESCAPE '\'"}.
   *
   * @return A escape clause string of LIKE
   */
  public String escapeClause() {
    return escapeClauseSupplier.apply(escapeChar);
  }

  /**
   * Creates a new builder instance for {@link Likes}.
   *
   * @return a new builder instance
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * The builder class for {@link Likes}.
   */
  public static class Builder {

    private final Likes instance = new Likes();

    private Builder() {
      // NOP
    }

    /**
     * Set an escape character for wildcard of LIKE. <br>
     * The default value is {@code '\'} (backslash)
     *
     * @param escapeChar
     *          A escape character
     * @return A self instance
     */
    public Builder escapeChar(Character escapeChar) {
      Optional.ofNullable(escapeChar).ifPresent(v -> instance.escapeChar = v);
      return this;
    }

    /**
     * Set additional escape target characters(custom wildcard characters) for LIKE condition. <br>
     * The default value is nothing.
     *
     * @param additionalEscapeTargetChars
     *          escape target characters(custom wildcard characters)
     * @return A self instance
     */
    public Builder additionalEscapeTargetChars(Character... additionalEscapeTargetChars) {
      Optional.ofNullable(additionalEscapeTargetChars)
          .ifPresent(v -> instance.additionalEscapeTargetChars = Arrays.stream(v).collect(Collectors.toSet()));
      return this;
    }

    /**
     * Set a format of escape clause. <br>
     * The default value is {@code "ESCAPE '%s'"}.
     *
     * @param escapeClauseFormat
     *          a format of escape clause
     * @return A self instance
     */
    public Builder escapeClauseFormat(String escapeClauseFormat) {
      Optional.ofNullable(escapeClauseFormat)
          .ifPresent(v -> instance.escapeClauseSupplier = escapeChar -> String.format(v, escapeChar));
      return this;
    }

    /**
     * Return a {@link Likes} instance .
     *
     * @return A {@link Likes} instance corresponding with specified option
     */
    public Likes build() {
      return instance;
    }

  }

}
