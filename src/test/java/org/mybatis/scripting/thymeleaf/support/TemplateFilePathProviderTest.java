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
package org.mybatis.scripting.thymeleaf.support;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriverConfig;

@DisabledIfSystemProperty(named = "mybatis.version", matches = "3\\.4\\..*|3\\.5\\.0")
class TemplateFilePathProviderTest {

  @BeforeEach
  @AfterEach
  void clean() {
    TemplateFilePathProvider.setCustomTemplateFilePathGenerator(null);
    TemplateFilePathProvider.setLanguageDriverConfig(null);
  }

  @Test
  void withoutDatabaseId() {
    String path = TemplateFilePathProvider.providePath(TestMapper.class, extractMethod(TestMapper.class, "update"),
        null);
    Assertions.assertEquals("org/mybatis/scripting/thymeleaf/support/TestMapper/TestMapper-update.sql", path);
  }

  @Test
  void withDatabaseId() {
    String path = TemplateFilePathProvider.providePath(TestMapper.class, extractMethod(TestMapper.class, "update"),
        "h2");
    Assertions.assertEquals("org/mybatis/scripting/thymeleaf/support/TestMapper/TestMapper-update-h2.sql", path);
  }

  @Test
  void fallbackWithDefaultDatabase() {
    String path = TemplateFilePathProvider.providePath(TestMapper.class, extractMethod(TestMapper.class, "delete"),
        "h2");
    Assertions.assertEquals("org/mybatis/scripting/thymeleaf/support/TestMapper/TestMapper-delete.sql", path);
  }

  @Test
  void fallbackDeclaringClassWithoutDatabaseId() {
    String path = TemplateFilePathProvider.providePath(TestMapper.class, extractMethod(TestMapper.class, "insert"),
        null);
    Assertions.assertEquals("org/mybatis/scripting/thymeleaf/support/BaseMapper/BaseMapper-insert.sql", path);
  }

  @Test
  void fallbackDeclaringClassWithDatabaseId() {
    String path = TemplateFilePathProvider.providePath(TestMapper.class, extractMethod(TestMapper.class, "insert"),
        "h2");
    Assertions.assertEquals("org/mybatis/scripting/thymeleaf/support/BaseMapper/BaseMapper-insert-h2.sql", path);
  }

  @Test
  void fallbackDeclaringClassAndDefaultDatabase() {
    String path = TemplateFilePathProvider.providePath(TestMapper.class, extractMethod(TestMapper.class, "count"),
        "h2");
    Assertions.assertEquals("org/mybatis/scripting/thymeleaf/support/BaseMapper/BaseMapper-count.sql", path);
  }

  @Test
  void notFoundSqlFile() {
    IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> TemplateFilePathProvider
        .providePath(TestMapper.class, extractMethod(TestMapper.class, "selectOne"), "h2"));
    Assertions.assertEquals(
        "The SQL template file not found. mapperType:[interface org.mybatis.scripting.thymeleaf.support.TestMapper] mapperMethod:[public abstract java.lang.Object org.mybatis.scripting.thymeleaf.support.BaseMapper.selectOne(int)] databaseId:[h2]",
        e.getMessage());
  }

  @Test
  void notFoundSqlFileWithoutDatabaseId() {
    IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> TemplateFilePathProvider
        .providePath(TestMapper.class, extractMethod(TestMapper.class, "selectOne"), null));
    Assertions.assertEquals(
        "The SQL template file not found. mapperType:[interface org.mybatis.scripting.thymeleaf.support.TestMapper] mapperMethod:[public abstract java.lang.Object org.mybatis.scripting.thymeleaf.support.BaseMapper.selectOne(int)] databaseId:[null]",
        e.getMessage());
  }

  @Test
  void notFoundSqlFileWithoutFallbackDeclaringClass() {
    IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> TemplateFilePathProvider
        .providePath(TestMapper.class, extractMethod(TestMapper.class, "selectAllByFirstName"), null));
    Assertions.assertEquals(
        "The SQL template file not found. mapperType:[interface org.mybatis.scripting.thymeleaf.support.TestMapper] mapperMethod:[public abstract java.util.List org.mybatis.scripting.thymeleaf.support.TestMapper.selectAllByFirstName(java.lang.String)] databaseId:[null]",
        e.getMessage());
  }

  @Test
  void includesPackagePathAndSeparatesDirectoryPerMapperIsFalse() {
    TemplateFilePathProvider.setLanguageDriverConfig(ThymeleafLanguageDriverConfig.newInstance(c -> {
      c.getTemplateFile().setBaseDir("org/mybatis/scripting/thymeleaf/support/sql");
      c.getTemplateFile().getPathProvider().setIncludesPackagePath(false);
      c.getTemplateFile().getPathProvider().setSeparateDirectoryPerMapper(false);
    }));
    String path = TemplateFilePathProvider.providePath(TestMapper.class,
        extractMethod(TestMapper.class, "selectAllDesc"), null);
    Assertions.assertEquals("TestMapper-selectAllDesc.sql", path);
  }

  @Test
  void baseDirEndWithSlash() {
    TemplateFilePathProvider.setLanguageDriverConfig(ThymeleafLanguageDriverConfig.newInstance(c -> {
      c.getTemplateFile().setBaseDir("org/mybatis/scripting/thymeleaf/support/sql/");
      c.getTemplateFile().getPathProvider().setIncludesPackagePath(false);
      c.getTemplateFile().getPathProvider().setSeparateDirectoryPerMapper(false);
    }));
    String path = TemplateFilePathProvider.providePath(TestMapper.class,
        extractMethod(TestMapper.class, "selectAllDesc"), null);
    Assertions.assertEquals("TestMapper-selectAllDesc.sql", path);
  }

  @Test
  void includesMapperNameWhenSeparateDirectoryIsFalse() {
    TemplateFilePathProvider.setLanguageDriverConfig(ThymeleafLanguageDriverConfig
        .newInstance(c -> c.getTemplateFile().getPathProvider().setIncludesMapperNameWhenSeparateDirectory(false)));
    String path = TemplateFilePathProvider.providePath(TestMapper.class,
        extractMethod(TestMapper.class, "selectAllAsc"), null);
    Assertions.assertEquals("org/mybatis/scripting/thymeleaf/support/TestMapper/selectAllAsc.sql", path);
  }

  @Test
  void prefix() {
    TemplateFilePathProvider.setLanguageDriverConfig(ThymeleafLanguageDriverConfig.newInstance(c -> {
      c.getTemplateFile().getPathProvider().setPrefix("org/mybatis/scripting/thymeleaf/support/sql/");
      c.getTemplateFile().getPathProvider().setIncludesPackagePath(false);
      c.getTemplateFile().getPathProvider().setSeparateDirectoryPerMapper(false);
    }));
    String path = TemplateFilePathProvider.providePath(TestMapper.class,
        extractMethod(TestMapper.class, "selectAllDesc"), null);
    Assertions.assertEquals("org/mybatis/scripting/thymeleaf/support/sql/TestMapper-selectAllDesc.sql", path);
  }

  @Test
  void defaultPackageMapper() throws ClassNotFoundException {
    TemplateFilePathProvider.setLanguageDriverConfig(ThymeleafLanguageDriverConfig
        .newInstance(c -> c.getTemplateFile().setBaseDir("org/mybatis/scripting/thymeleaf/support/")));
    Class<?> mapperType = Class.forName("DefaultPackageNameMapper");
    String path = TemplateFilePathProvider.providePath(mapperType, extractMethod(mapperType, "selectAllDesc"), null);
    Assertions.assertEquals("DefaultPackageNameMapper/DefaultPackageNameMapper-selectAllDesc.sql", path);
  }

  @Test
  void defaultPackageMapperWithIncludesPackagePathIsFalse() throws ClassNotFoundException {
    TemplateFilePathProvider.setLanguageDriverConfig(ThymeleafLanguageDriverConfig.newInstance(c -> {
      c.getTemplateFile().setBaseDir("org/mybatis/scripting/thymeleaf/support/");
      c.getTemplateFile().getPathProvider().setIncludesPackagePath(false);
    }));
    Class<?> mapperType = Class.forName("DefaultPackageNameMapper");
    String path = TemplateFilePathProvider.providePath(mapperType, extractMethod(mapperType, "selectAllDesc"), null);
    Assertions.assertEquals("DefaultPackageNameMapper/DefaultPackageNameMapper-selectAllDesc.sql", path);
  }

  @Test
  void customTemplateFileGenerator() {
    TemplateFilePathProvider.setCustomTemplateFilePathGenerator(
        (type, method, databaseId) -> type.getName().replace('.', '/') + "_" + method.getName() + ".sql");
    String path = TemplateFilePathProvider.providePath(TestMapper.class, extractMethod(TestMapper.class, "selectOne"),
        null);
    Assertions.assertEquals("org/mybatis/scripting/thymeleaf/support/BaseMapper_selectOne.sql", path);

  }

  private Method extractMethod(Class<?> type, String methodName) {
    return Arrays.stream(type.getMethods()).filter(m -> m.getName().equals(methodName)).findFirst().orElseThrow(
        () -> new IllegalArgumentException("The method not found. type:" + type + " methodName:" + methodName));
  }

}
