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
package org.mybatis.scripting.thymeleaf.integrationtest;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriver;
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Mail;
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Name;
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Person;
import org.mybatis.scripting.thymeleaf.integrationtest.mapper.NameMapper;
import org.mybatis.scripting.thymeleaf.integrationtest.mapper.NameParam;
import org.mybatis.scripting.thymeleaf.integrationtest.mapper.PersonMapper;

import java.io.Reader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class AnnotationDrivenMapperTest {
  private static SqlSessionFactory sqlSessionFactory;

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
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);

    configuration.addMapper(NameMapper.class);
    configuration.addMapper(PersonMapper.class);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
  }

  @Test
  void testNoParam() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      List<Name> names = mapper.getAllNames();
      Assertions.assertEquals(7, names.size());
    }
  }

  @Test
  void testListParamWithParamAnnotation() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      List<Name> names = mapper.findByIds(Arrays.asList(1, 3, 4));
      Assertions.assertEquals(3, names.size());
      Assertions.assertEquals(1, names.get(0).getId());
      Assertions.assertEquals(3, names.get(1).getId());
      Assertions.assertEquals(4, names.get(2).getId());
    }
  }

  @Test
  void testListParamWithoutParamAnnotation() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      List<Name> names = mapper.findByIdsWithoutParamAnnotation(Arrays.asList(1, 3, 4));
      Assertions.assertEquals(3, names.size());
      Assertions.assertEquals(1, names.get(0).getId());
      Assertions.assertEquals(3, names.get(1).getId());
      Assertions.assertEquals(4, names.get(2).getId());
    }
  }

  @Test
  void testParamObject() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      List<Name> names = mapper.findUsingScript(new NameParam(4));
      Assertions.assertEquals(1, names.size());
      Name name = names.get(0);
      Assertions.assertEquals(4, name.getId());
    }
  }

  @Test
  void testParamObjectWithParamAnnotation() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      List<Name> names = mapper.findByIdWithNestedParam(new NameParam(2));
      Assertions.assertEquals(1, names.size());
      Name name = names.get(0);
      Assertions.assertEquals(2, name.getId());
    }
  }

  @Test
  void testParamValueWithParamAnnotation() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      List<Name> names = mapper.findById(4);
      Assertions.assertEquals(1, names.size());
      Name name = names.get(0);
      Assertions.assertEquals(4, name.getId());
    }
  }

  @Test
  void testParamValueWithoutParamAnnotation() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      List<Name> names = mapper.findByIdWithoutParamAnnotation(4);
      Assertions.assertEquals(1, names.size());
      Name name = names.get(0);
      Assertions.assertEquals(4, name.getId());
    }
  }

  @Test
  void testParamObjectOnTemplateFile() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      List<Name> names = mapper.findUsingTemplateFile(new NameParam(3));
      Assertions.assertEquals(1, names.size());
      Name name = names.get(0);
      Assertions.assertEquals(3, name.getId());
    }
  }

  @Test
  void testInsert() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      Name name = new Name();
      name.setFirstName("Thymeleaf");
      name.setLastName("MyBatis");
      mapper.insert(name);

      List<Name> names = mapper.findById(name.getId());
      Assertions.assertEquals(1, names.size());
      Name loadedName = names.get(0);
      Assertions.assertEquals(name.getFirstName(), loadedName.getFirstName());
      Assertions.assertEquals(name.getLastName(), loadedName.getLastName());
    }
  }

  @Test
  void testInsertByBulk() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      List<Name> names = new ArrayList<>();
      {
        Name name = new Name();
        name.setFirstName("Thymeleaf001");
        name.setLastName("MyBatis001");
        names.add(name);
      }
      {
        Name name = new Name();
        name.setFirstName("Thymeleaf002");
        name.setLastName("MyBatis002");
        names.add(name);
      }
      {
        Name name = new Name();
        name.setFirstName("Thymeleaf003");
        name.setLastName("MyBatis003");
        names.add(name);
      }

      mapper.insertByBulk(names);

      Assertions.assertEquals(3, mapper.findByIds(names.stream().map(Name::getId).collect(Collectors.toList())).size());

    }
  }

  @Test
  void testUpdate() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      Name name = new Name();
      name.setFirstName("Thymeleaf");
      name.setLastName("MyBatis");
      mapper.insert(name);

      Name updatingName = new Name();
      updatingName.setId(name.getId());
      updatingName.setFirstName("Thymeleaf3");
      mapper.update(updatingName);

      List<Name> names = mapper.findById(name.getId());
      Assertions.assertEquals(1, names.size());
      Name loadedName = names.get(0);
      Assertions.assertEquals(updatingName.getFirstName(), loadedName.getFirstName());
      Assertions.assertEquals(name.getLastName(), loadedName.getLastName());
    }
  }

  @Test
  void testDelete() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      Name name = new Name();
      name.setFirstName("Thymeleaf");
      name.setLastName("MyBatis");
      mapper.insert(name);

      mapper.delete(name);

      List<Name> names = mapper.findById(name.getId());
      Assertions.assertEquals(0, names.size());
    }
  }

  @Test
  void testCustomBindVariables() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      NameParam param = new NameParam();
      param.setFirstName("B");
      param.setLastName("Rub");
      List<Name> names = mapper.findByName(param);
      Assertions.assertEquals(2, names.size());
    }
  }

  @Test
  void testEscapeLikeWildcard() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      NameParam param = new NameParam();
      param.setFirstName("Be%");
      List<Name> names = mapper.findByName(param);
      Assertions.assertEquals(1, names.size());
      Assertions.assertEquals(names.get(0).getId(), 6);
      Assertions.assertEquals(names.get(0).getFirstName(), "Be%ty");
      Assertions.assertEquals(names.get(0).getLastName(), "Ab_le");
    }
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      NameParam param = new NameParam();
      param.setLastName("Ab_");
      List<Name> names = mapper.findByName(param);
      Assertions.assertEquals(1, names.size());
      Assertions.assertEquals(names.get(0).getId(), 6);
      Assertions.assertEquals(names.get(0).getFirstName(), "Be%ty");
      Assertions.assertEquals(names.get(0).getLastName(), "Ab_le");
    }
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      NameParam param = new NameParam();
      param.setFirstName("Be\\");
      List<Name> names = mapper.findByName(param);
      Assertions.assertEquals(1, names.size());
      Assertions.assertEquals(names.get(0).getId(), 7);
      Assertions.assertEquals(names.get(0).getFirstName(), "Be\\ty");
      Assertions.assertEquals(names.get(0).getLastName(), "Abble");
    }
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      NameParam param = new NameParam();
      param.setFirstName("");
      List<Name> names = mapper.findByName(param);
      Assertions.assertEquals(7, names.size());
    }
  }

  @Test
  void testListWithinParam() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      NameParam param = new NameParam();
      param.setIds(Arrays.asList(1, 3, 4));
      List<Name> names = mapper.findByIdsWithinParam(param);
      Assertions.assertEquals(3, names.size());
      Assertions.assertEquals(1, names.get(0).getId());
      Assertions.assertEquals(3, names.get(1).getId());
      Assertions.assertEquals(4, names.get(2).getId());
    }
  }

  @Test
  void a() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      PersonMapper mapper = sqlSession.getMapper(PersonMapper.class);
      List<Person> persons = new ArrayList<>();
      {
        Person person = new Person();
        person.setName("MyBatis 1");
        List<Mail> mails = new ArrayList<>();
        person.setMails(mails);
        {
          Mail mail = new Mail();
          mail.setAddress("mybatis1.main@test.com");
          mails.add(mail);
        }
        {
          Mail mail = new Mail();
          mail.setAddress("mybatis1.sub@test.com");
          mails.add(mail);
        }
        persons.add(person);
      }
      {
        Person person = new Person();
        person.setName("MyBatis 2");
        List<Mail> mails = new ArrayList<>();
        person.setMails(mails);
        {
          Mail mail = new Mail();
          mail.setAddress("mybatis2.main@test.com");
          mails.add(mail);
        }
        {
          Mail mail = new Mail();
          mail.setAddress("mybatis2.sub@test.com");
          mails.add(mail);
        }
        persons.add(person);
      }
      mapper.insertByBulk(persons);
      mapper.insertMailsByBulk(persons);

      persons.stream().flatMap(p -> p.getMails().stream()).forEach(m -> System.out.println(m.getId()));

      mapper.selectMails().forEach(m -> System.out.println(m.getId()));
    }
  }

}
