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
package org.mybatis.scripting.thymeleaf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.scripting.thymeleaf.support.spring.SpringNamedParameterBindVariableRender;

class SqlGeneratorConfigTest {

  @Test
  void newInstanceWithResourcePath() {
    SqlGeneratorConfig config = SqlGeneratorConfig
        .newInstanceWithResourcePath("mybatis-thymeleaf-custom-without-path-provider.properties");
    Assertions.assertEquals(StandardCharsets.ISO_8859_1, config.getTemplateFile().getEncoding());
    Assertions.assertEquals(SpringNamedParameterBindVariableRender.class, config.getDialect().getBindVariableRender());
  }

  @Test
  void newInstanceWithProperties() {
    Properties properties = new Properties();
    properties.setProperty("template-file.encoding", "Windows-31J");
    SqlGeneratorConfig config = SqlGeneratorConfig.newInstanceWithProperties(properties);
    Assertions.assertEquals(Charset.forName("Windows-31J"), config.getTemplateFile().getEncoding());
  }

  @Test
  void invalidKey() {
    Properties properties = new Properties();
    properties.setProperty("template-file.encodings", "Windows-31J");

    IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
        () -> SqlGeneratorConfig.newInstanceWithProperties(properties));
    Assertions.assertEquals("Detected an invalid property. key='template-file.encodings' value='Windows-31J'",
        e.getMessage());
    Assertions.assertEquals(IllegalArgumentException.class, e.getCause().getClass());
    Assertions.assertEquals(
        "Does not get a property type because property 'encodings' not found on 'org.mybatis.scripting.thymeleaf.SqlGeneratorConfig$TemplateFileConfig' class.",
        e.getCause().getMessage());
  }

  @Test
  void invalidValue() {
    Properties properties = new Properties();
    properties.setProperty("template-file.encoding", "UTF-77");

    IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
        () -> SqlGeneratorConfig.newInstanceWithProperties(properties));
    Assertions.assertEquals("Detected an invalid property. key='template-file.encoding' value='UTF-77'",
        e.getMessage());
    Assertions.assertEquals(UnsupportedCharsetException.class, e.getCause().getClass());
    Assertions.assertEquals("UTF-77", e.getCause().getMessage());
  }
}
