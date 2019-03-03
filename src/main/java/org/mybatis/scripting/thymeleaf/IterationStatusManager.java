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

import org.thymeleaf.engine.IterationStatusVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager class for using {@link IterationStatusVar} in template processing.
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
public class IterationStatusManager {

  private final Map<String, List<IterationStatusVar>> statusListMapping = new HashMap<>();

  /**
   * Constructor.
   */
  IterationStatusManager() {
    // NOP
  }

  /**
   * Generate an unique variable name per iteration object.
   *
   * @param objectName base object name
   * @param status iteration status object
   * @return an unique variable name per iteration object
   */
  public String generateUniqueName(String objectName, IterationStatusVar status) {
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