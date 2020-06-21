/**
 *    Copyright 2018-2020 the original author or authors.
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

import java.io.Reader;
import java.sql.Connection;
import java.util.List;
import java.util.Properties;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Name;
import org.mybatis.scripting.thymeleaf.integrationtest.mapper.InvalidNameParam;

class ThymeleafSqlSourceTest {

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {

    try (Reader reader = Resources.getResourceAsReader("mapper-config.xml")) {
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    }
    sqlSessionFactory.getConfiguration().setVariables(new Properties());

    try (Connection conn = sqlSessionFactory.getConfiguration().getEnvironment().getDataSource().getConnection()) {
      try (Reader reader = Resources.getResourceAsReader("create-db.sql")) {
        ScriptRunner runner = new ScriptRunner(conn);
        runner.setLogWriter(null);
        runner.setErrorLogWriter(null);
        runner.runScript(reader);
        conn.commit();
      }
    }
  }

  @Test
  void testErrorOnGetterMethod() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      try {
        sqlSession.selectList(
            "org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findUsing_parameter",
            new InvalidNameParam(4));
        Assertions.fail();
      } catch (PersistenceException e) {
        Assertions.assertEquals(
            "Cannot get a value for property named 'id' in 'class org.mybatis.scripting.thymeleaf.integrationtest.mapper.InvalidNameParam'",
            e.getCause().getCause().getMessage());
      }
    }
  }

  @Test
  void testConfigurationProperties() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      sqlSessionFactory.getConfiguration().getVariables().setProperty("tableNameOfNames", "names2");
      List<Name> list = sqlSession.selectList(
          "org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findAllFormSpecifiedTable");
      Assertions.assertEquals(1, list.size());
    } finally {
      sqlSessionFactory.getConfiguration().getVariables().remove("tableNameOfNames");
    }
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      List<Name> list = sqlSession.selectList(
          "org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findAllFormSpecifiedTable");
      Assertions.assertEquals(7, list.size());
    }
  }

}
