/*
 *    Copyright 2018-2024 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.scripting.thymeleaf.integrationtest.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Name;

public interface XmlNameMapper {
  void insert(Name name);

  void update(Name name);

  void delete(Name name);

  List<Name> getAllNames();

  List<Name> findByIds(@Param("ids") List<Integer> ids);

  List<Name> findByIdsWithoutParamAnnotation(List<Integer> ids);

  List<Name> findById(@Param("id") Integer id);

  List<Name> findByIdWithoutParamAnnotation(Integer id);

  List<Name> findByIdWithNestedParam(@Param("p") NameParam param);

  List<Name> findUsingScript(NameParam nameParam);

  List<Name> findUsingTemplateFile(NameParam nameParam);

  List<Name> findByName(NameParam param);

}
