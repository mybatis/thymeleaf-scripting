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
package org.mybatis.scripting.thymeleaf.integrationtest.mapper;

import java.util.List;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.FetchType;
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Mail;
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Person;

public interface PersonMapper {

  @Options(useGeneratedKeys = true, keyProperty = "id")
  @Insert("sql/PersonMapper/insertByBulk.sql")
  void insertByBulk(List<Person> persons);

  @Insert("sql/PersonMapper/insertMailsByBulk.sql")
  void insertMailsByBulk(List<Person> persons);

  @Select("SELECT MAX(id) FROM person_mails")
  Integer getMaxMailId();

  @Select("SELECT id, name FROM persons WHERE id IN ([# mb:p='ids'/]) ORDER BY id")
  @Results({
      @Result(property = "id", column = "id", id = true),
      @Result(property = "mails", column = "id", many = @Many(select = "selectPersonMails", fetchType = FetchType.EAGER))
  })
  List<Person> selectPersons(@Param("ids") int... ids);

  @Select("SELECT id, person_id, address FROM person_mails WHERE person_id = #{id} ORDER BY id")
  List<Mail> selectPersonMails(int personId);

  @Select("SELECT id, person_id, address FROM person_mails WHERE address IN ([# mb:p='conditions.mails'/]) ORDER BY id")
  List<Mail> selectMailsByConditions(@Param("conditions") Conditions conditions);

  @Select("sql/PersonMapper/selectMailsByConditionsArray.sql")
  List<Mail> selectMailsByConditionsArray(List<Conditions> list);

  class Conditions {
    private List<String> mails;

    public List<String> getMails() {
      return mails;
    }

    public void setMails(List<String> mails) {
      this.mails = mails;
    }
  }

}
