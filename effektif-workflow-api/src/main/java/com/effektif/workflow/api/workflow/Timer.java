/* Copyright 2014 Effektif GmbH.
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

import java.util.Map;


public class Timer extends Base {

  protected String type;
  protected Map<String,Object> configuration;
  protected String duedate;
  protected String repeat;

  public String getType() {
    return this.type;
  }
  public void setType(String type) {
    this.type = type;
  }
  public Timer type(String type) {
    this.type = type;
    return this;
  }
  
  public Map<String,Object> getConfiguration() {
    return this.configuration;
  }
  public void setConfiguration(Map<String,Object> configuration) {
    this.configuration = configuration;
  }
  public Timer configuration(Map<String,Object> configuration) {
    this.configuration = configuration;
    return this;
  }

  public String getDuedate() {
    return this.duedate;
  }
  public void setDuedate(String duedate) {
    this.duedate = duedate;
  }
  public Timer duedate(String duedate) {
    this.duedate = duedate;
    return this;
  }

  public String getRepeat() {
    return this.repeat;
  }
  public void setRepeat(String repeat) {
    this.repeat = repeat;
  }
  public Timer repeat(String repeat) {
    this.repeat = repeat;
    return this;
  }
}