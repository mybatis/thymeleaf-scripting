/*
 *    Copyright 2018-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.mybatis.scripting.thymeleaf.expression.Likes;
import org.mybatis.scripting.thymeleaf.processor.BindVariableRender;
import org.mybatis.scripting.thymeleaf.processor.MyBatisBindTagProcessor;
import org.mybatis.scripting.thymeleaf.processor.MyBatisParamTagProcessor;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * The Dialect for integrating with MyBatis. <br>
 * This dialect provides following features. This dialect prefix is {@code "mb"} by default.
 *
 * <ul>
 * <li>{@code #likes} expression : {@link Likes}</li>
 * <li>{@code mb:p} attribute tag: {@link MyBatisParamTagProcessor}</li>
 * <li>{@code mb:bind} attribute tag : {@link MyBatisBindTagProcessor}</li>
 * </ul>
 *
 * @author Kazuki Shimizu
 * @version 1.0.0
 */
public class MyBatisDialect extends AbstractProcessorDialect implements IExpressionObjectDialect {

  private static final String DEFAULT_PREFIX = "mb";

  private Likes likes = Likes.newBuilder().build();

  private BindVariableRender bindVariableRender;

  /**
   * Default constructor.
   */
  public MyBatisDialect() {
    this(DEFAULT_PREFIX);
  }

  /**
   * Constructor that can be specified the dialect prefix.
   *
   * @param prefix
   *          A dialect prefix
   */
  public MyBatisDialect(String prefix) {
    super("MyBatis Dialect", prefix, StandardDialect.PROCESSOR_PRECEDENCE);
  }

  /**
   * Set an expression utility object that provide helper method for like feature. <br>
   *
   * @param likes
   *          An expression utility object that provide helper method for like feature
   */
  public void setLikes(Likes likes) {
    this.likes = likes;
  }

  /**
   * Set a bind variable render.
   *
   * @param bindVariableRender
   *          a bind variable render
   * @since 1.0.2
   */
  public void setBindVariableRender(BindVariableRender bindVariableRender) {
    this.bindVariableRender = bindVariableRender;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<IProcessor> getProcessors(String dialectPrefix) {
    return new HashSet<>(Arrays.asList(new MyBatisBindTagProcessor(TemplateMode.TEXT, dialectPrefix),
        new MyBatisBindTagProcessor(TemplateMode.CSS, dialectPrefix),
        configure(new MyBatisParamTagProcessor(TemplateMode.TEXT, dialectPrefix)),
        configure(new MyBatisParamTagProcessor(TemplateMode.CSS, dialectPrefix))));
  }

  private MyBatisParamTagProcessor configure(MyBatisParamTagProcessor processor) {
    Optional.ofNullable(bindVariableRender).ifPresent(processor::setBindVariableRender);
    return processor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IExpressionObjectFactory getExpressionObjectFactory() {
    return new MyBatisExpressionObjectFactory();
  }

  private class MyBatisExpressionObjectFactory implements IExpressionObjectFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAllExpressionObjectNames() {
      return Collections.singleton("likes");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object buildObject(IExpressionContext context, String expressionObjectName) {
      return likes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCacheable(String expressionObjectName) {
      return true;
    }

  }

}
