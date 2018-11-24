/**
 *    Copyright 2018 the original author or authors.
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

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Name;
import org.mybatis.scripting.thymeleaf.integrationtest.mapper.NameParam;

import java.io.Reader;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

class SqlSessionTest {
  private static SqlSessionFactory sqlSessionFactory;
  private static SqlSessionFactory sqlSessionFactoryForH2;

  @BeforeAll
  static void setUp() throws Exception {

    try (Reader reader = Resources.getResourceAsReader("mapper-config.xml")) {
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    }

    try (Connection conn = sqlSessionFactory.getConfiguration().getEnvironment().getDataSource().getConnection()) {
      try (Reader reader = Resources.getResourceAsReader("create-db.sql")) {
        ScriptRunner runner = new ScriptRunner(conn);
        runner.setLogWriter(null);
        runner.setErrorLogWriter(null);
        runner.runScript(reader);
        conn.commit();
      }
    }

    try (Reader reader = Resources.getResourceAsReader("mapper-config.xml")) {
      sqlSessionFactoryForH2 = new SqlSessionFactoryBuilder().build(reader, "h2");
    }

    try (Connection conn = sqlSessionFactoryForH2.getConfiguration().getEnvironment().getDataSource().getConnection()) {
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
  void testNoParam() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      List<Name> names = sqlSession.selectList("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.getAllNames");
      Assertions.assertEquals(5, names.size());
    }
  }

  @Test
  void testListParamUsing_include() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      List<Name> names = sqlSession.selectList("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findByIdsUsing_include", Arrays.asList(1, 3, 4));
      Assertions.assertEquals(3, names.size());
      Assertions.assertEquals(1, names.get(0).getId());
      Assertions.assertEquals(3, names.get(1).getId());
      Assertions.assertEquals(4, names.get(2).getId());
    }
  }

  @Test
  void testParamObjectUsing_parameter() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      List<Name> names = sqlSession.selectList("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findUsing_parameter", new NameParam(4));
      Assertions.assertEquals(1, names.size());
      Name name = names.get(0);
      Assertions.assertEquals(4, name.getId());
    }
  }

  @Test
  void testParamValueUsing_value() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      List<Name> names = sqlSession.selectList("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findById_value", 4);
      Assertions.assertEquals(1, names.size());
      Name name = names.get(0);
      Assertions.assertEquals(4, name.getId());
    }
  }

  @Test
  void testParamObjectUsingTemplateFile() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      List<Name> names = sqlSession.selectList("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findUsingTemplateFile", new NameParam(3));
      Assertions.assertEquals(1, names.size());
      Name name = names.get(0);
      Assertions.assertEquals(3, name.getId());
    }
  }

  @Test
  void testDatabaseId() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      String databaseId = sqlSession.selectOne("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findDatabaseId");
      Assertions.assertEquals("hsql", databaseId);
    }
  }

  @Test
  void testDatabaseIdWithH2() {
    try (SqlSession sqlSession = sqlSessionFactoryForH2.openSession()) {
      String databaseId = sqlSession.selectOne("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findDatabaseId");
      Assertions.assertEquals("h2", databaseId);
    }
  }

  @Test
  void testInsert() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Name name = new Name();
      name.setFirstName("Thymeleaf");
      name.setLastName("MyBatis");
      sqlSession.insert("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.insert", name);

      Name loadedName = sqlSession.selectOne("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findById_value", name.getId());
      Assertions.assertEquals(name.getFirstName(), loadedName.getFirstName());
      Assertions.assertEquals(name.getLastName(), loadedName.getLastName());
    }
  }

  @Test
  void testUpdate() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Name name = new Name();
      name.setFirstName("Thymeleaf");
      name.setLastName("MyBatis");
      sqlSession.insert("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.insert", name);

      Name updatingName = new Name();
      updatingName.setId(name.getId());
      updatingName.setFirstName("Thymeleaf3");
      sqlSession.update("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.update", updatingName);

      Name loadedName = sqlSession.selectOne("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findById_value", name.getId());
      Assertions.assertEquals(updatingName.getFirstName(), loadedName.getFirstName());
      Assertions.assertEquals(name.getLastName(), loadedName.getLastName());
    }
  }

  @Test
  void testUpdateWithEmptyComment() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Name name = new Name();
      name.setFirstName("Thymeleaf");
      name.setLastName("MyBatis");
      sqlSession.insert("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.insert", name);

      Name updatingName = new Name();
      updatingName.setId(name.getId());
      updatingName.setFirstName("Thymeleaf3");
      updatingName.setLastName("MyBatis3");
      sqlSession.update("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.updateWithEmptyComment", updatingName);

      Name loadedName = sqlSession.selectOne("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findById_value", name.getId());
      Assertions.assertEquals(updatingName.getFirstName(), loadedName.getFirstName());
      Assertions.assertEquals(updatingName.getLastName(), loadedName.getLastName());
    }
  }

  @Test
  void testDelete() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Name name = new Name();
      name.setFirstName("Thymeleaf");
      name.setLastName("MyBatis");
      sqlSession.insert("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.insert", name);

      sqlSession.delete("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.delete", name);

      Name loadedName = sqlSession.selectOne("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findById_value", name.getId());
      Assertions.assertNull(loadedName);
    }
  }

  @Test
  void testFindByNameWithEmptyComment() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Name name = new Name();
      name.setFirstName("Thymeleaf");
      name.setLastName("MyBatis");
      sqlSession.insert("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.insert", name);

      NameParam param = new NameParam();
      param.setFirstName(name.getFirstName());
      param.setLastName(name.getLastName());
      Name loadedName = sqlSession.selectOne("org.mybatis.scripting.thymeleaf.integrationtest.mapper.XmlNameSqlSessionMapper.findByNameWithEmptyComment", param);
      Assertions.assertEquals(param.getFirstName(), loadedName.getFirstName());
      Assertions.assertEquals(param.getLastName(), loadedName.getLastName());
    }
  }



}
