MyBatis Thymeleaf Support
========================

[![Build Status](https://travis-ci.org/kazuki43zoo/thymeleaf-scripting.svg?branch=master)](https://travis-ci.org/kazuki43zoo/thymeleaf-scripting)
[![Coverage Status](https://coveralls.io/repos/kazuki43zoo/thymeleaf-scripting/badge.svg?branch=master&service=github)](https://coveralls.io/github/kazuki43zoo/thymeleaf-scripting?branch=master)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/org.mybatis.scripting/mybatis-thymeleaf/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.mybatis.scripting/mybatis-thymeleaf)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

![thymeleaf-scripting](http://mybatis.github.io/images/mybatis-logo.png)

MyBatis Thymeleaf Scripting Support.

Getting started
===============

## Introduction

The mybatis-thymeleaf is a plugin that helps creating a simple SQL and dynamic SQL as 2-way SQL(natural SQL).
If you are not familiar with Thymeleaf syntax, you can view [Tutorial: Using Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html).

e.g.) a simple sql

```sql
SELECT * FROM names
  WHERE id = /*[('#{id}')]*/ 1
```

mybatis-thymeleaf(thymeleaf) translate to as follow:

```sql
SELECT * FROM names
  WHERE id = #{id}
```

e.g.) a dynamic sql

```sql
SELECT * FROM names
  WHERE 1 = 1
  /*[# th:if="${not #lists.isEmpty(ids)}"]*/
    AND id in (
    /*[# th:each="id : ${ids}"]*/
      /*[+ [# th:if="${not idStat.first}"][(',')][/] +]*/
      /*[('#{ids[' + ${idStat.index} + ']}')]*/ 1
    /*[/]*/
    )
  /*[/]*/
  ORDER BY id
```

if 'ids' is empty, mybatis-thymeleaf(thymeleaf) translate to as follow:
```sql
SELECT * FROM names
  WHERE 1 = 1
  ORDER BY id
```

if 'ids' has 3 elements, mybatis-thymeleaf(thymeleaf) translate to as follow:
```sql
SELECT * FROM names
  WHERE 1 = 1
    AND id in (
      #{ids[0]}
       , 
      #{ids[1]}
       , 
      #{ids[2]}
    )
  ORDER BY id
```
  
## Install

The mybatis-thymeleaf is available in Maven Central. So, if you are using maven, you can add as follow:

```xml
<dependency>
  <groupId>org.mybatis.scripting</groupId>
  <artifactId>mybatis-thymeleaf</artifactId>
  <version>1.0.0</version>
</dependency>
```

If you are using gradle, you can add as follow:

```groovy
dependencies {
  compile("org.mybatis.scripting:mybatis-thymeleaf:1.0.0")
}
```

## Configuration

### XML-driven

If you are using XML-driven, you may need to do next steps:

- Register the language driver alias in your mybatis configuration file:

```xml
<configuration>
  ...
  <typeAliases>
    <typeAlias alias="THYMELEAF" type="org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriver"/>
  </typeAliases>
  ...
</configuration>
```

- (Optional) Set the THYMELEAF as your default scripting language:

```xml
<configuration>
  ...
  <settings>
    <setting name="defaultScriptingLanguage" value="THYMELEAF"/>
  </settings>
  ...
</configuration>
```
