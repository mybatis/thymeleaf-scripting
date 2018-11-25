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
package org.mybatis.scripting.thymeleaf.integrationtest.mapper;

import org.apache.ibatis.annotations.*;
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Name;

import java.util.List;

public interface NameMapper {

  @Options(useGeneratedKeys = true, keyProperty = "id")
  @Insert("sql/NameMapper/insert.sql")
  void insert(Name name);

  @Update("sql/NameMapper/update.sql")
  void update(Name name);

  @Delete("sql/NameMapper/delete.sql")
  void delete(Name name);

  @Select("SELECT * FROM names")
  List<Name> getAllNames();

  @Select("sql/NameMapper/findByIds.sql")
  List<Name> findByIds(@Param("ids") List<Integer> ids);

  @Select("sql/NameMapper/findByIdsWithoutParamAnnotation.sql")
  List<Name> findByIdsWithoutParamAnnotation(List<Integer> ids);

  @Select("sql/NameMapper/findById.sql")
  List<Name> findById(@Param("id") Integer id);

  @Select("sql/NameMapper/findByIdWithoutParamAnnotation.sql")
  List<Name> findByIdWithoutParamAnnotation(Integer id);

  @Select("sql/NameMapper/findByIdWithNestedParam.sql")
  List<Name> findByIdWithNestedParam(@Param("p") NameParam param);

  @Select({
      "SELECT * FROM names",
      "/*[# th:insert=\"~{sql/NameMapper/findByIdWhere.sql}\" /]*/"
  })
  List<Name> findUsingScript(NameParam nameParam);

  @Select("sql/NameMapper/findById.sql")
  List<Name> findUsingTemplateFile(NameParam nameParam);

  @Select("sql/NameMapper/findByName.sql")
  List<Name> findByName(NameParam param);
}
