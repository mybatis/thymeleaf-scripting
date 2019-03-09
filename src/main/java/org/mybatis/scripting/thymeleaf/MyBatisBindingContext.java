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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.thymeleaf.context.IContext;
import org.thymeleaf.engine.IterationStatusVar;

/**
 * The context object for integrating with MyBatis and Thymeleaf template engine.
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
public class MyBatisBindingContext {

  static final String CONTEXT_VARIABLE_NAME = "_" + MyBatisBindingContext.class.getName();

  private final Map<String, Object> customBindVariables = new HashMap<>();
  private final IterationStatusManager iterationStatusManager = new IterationStatusManager();
  private final boolean fallbackParameterObject;

  /**
   * Load instance from {@link IContext} provided by Thymeleaf.
   *
   * @param context a context of thymeleaf template processing
   * @return instance of this class
   */
  public static MyBatisBindingContext load(IContext context) {
    return (MyBatisBindingContext)context.getVariable(CONTEXT_VARIABLE_NAME);
  }

  /**
   * Constructor.
   *
   * @param fallbackParameterObject whether use fallback parameter object when parameter is value object
   */
  MyBatisBindingContext(boolean fallbackParameterObject) {
    this.fallbackParameterObject = fallbackParameterObject;
  }

  /**
   * Get custom bind variables.
   *
   * @return custom bind variables
   */
  Map<String, Object> getCustomBindVariables() {
    return customBindVariables;
  }

  /**
   * Set a value into custom bind variable.
   *
   * @param name variable name
   * @param value variable value
   */
  public void setCustomBindVariable(String name, Object value) {
    customBindVariables.put(name, value);
  }

  /**
   * Return whether contains specified variable into custom bind variables.
   * @param name variable name
   * @return If specified variable exists, return {@code true}
   */
  public boolean containsCustomBindVariable(String name) {
    return customBindVariables.containsKey(name);
  }

  /**
   * Generate an unique variable name per iteration object.
   * <br>
   * Variable name rule is {@code {objectName}_{status list index}_{status.getIndex()}}.
   *
   * @param objectName base object name
   * @param status iteration status object
   * @return an unique variable name per iteration object
   */
  public String generateUniqueName(String objectName, IterationStatusVar status) {
    return iterationStatusManager.generateUniqueName(objectName, status);
  }

  /**
   * Return whether use fallback parameter object when parameter is value object.
   *
   * @return If use fallback parameter object, return {@code true}
   */
  boolean isFallbackParameterObject() {
    return fallbackParameterObject;
  }

  private static class IterationStatusManager {

    private final Map<String, List<IterationStatusVar>> statusListMapping = new HashMap<>();

    private String generateUniqueName(String objectName, IterationStatusVar status) {
      List<IterationStatusVar> statusList = statusListMapping.computeIfAbsent(objectName, k -> new ArrayList<>());
      int index;
      if (!statusList.contains(status)) {
        index = statusList.size();
        statusList.add(status);
      } else {
        index = statusList.indexOf(status);
      }
      return objectName + "_" + index + "_" + status.getIndex();
    }

  }

}