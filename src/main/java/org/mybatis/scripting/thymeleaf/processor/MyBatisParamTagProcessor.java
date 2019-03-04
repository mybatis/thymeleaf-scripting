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
package org.mybatis.scripting.thymeleaf.processor;

import org.mybatis.scripting.thymeleaf.MyBatisBindingContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.engine.IterationStatusVar;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * The processor class for handling the {@code mybatis:p} tag.
 * <br>
 * This class will output a bind variable expression such as {@code #{...}}.
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
public class MyBatisParamTagProcessor extends AbstractAttributeTagProcessor {

  private static final int PRECEDENCE = 1400;
  private static final String ATTR_NAME = "p";

  /**
   * Constructor that can be specified the template mode and dialect prefix.
   * @param templateMode A target template mode
   * @param prefix A target dialect prefix
   */
  public MyBatisParamTagProcessor(final TemplateMode templateMode, final String prefix) {
    super(templateMode, prefix, null, false, ATTR_NAME, true, PRECEDENCE, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
          String attributeValue, IElementTagStructureHandler structureHandler) {
    int optionStartIndex = attributeValue.indexOf(',');
    String parameterValue;
    String options;
    if (optionStartIndex == -1) {
      parameterValue = attributeValue;
      options = "";
    } else {
      parameterValue = attributeValue.substring(0, optionStartIndex);
      options = attributeValue.substring(optionStartIndex);
    }
    int nestedPropertyPathStartIndex = parameterValue.indexOf('.');
    String objectName;
    String propertyPath;
    if (nestedPropertyPathStartIndex == -1) {
      objectName = parameterValue;
      propertyPath = "";
    } else {
      objectName = parameterValue.substring(0, nestedPropertyPathStartIndex);
      propertyPath = parameterValue.substring(nestedPropertyPathStartIndex);
    }
    String iterationObjectKey = objectName + "Stat";
    if (context.containsVariable(iterationObjectKey)) {
      @SuppressWarnings("unchecked")
      MyBatisBindingContext bindingContext =
          (MyBatisBindingContext) context.getVariable(MyBatisBindingContext.CONTEXT_VARIABLE_NAME);
      IterationStatusVar iterationStatus = (IterationStatusVar) context.getVariable(iterationObjectKey);
      String iterationObjectVariableName = bindingContext.generateUniqueName(objectName, iterationStatus);
      if (!bindingContext.containsCustomBindVariable(iterationObjectVariableName)) {
        bindingContext.setCustomBindVariable(iterationObjectVariableName, iterationStatus.getCurrent());
      }
      structureHandler.setBody("#{" + iterationObjectVariableName + propertyPath + options + "}", false);
    } else {
      structureHandler.setBody("#{" + objectName + propertyPath + options + "}", false);
    }
  }

}
