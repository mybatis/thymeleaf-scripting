/*
 *    Copyright 2018-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.scripting.thymeleaf;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;

class TemplateEngineCustomizerTest {

  @Test
  void testNoMatchType() {
    Configuration configuration = new Configuration();
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);
    new SqlSessionFactoryBuilder().build(configuration);

    TemplateEngine templateEngine = DefaultTemplateEngineCustomizer.templateEngine;
    Assertions.assertFalse(
        TemplateEngineCustomizer.extractTemplateResolver(templateEngine, FileTemplateResolver.class).isPresent());
  }

}
