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

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.scripting.ScriptingException;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.scripting.thymeleaf.expression.MyBatisExpression;
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Name;
import org.mybatis.scripting.thymeleaf.integrationtest.mapper.NameMapper;
import org.mybatis.scripting.thymeleaf.integrationtest.mapper.NameParam;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

class ThymeleafLanguageDriverTest {

  private static SqlSessionFactory sqlSessionFactory;
  private String currentConfigFile;
  private String currentConfigEncoding;

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
    configuration.getLanguageRegistry().register(
        ThymeleafLanguageDriver.newBuilder().templateEngine(new TemplateEngine()).build());
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);

    configuration.addMapper(NameMapper.class);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
  }

  @BeforeEach
  void saveCurrentConfig() {
    currentConfigFile = System.getProperty("mybatis-thymeleaf.config");
    currentConfigEncoding = System.getProperty("mybatis-thymeleaf.config.encoding");
  }

  @AfterEach
  void restoreConfig() {
    if (currentConfigFile == null) {
      System.clearProperty("mybatis-thymeleaf.config.file");
    } else {
      System.setProperty("mybatis-thymeleaf.config.file", currentConfigFile);
    }
    if (currentConfigEncoding == null) {
      System.clearProperty("mybatis-thymeleaf.config.encoding");
    } else {
      System.setProperty("mybatis-thymeleaf.config.encoding", currentConfigEncoding);
    }
  }

  @Test
  void testDefaultConfig() {
    DefaultTemplateEngineCustomizer.templateEngine = null;
    Configuration configuration = new Configuration();
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);
    new SqlSessionFactoryBuilder().build(configuration);

    TemplateEngine templateEngine = DefaultTemplateEngineCustomizer.templateEngine;
    ClassLoaderTemplateResolver classLoaderTemplateResolver =
        TemplateEngineCustomizer.extractTemplateResolver(templateEngine, ClassLoaderTemplateResolver.class)
            .orElseGet(() -> Assertions.fail("Cannot a ClassLoaderTemplateResolver instance."));

    Assertions.assertEquals(TemplateMode.CSS, classLoaderTemplateResolver.getTemplateMode());
    Assertions.assertTrue(classLoaderTemplateResolver.isCacheable());
    Assertions.assertNull(classLoaderTemplateResolver.getCacheTTLMs());
    Assertions.assertEquals("UTF-8", classLoaderTemplateResolver.getCharacterEncoding());
    Assertions.assertEquals("", classLoaderTemplateResolver.getPrefix());
    Assertions.assertEquals(new LinkedHashSet<>(Collections.singleton("*.sql")), classLoaderTemplateResolver.getResolvablePatterns());

    StringTemplateResolver stringTemplateResolver =
        TemplateEngineCustomizer.extractTemplateResolver(templateEngine, StringTemplateResolver.class)
            .orElseGet(() -> Assertions.fail("Cannot a StringTemplateResolver instance."));
    Assertions.assertEquals(TemplateMode.CSS, stringTemplateResolver.getTemplateMode());
    Assertions.assertFalse(stringTemplateResolver.isCacheable());
    Assertions.assertNull(stringTemplateResolver.getCacheTTLMs());

    templateEngine.getDialects().stream().filter(MyBatisDialect.class::isInstance).findFirst()
        .map(MyBatisDialect.class::cast).ifPresent(v -> {
      MyBatisExpression expression = (MyBatisExpression) v.getExpressionObjectFactory()
          .buildObject(null, null);
      Assertions.assertEquals("ESCAPE '\\'", expression.likeEscapeClause());
    });
  }

  @Test
  void testCustomWithCustomConfigFile() {
    System.setProperty("mybatis-thymeleaf.config.file", "mybatis-thymeleaf-custom.properties");
    Configuration configuration = new Configuration();
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);

    new SqlSessionFactoryBuilder().build(configuration);

    TemplateEngine templateEngine = CustomTemplateEngineCustomizer.templateEngine;
    ClassLoaderTemplateResolver classLoaderTemplateResolver =
        TemplateEngineCustomizer.extractTemplateResolver(templateEngine, ClassLoaderTemplateResolver.class)
            .orElseGet(() -> Assertions.fail("Cannot a ClassLoaderTemplateResolver instance."));

    Assertions.assertEquals(TemplateMode.TEXT, classLoaderTemplateResolver.getTemplateMode());
    Assertions.assertFalse(classLoaderTemplateResolver.isCacheable());
    Assertions.assertEquals(Long.valueOf(30000), classLoaderTemplateResolver.getCacheTTLMs());
    Assertions.assertEquals("ISO-8859-1", classLoaderTemplateResolver.getCharacterEncoding());
    Assertions.assertEquals("/templates/sqls/", classLoaderTemplateResolver.getPrefix());
    Assertions.assertEquals(new LinkedHashSet<>(Arrays.asList("*.sql", "*.sql.template")), classLoaderTemplateResolver.getResolvablePatterns());

    StringTemplateResolver stringTemplateResolver =
        TemplateEngineCustomizer.extractTemplateResolver(templateEngine, StringTemplateResolver.class)
            .orElseGet(() -> Assertions.fail("Cannot a StringTemplateResolver instance."));
    Assertions.assertEquals(TemplateMode.TEXT, stringTemplateResolver.getTemplateMode());
    Assertions.assertFalse(stringTemplateResolver.isCacheable());

    templateEngine.getDialects().stream().filter(MyBatisDialect.class::isInstance).findFirst()
        .map(MyBatisDialect.class::cast).ifPresent(v -> {
      MyBatisExpression expression = (MyBatisExpression) v.getExpressionObjectFactory()
          .buildObject(null, null);
      Assertions.assertEquals("escape '~'", expression.likeEscapeClause());
      Assertions.assertEquals("a~％~＿~~b", expression.escapeLikeWildcard("a％＿~b"));
    });
  }

  @Test
  void testCustomWithBuilder() {
    System.setProperty("mybatis-thymeleaf.config.file", "mybatis-thymeleaf-empty.properties");
    Configuration configuration = new Configuration();
    configuration.getLanguageRegistry().register(ThymeleafLanguageDriver.newBuilder()
        .use2way(false)
        .cacheEnabled(false)
        .cacheTtl(30000)
        .fileCharacterEncoding(StandardCharsets.ISO_8859_1)
        .fileBaseDir("/templates/sqls/")
        .filePatterns("*.sql", "*.sql.template")
        .dialectLikeEscapeChar('~')
        .dialectLikeEscapeClauseFormat("escape '%s'")
        .dialectLikeAdditionalEscapeTargetChars('％' , '＿')
        .customizer(CustomTemplateEngineCustomizer.class)
        .build());
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);

    new SqlSessionFactoryBuilder().build(configuration);

    TemplateEngine templateEngine = CustomTemplateEngineCustomizer.templateEngine;
    ClassLoaderTemplateResolver classLoaderTemplateResolver =
        TemplateEngineCustomizer.extractTemplateResolver(templateEngine, ClassLoaderTemplateResolver.class)
            .orElseGet(() -> Assertions.fail("Cannot a ClassLoaderTemplateResolver instance."));

    Assertions.assertEquals(TemplateMode.TEXT, classLoaderTemplateResolver.getTemplateMode());
    Assertions.assertFalse(classLoaderTemplateResolver.isCacheable());
    Assertions.assertEquals(Long.valueOf(30000), classLoaderTemplateResolver.getCacheTTLMs());
    Assertions.assertEquals("ISO-8859-1", classLoaderTemplateResolver.getCharacterEncoding());
    Assertions.assertEquals("/templates/sqls/", classLoaderTemplateResolver.getPrefix());
    Assertions.assertEquals(new LinkedHashSet<>(Arrays.asList("*.sql", "*.sql.template")), classLoaderTemplateResolver.getResolvablePatterns());

    StringTemplateResolver stringTemplateResolver =
        TemplateEngineCustomizer.extractTemplateResolver(templateEngine, StringTemplateResolver.class)
            .orElseGet(() -> Assertions.fail("Cannot a StringTemplateResolver instance."));
    Assertions.assertEquals(TemplateMode.TEXT, stringTemplateResolver.getTemplateMode());
    Assertions.assertFalse(stringTemplateResolver.isCacheable());

    templateEngine.getDialects().stream().filter(MyBatisDialect.class::isInstance).findFirst()
        .map(MyBatisDialect.class::cast).ifPresent(v -> {
      MyBatisExpression expression = (MyBatisExpression) v.getExpressionObjectFactory()
          .buildObject(null, null);
      Assertions.assertEquals("escape '~'", expression.likeEscapeClause());
      Assertions.assertEquals("a~％~＿~~b", expression.escapeLikeWildcard("a％＿~b"));
    });
  }

  @Test
  void testConfigFileNotFound() {
    System.setProperty("mybatis-thymeleaf.config.file", "mybatis-thymeleaf-dummy.properties");
    Configuration configuration = new Configuration();
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);
    Assertions.assertEquals(ThymeleafLanguageDriver.class, configuration.getLanguageRegistry().getDefaultDriverClass());
  }

  @Test
  void testCustomizerNotFound() {
    System.setProperty("mybatis-thymeleaf.config.file", "mybatis-thymeleaf-customizer-not-found.properties");
    Configuration configuration = new Configuration();
    try {
      configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);
      Assertions.fail();
    } catch (ScriptingException e) {
      Assertions.assertEquals("Failed to load language driver for org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriver", e.getMessage());
      Assertions.assertEquals("java.lang.ClassNotFoundException: Cannot find class: org.mybatis.scripting.thymeleaf.FooTemplateEngineCustomizer", e.getCause().getMessage());
    }
  }

  @Test
  void testCustomizerNotCreation() {
    System.setProperty("mybatis-thymeleaf.config.file", "mybatis-thymeleaf-customizer-no-default-constructor.properties");
    Configuration configuration = new Configuration();
    try {
      configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);
      Assertions.fail();
    } catch (ScriptingException e) {
      Assertions.assertEquals("Failed to load language driver for org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriver", e.getMessage());
      Assertions.assertEquals("Cannot create an instance for class: class org.mybatis.scripting.thymeleaf.NoDefaultConstructorTemplateEngineCustomizer", e.getCause().getMessage());
    }
  }

  @Test
  void testCanResolveStringTemplateByUserDefineTemplateEngine() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      List<Name> names = mapper.getAllNames();
      Assertions.assertEquals(7, names.size());
    }
  }

  @Test
  void testCanNotResolveTemplateFileByUserDefineTemplateEngine() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      NameMapper mapper = sqlSession.getMapper(NameMapper.class);
      mapper.findUsingTemplateFile(new NameParam(3));
      Assertions.fail();
    } catch (PersistenceException e) {
      Assertions.assertEquals("unexpected token: SQL in statement [sql/NameMapper/findById.sql]", e.getCause().getMessage());
    }
  }

}
