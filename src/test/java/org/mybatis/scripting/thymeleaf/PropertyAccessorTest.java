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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PropertyAccessorTest {

  @Test
  void propertyNotFoundWhenGetPropertyType() {
    IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
        () -> PropertyAccessor.BuiltIn.STANDARD.getPropertyType(SqlGeneratorConfig.class, "id"));
    Assertions.assertEquals(
        "Does not get a property type because property 'id' not found on 'org.mybatis.scripting.thymeleaf.SqlGeneratorConfig' class.",
        e.getMessage());
  }

  @Test
  void propertyNotFoundWhenGetPropertyValue() {
    IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
        () -> PropertyAccessor.BuiltIn.STANDARD.getPropertyValue(new SqlGeneratorConfig(), "foo"));
    Assertions.assertEquals(
        "Does not get a property value because property 'foo' not found on 'org.mybatis.scripting.thymeleaf.SqlGeneratorConfig' class.",
        e.getMessage());
  }

  @Test
  void propertyNotFoundWhenSetPropertyValue() {
    IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
        () -> PropertyAccessor.BuiltIn.STANDARD.setPropertyValue(new SqlGeneratorConfig(), "bar", "aaa"));
    Assertions.assertEquals(
        "Does not set a property value because property 'bar' not found on 'org.mybatis.scripting.thymeleaf.SqlGeneratorConfig' class.",
        e.getMessage());
  }

  @Test
  void errorWhenGetPropertyValue() {
    IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
        () -> PropertyAccessor.BuiltIn.STANDARD.getPropertyValue(new Bean(), "id"));
    Assertions.assertEquals("java.lang.reflect.InvocationTargetException", e.getMessage());
  }

  @Test
  void errorWhenSetPropertyValue() {
    IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
        () -> PropertyAccessor.BuiltIn.STANDARD.setPropertyValue(new Bean(), "id", 10));
    Assertions.assertEquals("java.lang.reflect.InvocationTargetException", e.getMessage());
  }

  static class Bean {
    private Integer id;

    public Integer getId() {
      throw new IllegalStateException("test");
    }

    public void setId(Integer id) {
      throw new IllegalStateException("test");
    }
  }

}
