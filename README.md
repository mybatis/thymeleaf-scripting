# MyBatis Thymeleaf 3 Support

[![Build Status](https://travis-ci.org/mybatis/thymeleaf-scripting.svg?branch=master)](https://travis-ci.org/mybatis/thymeleaf-scripting)
[![Coverage Status](https://coveralls.io/repos/github/mybatis/thymeleaf-scripting/badge.svg?branch=master)](https://coveralls.io/github/mybatis/thymeleaf-scripting?branch=master)
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

## Documentation

* [User's Guide](src/main/asciidoc/user-guide.adoc)
