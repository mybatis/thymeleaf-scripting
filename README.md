# MyBatis Thymeleaf 3 Support

[![Java CI](https://github.com/mybatis/thymeleaf-scripting/actions/workflows/ci.yaml/badge.svg)](https://github.com/mybatis/thymeleaf-scripting/actions/workflows/ci.yaml)
[![Coverage Status](https://coveralls.io/repos/github/mybatis/thymeleaf-scripting/badge.svg?branch=master)](https://coveralls.io/github/mybatis/thymeleaf-scripting?branch=master)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/org.mybatis.scripting/mybatis-thymeleaf/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.mybatis.scripting/mybatis-thymeleaf)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/org.mybatis.scripting/mybatis-thymeleaf.svg)](https://oss.sonatype.org/content/repositories/snapshots/org/mybatis/scripting/mybatis-thymeleaf/)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

![thymeleaf-scripting](http://mybatis.github.io/images/mybatis-logo.png)

MyBatis Thymeleaf 3 Scripting Support.

## Introduction

The mybatis-thymeleaf is a plugin that helps applying a SQL using template provided by Thymeleaf 3.
If you are not familiar with Thymeleaf 3 syntax, you can see the Thymeleaf documentations.

* https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#textual-template-modes

### Simple bindable 2-way SQL

```sql
SELECT * FROM names
  WHERE id = /*[# mb:p="id"]*/ 1 /*[/]*/
```

### Dynamic bindable 2-way SQL

```sql
SELECT * FROM names
  WHERE 1 = 1
  /*[# th:if="${not #lists.isEmpty(ids)}"]*/
    AND id IN (
      /*[# mb:p="ids"]*/ 1 /*[/]*/
    )
  /*[/]*/
  ORDER BY id
```

### Dynamic bindable SQL(non 2-way)

```sql
SELECT * FROM names
  WHERE 1 = 1
  [# th:if="${not #lists.isEmpty(ids)}"]
    AND id IN (
      [# mb:p="ids" /]
    )
  [/]
  ORDER BY id
```

## Requirements

  * Java 8, Java 11+
  * MyBatis 3.4.3+ (Recommend to use 3.5+ or 3.4.x latest version)
  * Thymeleaf 3.0+ (Recommend to use 3.0.x latest version)

## Documentation

* [Published User's Guide](https://www.mybatis.org/thymeleaf-scripting/user-guide.html)


* [Snapshot User's Guide](src/main/asciidoc/user-guide.adoc)


## Related Resources

* [Quick Start](https://github.com/mybatis/thymeleaf-scripting/wiki/Quick-Start)
* [Code completion](https://github.com/mybatis/thymeleaf-scripting/wiki/Code-completion)
* [Usage on framework](https://github.com/mybatis/thymeleaf-scripting/wiki/Usage-on-framework)


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
