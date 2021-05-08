/*
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

import java.lang.reflect.Proxy;
import java.util.Map;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.IEngineContext;
import org.thymeleaf.context.IEngineContextFactory;
import org.thymeleaf.engine.TemplateData;

/**
 * The implementation of {@link IEngineContextFactory} for integrating with MyBatis.
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
public class MyBatisIntegratingEngineContextFactory implements IEngineContextFactory {
  private final IEngineContextFactory delegate;
  private final ClassLoader classLoader = getClass().getClassLoader();

  /**
   * Constructor.
   *
   * @param delegate
   *          A target context factory for delegating
   */
  public MyBatisIntegratingEngineContextFactory(IEngineContextFactory delegate) {
    this.delegate = delegate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IEngineContext createEngineContext(IEngineConfiguration configuration, TemplateData templateData,
      Map<String, Object> templateResolutionAttributes, IContext context) {
    IEngineContext engineContext = delegate.createEngineContext(configuration, templateData,
        templateResolutionAttributes, context);
    return (IEngineContext) Proxy.newProxyInstance(classLoader, new Class[] { IEngineContext.class },
        (proxy, method, args) -> {
          if (method.getName().equals("getVariable")) {
            String name = (String) args[0];
            Object value;
            MyBatisBindingContext bindingContext = MyBatisBindingContext.load(engineContext);
            if (bindingContext.isFallbackParameterObject()) {
              value = engineContext.containsVariable(name) ? engineContext.getVariable(name)
                  : engineContext.getVariable(SqlGenerator.ContextKeys.PARAMETER_OBJECT);
            } else {
              value = engineContext.getVariable(name);
            }
            return value;
          }
          return method.invoke(engineContext, args);
        });
  }

}