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

/**
 * Constants for reserved special variable names.
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
public class ContextVariableNames {

  private ContextVariableNames() {
    // NOP
  }

  /**
   * Variable name for holding custom bind variables.
   *
   * @see org.mybatis.scripting.thymeleaf.processor.MyBatisBindTagProcessor
   */
  public static final String CUSTOM_BIND_VARS = "_customBindVariables";

  /**
   * Variable name for holding whether use fallback parameter object when parameter is value object.
   *
   * @see MyBatisIntegratingEngineContextFactory
   */
  public static final String FALLBACK_PARAMETER_OBJECT = "_fallbackParameterObject";

}
