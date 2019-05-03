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

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.scripting.thymeleaf.expression.Likes;
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Name;
import org.mybatis.scripting.thymeleaf.integrationtest.mapper.NameMapper;
import org.mybatis.scripting.thymeleaf.integrationtest.mapper.NameParam;
import org.mybatis.scripting.thymeleaf.support.TemplateFilePathProvider;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

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
    configuration.getLanguageRegistry().register(new ThymeleafLanguageDriver(new TemplateEngine()));
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);

    configuration.addMapper(NameMapper.class);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
  }

  @BeforeEach
  @AfterEach
  void cleanup() {
    TemplateFilePathProvider.setLanguageDriverConfig(null);
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
    ClassLoaderTemplateResolver classLoaderTemplateResolver = TemplateEngineCustomizer
        .extractTemplateResolver(templateEngine, ClassLoaderTemplateResolver.class)
        .orElseGet(() -> Assertions.fail("Cannot a ClassLoaderTemplateResolver instance."));

    Assertions.assertEquals(TemplateMode.CSS, classLoaderTemplateResolver.getTemplateMode());
    Assertions.assertTrue(classLoaderTemplateResolver.isCacheable());
    Assertions.assertNull(classLoaderTemplateResolver.getCacheTTLMs());
    Assertions.assertEquals("UTF-8", classLoaderTemplateResolver.getCharacterEncoding());
    Assertions.assertEquals("", classLoaderTemplateResolver.getPrefix());
    Assertions.assertEquals(new LinkedHashSet<>(Collections.singleton("*.sql")),
        classLoaderTemplateResolver.getResolvablePatterns());

    StringTemplateResolver stringTemplateResolver = TemplateEngineCustomizer
        .extractTemplateResolver(templateEngine, StringTemplateResolver.class)
        .orElseGet(() -> Assertions.fail("Cannot a StringTemplateResolver instance."));
    Assertions.assertEquals(TemplateMode.CSS, stringTemplateResolver.getTemplateMode());
    Assertions.assertFalse(stringTemplateResolver.isCacheable());
    Assertions.assertNull(stringTemplateResolver.getCacheTTLMs());

    templateEngine.getDialects().stream().filter(MyBatisDialect.class::isInstance).findFirst()
        .map(MyBatisDialect.class::cast).ifPresent(v -> {
          Assertions.assertEquals("mb", v.getPrefix());
          Likes expression = (Likes) v.getExpressionObjectFactory().buildObject(null, null);
          Assertions.assertEquals("ESCAPE '\\'", expression.escapeClause());
        });
  }

  @Test
  void testCustomWithCustomConfigFileUsingSystemProperty() {
    System.setProperty("mybatis-thymeleaf.config.file", "mybatis-thymeleaf-custom.properties");
    Configuration configuration = new Configuration();
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);

    new SqlSessionFactoryBuilder().build(configuration);

    TemplateEngine templateEngine = CustomTemplateEngineCustomizer.templateEngine;
    ClassLoaderTemplateResolver classLoaderTemplateResolver = TemplateEngineCustomizer
        .extractTemplateResolver(templateEngine, ClassLoaderTemplateResolver.class)
        .orElseGet(() -> Assertions.fail("Cannot a ClassLoaderTemplateResolver instance."));

    Assertions.assertEquals(TemplateMode.TEXT, classLoaderTemplateResolver.getTemplateMode());
    Assertions.assertFalse(classLoaderTemplateResolver.isCacheable());
    Assertions.assertEquals(Long.valueOf(30000), classLoaderTemplateResolver.getCacheTTLMs());
    Assertions.assertEquals("ISO-8859-1", classLoaderTemplateResolver.getCharacterEncoding());
    Assertions.assertEquals("templates/", classLoaderTemplateResolver.getPrefix());
    Assertions.assertEquals(new LinkedHashSet<>(Arrays.asList("*.sql", "*.sql.template")),
        classLoaderTemplateResolver.getResolvablePatterns());

    StringTemplateResolver stringTemplateResolver = TemplateEngineCustomizer
        .extractTemplateResolver(templateEngine, StringTemplateResolver.class)
        .orElseGet(() -> Assertions.fail("Cannot a StringTemplateResolver instance."));
    Assertions.assertEquals(TemplateMode.TEXT, stringTemplateResolver.getTemplateMode());
    Assertions.assertFalse(stringTemplateResolver.isCacheable());

    templateEngine.getDialects().stream().filter(MyBatisDialect.class::isInstance).findFirst()
        .map(MyBatisDialect.class::cast).ifPresent(v -> {
          Assertions.assertEquals("mybatis", v.getPrefix());
          Likes expression = (Likes) v.getExpressionObjectFactory().buildObject(null, null);
          Assertions.assertEquals("escape '~'", expression.escapeClause());
          Assertions.assertEquals("a~％~＿~~b", expression.escapeWildcard("a％＿~b"));
        });
  }

  @Test
  void testCustomWithCustomConfigFileUsingMethodArgument() {
    ThymeleafLanguageDriverConfig thymeleafLanguageDriverConfig = ThymeleafLanguageDriverConfig
        .newInstance("mybatis-thymeleaf-custom.properties");
    Configuration configuration = new Configuration();
    configuration.getLanguageRegistry().register(new ThymeleafLanguageDriver(thymeleafLanguageDriverConfig));
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);

    new SqlSessionFactoryBuilder().build(configuration);

    TemplateEngine templateEngine = CustomTemplateEngineCustomizer.templateEngine;
    ClassLoaderTemplateResolver classLoaderTemplateResolver = TemplateEngineCustomizer
        .extractTemplateResolver(templateEngine, ClassLoaderTemplateResolver.class)
        .orElseGet(() -> Assertions.fail("Cannot a ClassLoaderTemplateResolver instance."));

    Assertions.assertEquals(TemplateMode.TEXT, classLoaderTemplateResolver.getTemplateMode());
    Assertions.assertFalse(classLoaderTemplateResolver.isCacheable());
    Assertions.assertEquals(Long.valueOf(30000), classLoaderTemplateResolver.getCacheTTLMs());
    Assertions.assertEquals("ISO-8859-1", classLoaderTemplateResolver.getCharacterEncoding());
    Assertions.assertEquals("templates/", classLoaderTemplateResolver.getPrefix());
    Assertions.assertEquals(new LinkedHashSet<>(Arrays.asList("*.sql", "*.sql.template")),
        classLoaderTemplateResolver.getResolvablePatterns());

    StringTemplateResolver stringTemplateResolver = TemplateEngineCustomizer
        .extractTemplateResolver(templateEngine, StringTemplateResolver.class)
        .orElseGet(() -> Assertions.fail("Cannot a StringTemplateResolver instance."));
    Assertions.assertEquals(TemplateMode.TEXT, stringTemplateResolver.getTemplateMode());
    Assertions.assertFalse(stringTemplateResolver.isCacheable());

    templateEngine.getDialects().stream().filter(MyBatisDialect.class::isInstance).findFirst()
        .map(MyBatisDialect.class::cast).ifPresent(v -> {
          Assertions.assertEquals("mybatis", v.getPrefix());
          Likes expression = (Likes) v.getExpressionObjectFactory().buildObject(null, null);
          Assertions.assertEquals("escape '~'", expression.escapeClause());
          Assertions.assertEquals("a~％~＿~~b", expression.escapeWildcard("a％＿~b"));
        });

    Assertions.assertEquals("sqls/", thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().getPrefix());
    Assertions.assertFalse(thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().isIncludesPackagePath());
    Assertions
        .assertFalse(thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().isSeparateDirectoryPerMapper());
    Assertions.assertFalse(
        thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().isIncludesMapperNameWhenSeparateDirectory());
    Assertions.assertFalse(thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().isCacheEnabled());

  }

  @Test
  void testCustomWithCustomizerFunction() {
    System.setProperty("mybatis-thymeleaf.config.file", "mybatis-thymeleaf-empty.properties");
    ThymeleafLanguageDriverConfig thymeleafLanguageDriverConfig = ThymeleafLanguageDriverConfig.newInstance(c -> {
      c.setUse2way(false);
      c.setCustomizer(CustomTemplateEngineCustomizer.class);
      c.getTemplateFile().setCacheEnabled(false);
      c.getTemplateFile().setCacheTtl(30000L);
      c.getTemplateFile().setEncoding(StandardCharsets.ISO_8859_1);
      c.getTemplateFile().setBaseDir("templates/");
      c.getTemplateFile().setPatterns("*.sql", "*.sql.template");
      c.getTemplateFile().getPathProvider().setPrefix("sqls/");
      c.getTemplateFile().getPathProvider().setIncludesPackagePath(false);
      c.getTemplateFile().getPathProvider().setSeparateDirectoryPerMapper(false);
      c.getTemplateFile().getPathProvider().setIncludesMapperNameWhenSeparateDirectory(false);
      c.getTemplateFile().getPathProvider().setCacheEnabled(false);
      c.getDialect().setPrefix("mbs");
      c.getDialect().setLikeEscapeChar('~');
      c.getDialect().setLikeEscapeClauseFormat("escape '%s'");
      c.getDialect().setLikeAdditionalEscapeTargetChars('％', '＿');
    });
    Configuration configuration = new Configuration();
    configuration.getLanguageRegistry().register(new ThymeleafLanguageDriver(thymeleafLanguageDriverConfig));
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);

    new SqlSessionFactoryBuilder().build(configuration);

    TemplateEngine templateEngine = CustomTemplateEngineCustomizer.templateEngine;
    ClassLoaderTemplateResolver classLoaderTemplateResolver = TemplateEngineCustomizer
        .extractTemplateResolver(templateEngine, ClassLoaderTemplateResolver.class)
        .orElseGet(() -> Assertions.fail("Cannot a ClassLoaderTemplateResolver instance."));

    Assertions.assertEquals(TemplateMode.TEXT, classLoaderTemplateResolver.getTemplateMode());
    Assertions.assertFalse(classLoaderTemplateResolver.isCacheable());
    Assertions.assertEquals(Long.valueOf(30000), classLoaderTemplateResolver.getCacheTTLMs());
    Assertions.assertEquals("ISO-8859-1", classLoaderTemplateResolver.getCharacterEncoding());
    Assertions.assertEquals("templates/", classLoaderTemplateResolver.getPrefix());
    Assertions.assertEquals(new LinkedHashSet<>(Arrays.asList("*.sql", "*.sql.template")),
        classLoaderTemplateResolver.getResolvablePatterns());

    StringTemplateResolver stringTemplateResolver = TemplateEngineCustomizer
        .extractTemplateResolver(templateEngine, StringTemplateResolver.class)
        .orElseGet(() -> Assertions.fail("Cannot a StringTemplateResolver instance."));
    Assertions.assertEquals(TemplateMode.TEXT, stringTemplateResolver.getTemplateMode());
    Assertions.assertFalse(stringTemplateResolver.isCacheable());

    templateEngine.getDialects().stream().filter(MyBatisDialect.class::isInstance).findFirst()
        .map(MyBatisDialect.class::cast).ifPresent(v -> {
          Assertions.assertEquals("mbs", v.getPrefix());
          Likes expression = (Likes) v.getExpressionObjectFactory().buildObject(null, null);
          Assertions.assertEquals("escape '~'", expression.escapeClause());
          Assertions.assertEquals("a~％~＿~~b", expression.escapeWildcard("a％＿~b"));
        });

    Assertions.assertEquals("sqls/", thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().getPrefix());
    Assertions.assertFalse(thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().isIncludesPackagePath());
    Assertions
        .assertFalse(thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().isSeparateDirectoryPerMapper());
    Assertions.assertFalse(
        thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().isIncludesMapperNameWhenSeparateDirectory());
    Assertions.assertFalse(thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().isCacheEnabled());
  }

  @Test
  void testCustomWithBuilderUsingCustomProperties() {
    System.setProperty("mybatis-thymeleaf.config.file", "mybatis-thymeleaf-empty.properties");
    Configuration configuration = new Configuration();
    Properties customProperties = new Properties();
    customProperties.setProperty("use2way", "false");
    customProperties.setProperty("customizer", "org.mybatis.scripting.thymeleaf.CustomTemplateEngineCustomizer");
    customProperties.setProperty("template-file.cache-enabled", "false");
    customProperties.setProperty("template-file.cache-ttl", "30000");
    customProperties.setProperty("template-file.encoding", "ISO-8859-1");
    customProperties.setProperty("template-file.base-dir", "templates/");
    customProperties.setProperty("template-file.patterns", "*.sql, *.sql.template");
    customProperties.setProperty("template-file.path-provider.prefix", "sqls/");
    customProperties.setProperty("template-file.path-provider.includes-package-path", "false");
    customProperties.setProperty("template-file.path-provider.separate-directory-per-mapper", "false");
    customProperties.setProperty("template-file.path-provider.includes-mapper-name-when-separate-directory", "false");
    customProperties.setProperty("template-file.path-provider.cache-enabled", "false");
    customProperties.setProperty("dialect.prefix", "mbs");
    customProperties.setProperty("dialect.like-escape-char", "~");
    customProperties.setProperty("dialect.like-escape-clause-format", "escape '%s'");
    customProperties.setProperty("dialect.like-additional-escape-target-chars", "％,＿");

    ThymeleafLanguageDriverConfig thymeleafLanguageDriverConfig = ThymeleafLanguageDriverConfig
        .newInstance(customProperties);

    configuration.getLanguageRegistry().register(new ThymeleafLanguageDriver(thymeleafLanguageDriverConfig));
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);

    new SqlSessionFactoryBuilder().build(configuration);

    TemplateEngine templateEngine = CustomTemplateEngineCustomizer.templateEngine;
    ClassLoaderTemplateResolver classLoaderTemplateResolver = TemplateEngineCustomizer
        .extractTemplateResolver(templateEngine, ClassLoaderTemplateResolver.class)
        .orElseGet(() -> Assertions.fail("Cannot a ClassLoaderTemplateResolver instance."));

    Assertions.assertEquals(TemplateMode.TEXT, classLoaderTemplateResolver.getTemplateMode());
    Assertions.assertFalse(classLoaderTemplateResolver.isCacheable());
    Assertions.assertEquals(Long.valueOf(30000), classLoaderTemplateResolver.getCacheTTLMs());
    Assertions.assertEquals("ISO-8859-1", classLoaderTemplateResolver.getCharacterEncoding());
    Assertions.assertEquals("templates/", classLoaderTemplateResolver.getPrefix());
    Assertions.assertEquals(new LinkedHashSet<>(Arrays.asList("*.sql", "*.sql.template")),
        classLoaderTemplateResolver.getResolvablePatterns());

    StringTemplateResolver stringTemplateResolver = TemplateEngineCustomizer
        .extractTemplateResolver(templateEngine, StringTemplateResolver.class)
        .orElseGet(() -> Assertions.fail("Cannot a StringTemplateResolver instance."));
    Assertions.assertEquals(TemplateMode.TEXT, stringTemplateResolver.getTemplateMode());
    Assertions.assertFalse(stringTemplateResolver.isCacheable());

    templateEngine.getDialects().stream().filter(MyBatisDialect.class::isInstance).findFirst()
        .map(MyBatisDialect.class::cast).ifPresent(v -> {
          Assertions.assertEquals("mbs", v.getPrefix());
          Likes expression = (Likes) v.getExpressionObjectFactory().buildObject(null, null);
          Assertions.assertEquals("escape '~'", expression.escapeClause());
          Assertions.assertEquals("a~％~＿~~b", expression.escapeWildcard("a％＿~b"));
        });

    Assertions.assertEquals("sqls/", thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().getPrefix());
    Assertions.assertFalse(thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().isIncludesPackagePath());
    Assertions
        .assertFalse(thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().isSeparateDirectoryPerMapper());
    Assertions.assertFalse(
        thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().isIncludesMapperNameWhenSeparateDirectory());
    Assertions.assertFalse(thymeleafLanguageDriverConfig.getTemplateFile().getPathProvider().isCacheEnabled());
  }

  @Test
  void testConfigFileNotFound() {
    System.setProperty("mybatis-thymeleaf.config.file", "mybatis-thymeleaf-dummy.properties");
    Configuration configuration = new Configuration();
    configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);
    Assertions.assertEquals(ThymeleafLanguageDriver.class, configuration.getLanguageRegistry().getDefaultDriverClass());
  }

  @Test
  void testConfigFileNotFoundAtMethodArgument() {
    ThymeleafLanguageDriverConfig.newInstance("mybatis-thymeleaf-dummy.properties");
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
      Assertions.assertEquals(
          "Failed to load language driver for org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriver", e.getMessage());
      // Since mybatis 3.5.1, exception is wrapped by InvocationTargetException
      Throwable cause = e.getCause() instanceof InvocationTargetException ? e.getCause().getCause() : e.getCause();
      Assertions.assertEquals(
          "java.lang.ClassNotFoundException: Cannot find class: org.mybatis.scripting.thymeleaf.FooTemplateEngineCustomizer",
          cause.getMessage());
    }
  }

  @Test
  void testCustomizerNotCreation() {
    System.setProperty("mybatis-thymeleaf.config.file",
        "mybatis-thymeleaf-customizer-no-default-constructor.properties");
    Configuration configuration = new Configuration();
    try {
      configuration.setDefaultScriptingLanguage(ThymeleafLanguageDriver.class);
      Assertions.fail();
    } catch (ScriptingException e) {
      Assertions.assertEquals(
          "Failed to load language driver for org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriver", e.getMessage());
      // Since mybatis 3.5.1, exception is wrapped by InvocationTargetException
      Throwable cause = e.getCause() instanceof InvocationTargetException ? e.getCause().getCause() : e.getCause();
      Assertions.assertEquals(
          "Cannot create an instance for class: class org.mybatis.scripting.thymeleaf.NoDefaultConstructorTemplateEngineCustomizer",
          cause.getMessage());
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
      Assertions.assertEquals("unexpected token: SQL in statement [sql/NameMapper/findById.sql]",
          e.getCause().getMessage());
    }
  }

}
