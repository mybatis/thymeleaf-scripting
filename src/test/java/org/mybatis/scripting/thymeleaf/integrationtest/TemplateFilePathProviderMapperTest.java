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
package org.mybatis.scripting.thymeleaf.integrationtest;

import java.io.Reader;
import java.sql.Connection;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriver;
import org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriverConfig;
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Name;
import org.mybatis.scripting.thymeleaf.integrationtest.mapper.TemplateFilePathProviderMapper;
import org.mybatis.scripting.thymeleaf.support.TemplateFilePathProvider;

@DisabledIfSystemProperty(named = "mybatis.version", matches = "3\\.4\\..*|3\\.5\\.0")
class TemplateFilePathProviderMapperTest {
  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  @AfterAll
  static void cleanup() {
    TemplateFilePathProvider.clearCache();
  }

  @BeforeAll
  static void setUp() throws Exception {
    Class.forName("org.hsqldb.jdbcDriver");
    JDBCDataSource dataSource = new JDBCDataSource();
    dataSource.setUrl("jdbc:hsqldb:mem:db1");
    dataSource.setUser("sa");
    dataSource.setPassword("");

    try (Connection conn = dataSource.getConnection()) {
      try (Reader reader = Resources.getResourceAsReader("create-db.sql")) {
        ScriptRunner runner = new ScriptRunner(conn);
        runner.setLogWriter(null);
        runner.setErrorLogWriter(null);
        runner.runScript(reader);
        conn.commit();
      }
    }

    TransactionFactory transactionFactory = new JdbcTransactionFactory();
    Environment environment = new Environment("development", transactionFactory, dataSource);

    Configuration configuration = new Configuration(environment);
    configuration.setMapUnderscoreToCamelCase(true);
    configuration.getLanguageRegistry()
        .register(new ThymeleafLanguageDriver(ThymeleafLanguageDriverConfig.newInstance(c -> {
          c.getTemplateFile().getPathProvider().setPrefix("sql/");
          c.getTemplateFile().getPathProvider().setIncludesPackagePath(false);
        })));
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);

    configuration.addMapper(TemplateFilePathProviderMapper.class);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
  }

  @Test
  void testInsert() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      TemplateFilePathProviderMapper mapper = sqlSession.getMapper(TemplateFilePathProviderMapper.class);
      Name name = new Name();
      name.setFirstName("Thymeleaf");
      name.setLastName("MyBatis");
      mapper.insert(name);

      Name loadedName = mapper.findById(name.getId());
      Assertions.assertEquals(name.getFirstName(), loadedName.getFirstName());
      Assertions.assertEquals(name.getLastName(), loadedName.getLastName());
    }
  }

  @Test
  void testUpdate() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      TemplateFilePathProviderMapper mapper = sqlSession.getMapper(TemplateFilePathProviderMapper.class);
      Name name = new Name();
      name.setFirstName("Thymeleaf");
      name.setLastName("MyBatis");
      mapper.insert(name);

      Name updatingName = new Name();
      updatingName.setId(name.getId());
      updatingName.setFirstName("Thymeleaf3");
      mapper.update(updatingName);

      Name loadedName = mapper.findById(name.getId());
      Assertions.assertEquals(updatingName.getFirstName(), loadedName.getFirstName());
      Assertions.assertEquals(name.getLastName(), loadedName.getLastName());
    }
  }

  @Test
  void testDelete() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      TemplateFilePathProviderMapper mapper = sqlSession.getMapper(TemplateFilePathProviderMapper.class);
      Name name = new Name();
      name.setFirstName("Thymeleaf");
      name.setLastName("MyBatis");
      mapper.insert(name);

      mapper.delete(name);

      Name loadedName = mapper.findById(name.getId());
      Assertions.assertNull(loadedName);
    }
  }

}
