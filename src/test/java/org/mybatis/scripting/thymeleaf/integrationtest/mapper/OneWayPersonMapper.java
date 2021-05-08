/*
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
import org.mybatis.scripting.thymeleaf.integrationtest.domain.Mail;

public interface OneWayPersonMapper {

  @Select("sql/OneWayPersonMapper/selectMailsByConditionsArray.sql")
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
