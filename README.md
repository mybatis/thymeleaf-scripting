# MyBatis Thymeleaf 3 Support

[![Build Status](https://travis-ci.org/mybatis/thymeleaf-scripting.svg?branch=master)](https://travis-ci.org/mybatis/thymeleaf-scripting)
[![Coverage Status](https://coveralls.io/repos/github/mybatis/thymeleaf-scripting/badge.svg?branch=master)](https://coveralls.io/github/mybatis/thymeleaf-scripting?branch=master)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/org.mybatis.scripting/mybatis-thymeleaf/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.mybatis.scripting/mybatis-thymeleaf)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

![thymeleaf-scripting](http://mybatis.github.io/images/mybatis-logo.png)

MyBatis Thymeleaf 3 Scripting Support.

## Introduction

The mybatis-thymeleaf is a plugin that helps applying a 2-way SQL using natural template provided by Thymeleaf 3.
If you are not familiar with Thymeleaf 3 syntax, you can see the Thymeleaf documentations.

* [Tutorial: Using Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html)
* [Tutorial: Using Thymeleaf -13 Textual template modes-](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#textual-template-modes)

### Simple SQL

```sql
SELECT * FROM names
  WHERE id = /*[('#{id}')]*/ 1
```

The mybatis-thymeleaf translate to as follow:

```sql
SELECT * FROM names
  WHERE id = #{id}
```

### Dynamic SQL

```sql
SELECT * FROM names
  WHERE 1 = 1
  /*[# th:if="${not #lists.isEmpty(ids)}"]*/
    AND id IN (
    /*[# th:each="id : ${ids}"]*/
      /*[+ [# th:if="${not idStat.first}"][(',')][/] +]*/
      /*[('#{ids[' + ${idStat.index} + ']}')]*/ 1
    /*[/]*/
    )
  /*[/]*/
  ORDER BY id
```

If `ids` is empty, the mybatis-thymeleaf translate to as follow:
```sql
SELECT * FROM names
  WHERE 1 = 1
  ORDER BY id
```

If `ids` has 3 elements, the mybatis-thymeleaf translate to as follow:
```sql
SELECT * FROM names
  WHERE 1 = 1
    AND id IN (
      #{ids[0]}
       , 
      #{ids[1]}
       , 
      #{ids[2]}
    )
  ORDER BY id
```

## Requirements

  * Java 8, Java 11+
  * MyBatis 3.4.3+
  * Thymeleaf 3.0+


## Installation

### Maven

If you are using the Maven as build tool, you can add as follow:

```xml
<dependency>
  <groupId>org.mybatis</groupId>
  <artifactId>mybatis</artifactId>
  <version>3.4.6</version> <!-- Adjust to your application -->
</dependency>
<dependency>
  <groupId>org.mybatis.scripting</groupId>
  <artifactId>mybatis-thymeleaf</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

If you use a snapshot version, you need add the configuration for using the "Sonatype OSS Snapshots Repository" as follow:

```xml
<repositories>
  <!-- ... -->
  <repository>
    <id>sonatype-oss-snapshots</id>
    <name>Sonatype OSS Snapshots Repository</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  </repository>
</repositories>
```

### Gradle

If you are using the Gradle as build tool, you can add as follow:

```groovy
dependencies {
  // ...
  compile("org.mybatis:mybatis:3.4.6") // Adjust version to your application
  compile("org.mybatis.scripting:mybatis-thymeleaf:1.0.0-SNAPSHOT")
}
```

If you use a snapshot version, you need add the configuration for using the "Sonatype OSS Snapshots Repository" as follow:

```groovy
repositories {
  // ...
  maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
}
```

## Documentation

* [User's Guide](src/main/asciidoc/user-guide.adoc)

## Contact us

### Question

When there is a question, at first please confirm whether exists same question at following web sites.

* [Google group for mybatis user(official mailing list)](https://groups.google.com/forum/#!forum/mybatis-user)
* [Stack Overflow tagged mybatis](https://stackoverflow.com/questions/tagged/mybatis)

If you cannot find a same question, please post new question to the [mailing list](https://groups.google.com/forum/#!newtopic/mybatis-user) or the [Stack Overflow](https://stackoverflow.com/questions/ask?tags=mybatis,mybatis-thymeleaf).

### Report and Request

When you found a bug or want to submit a feature request(new feature or current feature improvement), at first please confirm whether exists same bug or request at following pages.

* [Issue List](https://github.com/mybatis/thymeleaf-scripting/issues)
* [Pull Request List](https://github.com/mybatis/thymeleaf-scripting/pulls)

If you cannot find a same report or request, please post new issue to the [issue tracker](https://github.com/mybatis/thymeleaf-scripting/issues/new).

> **IMPORTANT:**
>
> When you found a security vulnerability, at first please report it to the [mybatis organization members](https://github.com/orgs/mybatis/people) instead of issue tracker.
