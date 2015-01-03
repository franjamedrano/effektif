/* Copyright (c) 2014, Effektif GmbH.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package com.effektif.workflow.api.workflow;


/**
 * @author Tom Baeyens
 */
public class Binding<T> {

  protected Object value;
  protected String variableId;
  protected String expression;

  public Object getValue() {
    return this.value;
  }
  public void setValue(Object value) {
    this.value = value;
  }
  public Binding value(Object value) {
    this.value = value;
    return this;
  }
  
  public String getVariableId() {
    return this.variableId;
  }
  public void setVariableId(String variableId) {
    this.variableId = variableId;
  }
  public Binding variableId(String variableId) {
    this.variableId = variableId;
    return this;
  }
  
  public String getExpression() {
    return this.expression;
  }
  public void setExpression(String expression) {
    this.expression = expression;
  }
  public Binding expression(String expression) {
    this.expression = expression;
    return this;
  }
}