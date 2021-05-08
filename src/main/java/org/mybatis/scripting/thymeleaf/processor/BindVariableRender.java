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
package org.mybatis.scripting.thymeleaf.processor;

import java.util.function.UnaryOperator;

import org.mybatis.scripting.thymeleaf.support.spring.SpringNamedParameterBindVariableRender;

/**
 * The interface for rendering a bind variable. <br>
 * If you want to customize a default {@code BindVariableRender}, you implements class of this interface and you need to
 * specify the 'dialect.bind-variable-render' property of mybatis-thymeleaf.properties. <br>
 * <br>
 * e.g.) Implementation class:
 *
 * <pre>
 * package com.example;
 *
 * // ...
 * public class R2dbcMySQLBindVariableRender extends EnclosingBasedBindVariableRender {
 *   public R2dbcMySQLBindVariableRender() {
 *     super("?", ""); // Render '?...' (e.g. ?id)
 *   }
 * }
 * </pre>
 *
 * <br>
 * e.g.) Configuration file (mybatis-thymeleaf.properties):
 *
 * <pre>
 * dialect.bind-variable-render = com.example.MyBindVariableRender
 * </pre>
 *
 * @author Kazuki Shimizu
 * @version 1.0.2
 */
@FunctionalInterface
public interface BindVariableRender extends UnaryOperator<String> {

  /**
   * {@inheritDoc}
   *
   * @see #render(String)
   */
  @Override
  default String apply(String name) {
    return render(name);
  }

  /**
   * Render a bind variable.
   *
   * @param name
   *          a bind variable name
   * @return a bind variable
   */
  String render(String name);

  /**
   * The built-in bind variable renders.
   */
  enum BuiltIn implements BindVariableRender {

    /**
     * The render for MyBatis core named parameter format(.e.g {@literal #{id}}).
     * <p>
     * This is default.
     * </p>
     */
    MYBATIS(name -> "#{" + name + "}"),
    /**
     * The render for Spring JDBC named parameter format(.e.g {@literal :id}).
     */
    SPRING_NAMED_PARAMETER(new SpringNamedParameterBindVariableRender());

    private final BindVariableRender delegate;

    BuiltIn(BindVariableRender delegate) {
      this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String render(String name) {
      return delegate.render(name);
    }

    /**
     * Get a type of the actual {@link BindVariableRender}.
     *
     * @return a type of delegating {@link BindVariableRender}
     */
    public Class<? extends BindVariableRender> getType() {
      return delegate.getClass();
    }

  }

}
