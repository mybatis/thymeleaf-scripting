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

import java.beans.PropertyDescriptor;
import java.io.Reader;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.scripting.thymeleaf.processor.BindVariableRender;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.thymeleaf.TemplateEngine;

class SqlGeneratorTest {

  private static JDBCDataSource dataSource;
  private static SqlGeneratorConfig config;

  @BeforeAll
  static void setUp() throws Exception {
    Class.forName("org.hsqldb.jdbcDriver");
    dataSource = new JDBCDataSource();
    dataSource.setUrl("jdbc:hsqldb:mem:sql-template");
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
    config = SqlGeneratorConfig.newInstanceWithCustomizer(
        c -> c.getDialect().setBindVariableRender(BindVariableRender.BuiltIn.SPRING_NAMED_PARAMETER.getType()));
  }

  @Test
  void processWithDefaultConfig() {
    SqlGenerator sqlGenerator = new SqlGenerator();
    // @formatter: off
    String sqlTemplate = "SELECT * FROM names " + "/*[# th:if='${id != null}']*/ "
        + "WHERE id = /*[# mb:p='id']*/ 1 /*[/]*/ " + "/*[/]*/";
    // @formatter: on
    {
      Param param = new Param();
      param.id = 5;

      String sql = sqlGenerator.generate(sqlTemplate, param);

      Assertions.assertEquals("SELECT * FROM names  WHERE id = #{id} ", sql);
    }
    {
      Param param = new Param();

      String sql = sqlGenerator.generate(sqlTemplate, param);

      Assertions.assertEquals("SELECT * FROM names ", sql);
    }
  }

  @Test
  void processWithConfig() {
    SqlGeneratorConfig config = SqlGeneratorConfig.newInstanceWithCustomizer(
        c -> c.getDialect().setBindVariableRender(BindVariableRender.BuiltIn.SPRING_NAMED_PARAMETER.getType()));
    SqlGenerator sqlGenerator = new SqlGenerator(config);
    NamedParameterJdbcOperations jdbcOperations = new NamedParameterJdbcTemplate(dataSource);

    // @formatter: off
    String sqlTemplate = "SELECT * FROM names " + "/*[# th:if='${id != null}']*/ "
        + "WHERE id = /*[# mb:p='id']*/ 1 /*[/]*/ " + "/*[/]*/";
    // @formatter: on
    {
      Param param = new Param();
      param.id = 5;

      String sql = sqlGenerator.generate(sqlTemplate, param);

      Map<String, Object> record = jdbcOperations.queryForMap(sql, new BeanPropertySqlParameterSource(param));

      Assertions.assertEquals(3, record.size());
      Assertions.assertEquals(5, record.get("ID"));
      Assertions.assertEquals("Betty", record.get("FIRSTNAME"));
      Assertions.assertEquals("Rubble", record.get("LASTNAME"));
    }
    {
      Param param = new Param();

      String sql = sqlGenerator.generate(sqlTemplate, param);

      IncorrectResultSizeDataAccessException e = Assertions.assertThrows(IncorrectResultSizeDataAccessException.class,
          () -> jdbcOperations.queryForMap(sql, new BeanPropertySqlParameterSource(param)));

      Assertions.assertEquals(1, e.getExpectedSize());
      Assertions.assertEquals(7, e.getActualSize());
    }
  }

  @Test
  void processWithTemplateEngine() {
    SqlGenerator sqlGenerator = new SqlGenerator(new TemplateEngine());
    // @formatter: off
    String sqlTemplate = "SELECT * FROM names " + "/*[# th:if='${id != null}']*/ "
        + "WHERE id = /*[# mb:p='id']*/ 1 /*[/]*/ " + "/*[/]*/";
    // @formatter: on
    {
      Param param = new Param();
      param.id = 5;

      String sql = sqlGenerator.generate(sqlTemplate, param);

      Assertions.assertEquals(sqlTemplate, sql);
    }
  }

  @Test
  void processWithSimpleValue() {
    SqlGenerator sqlGenerator = new SqlGenerator(config);
    NamedParameterJdbcOperations jdbcOperations = new NamedParameterJdbcTemplate(dataSource);

    // @formatter: off
    String sqlTemplate = "SELECT * FROM names " + "/*[# th:if='${id != null}']*/ "
        + "WHERE id = /*[# mb:p='id']*/ 1 /*[/]*/ " + "/*[/]*/";
    // @formatter: on
    {
      Integer id = 6;

      String sql = sqlGenerator.generate(sqlTemplate, id);

      Map<String, Object> record = jdbcOperations.queryForMap(sql, new MapSqlParameterSource("id", id));

      Assertions.assertEquals(6, record.get("ID"));
      Assertions.assertEquals("Be%ty", record.get("FIRSTNAME"));
      Assertions.assertEquals("Ab_le", record.get("LASTNAME"));
    }
    {
      Integer id = null;

      String sql = sqlGenerator.generate(sqlTemplate, id);

      IncorrectResultSizeDataAccessException e = Assertions.assertThrows(IncorrectResultSizeDataAccessException.class,
          () -> jdbcOperations.queryForMap(sql, new MapSqlParameterSource("id", id)));

      Assertions.assertEquals(1, e.getExpectedSize());
      Assertions.assertEquals(7, e.getActualSize());
    }
  }

  @Test
  void processWithMap() {
    SqlGenerator sqlGenerator = new SqlGenerator(config);
    NamedParameterJdbcOperations jdbcOperations = new NamedParameterJdbcTemplate(dataSource);

    // @formatter: off
    String sqlTemplate = "SELECT * FROM names " + "/*[# th:if='${id != null}']*/ "
        + "WHERE id = /*[# mb:p='id']*/ 1 /*[/]*/ " + "/*[/]*/";
    // @formatter: on
    {
      Map<String, Object> param = Collections.singletonMap("id", 2);

      String sql = sqlGenerator.generate(sqlTemplate, param);

      Map<String, Object> record = jdbcOperations.queryForMap(sql, new MapSqlParameterSource(param));

      Assertions.assertEquals(2, record.get("ID"));
      Assertions.assertEquals("Wilma", record.get("FIRSTNAME"));
      Assertions.assertEquals("Flintstone", record.get("LASTNAME"));
    }
    {
      Map<String, Object> param = Collections.emptyMap();

      String sql = sqlGenerator.generate(sqlTemplate, param);
      System.out.println(sql);
      IncorrectResultSizeDataAccessException e = Assertions.assertThrows(IncorrectResultSizeDataAccessException.class,
          () -> jdbcOperations.queryForMap(sql, new MapSqlParameterSource(param)));

      Assertions.assertEquals(1, e.getExpectedSize());
      Assertions.assertEquals(7, e.getActualSize());
    }
  }

  @Test
  void processWithCustomVariables() {
    SqlGenerator sqlGenerator = new SqlGenerator(config);
    sqlGenerator.setDefaultCustomVariables(Collections.singletonMap("tableName", "names"));

    NamedParameterJdbcOperations jdbcOperations = new NamedParameterJdbcTemplate(dataSource);

    // @formatter: off
    String sqlTemplate = "SELECT * FROM /*[(${tableName} ?: 'users')]*/ " + "/*[# th:if='${id != null}']*/ "
        + "WHERE id = /*[# mb:p='id']*/ 1 /*[/]*/ " + "/*[/]*/";
    // @formatter: on
    {
      Map<String, Object> customVariables = Collections.singletonMap("id", 2);

      String sql = sqlGenerator.generate(sqlTemplate, null, customVariables);

      Map<String, Object> record = jdbcOperations.queryForMap(sql, new MapSqlParameterSource(customVariables));

      Assertions.assertEquals(2, record.get("ID"));
      Assertions.assertEquals("Wilma", record.get("FIRSTNAME"));
      Assertions.assertEquals("Flintstone", record.get("LASTNAME"));
    }
  }

  @Test
  void processWithCustomBindVariablesStore() {
    SqlGenerator sqlGenerator = new SqlGenerator(config);
    NamedParameterJdbcOperations jdbcOperations = new NamedParameterJdbcTemplate(dataSource);

    // @formatter: off
    String sqlTemplate = "SELECT * FROM names " + "/*[# th:if='${name != null}']*/ "
        + "/*[# mb:bind='patternName=|${#likes.escapeWildcard(name)}%|' /]*/ "
        + "WHERE firstName LIKE /*[# mb:p='patternName']*/ 'foo%' /*[/]*/ /*[(${#likes.escapeClause()})]*/ "
        + "/*[/]*/";
    // @formatter: on
    {
      Map<String, Object> param = Collections.singletonMap("name", "Be%");
      MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource(param);

      String sql = sqlGenerator.generate(sqlTemplate, param, null, mapSqlParameterSource::addValue);

      Map<String, Object> record = jdbcOperations.queryForMap(sql, mapSqlParameterSource);

      Assertions.assertEquals(6, record.get("ID"));
      Assertions.assertEquals("Be%ty", record.get("FIRSTNAME"));
      Assertions.assertEquals("Ab_le", record.get("LASTNAME"));

      Assertions.assertEquals("Be\\%%", mapSqlParameterSource.getValue("patternName"));
    }
  }

  @Test
  void processWithCustomPropertyAccessor() {
    SqlGenerator sqlGenerator = new SqlGenerator(config);
    sqlGenerator.setPropertyAccessor(new PropertyAccessor() {
      Map<Class<?>, BeanWrapper> beanWrappers = new HashMap<>();

      @Override
      public Set<String> getPropertyNames(Class<?> type) {
        return Stream.of(beanWrappers.computeIfAbsent(type, BeanWrapperImpl::new).getPropertyDescriptors())
            .map(PropertyDescriptor::getName).collect(Collectors.toSet());
      }

      @Override
      public Class<?> getPropertyType(Class<?> type, String name) {
        return beanWrappers.computeIfAbsent(type, BeanWrapperImpl::new).getPropertyType(name);
      }

      @Override
      public Object getPropertyValue(Object target, String name) {
        return new BeanWrapperImpl(target).getPropertyValue(name);
      }

      @Override
      public void setPropertyValue(Object target, String name, Object value) {
        new BeanWrapperImpl(target).setPropertyValue(name, value);
      }
    });
    NamedParameterJdbcOperations jdbcOperations = new NamedParameterJdbcTemplate(dataSource);

    // @formatter: off
    String sqlTemplate = "SELECT * FROM names " + "/*[# th:if='${id != null}']*/ "
        + "WHERE id = /*[# mb:p='id']*/ 1 /*[/]*/ " + "/*[/]*/";
    // @formatter: on
    {
      Param param = new Param();
      param.id = 5;

      String sql = sqlGenerator.generate(sqlTemplate, param);

      Map<String, Object> record = jdbcOperations.queryForMap(sql, new BeanPropertySqlParameterSource(param));

      Assertions.assertEquals(3, record.size());
      Assertions.assertEquals(5, record.get("ID"));
      Assertions.assertEquals("Betty", record.get("FIRSTNAME"));
      Assertions.assertEquals("Rubble", record.get("LASTNAME"));
    }
  }

  static class Param {
    private Integer id;

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }
  }

}
