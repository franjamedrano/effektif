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
package com.effektif.workflow.test.execution;

import org.junit.Test;

import com.effektif.workflow.api.activities.UserTask;
import com.effektif.workflow.api.command.StartCommand;
import com.effektif.workflow.api.variables.List;
import com.effektif.workflow.api.variables.Text;
import com.effektif.workflow.api.workflow.MultiInstance;
import com.effektif.workflow.api.workflow.Workflow;
import com.effektif.workflow.api.workflowinstance.WorkflowInstance;
import com.effektif.workflow.impl.util.Lists;
import com.effektif.workflow.test.TestHelper;
import com.effektif.workflow.test.WorkflowTest;


public class MultiInstanceTest extends WorkflowTest {
  
  @Test
  public void testTask() throws Exception {
    Workflow workflow = new Workflow()
      .variable(new List("reviewers", new Text()))
      .activity(new UserTask("Review")
        .candidateIdVariableId("reviewer")
        .multiInstance(new MultiInstance()
          .valuesVariableId("reviewers")
          .variable(new Text("reviewer"))));
    
    workflow = deploy(workflow);
    
    WorkflowInstance workflowInstance = workflowEngine.startWorkflowInstance(new StartCommand()
      .workflowId(workflow.getId())
      .variableValue("reviewers", Lists.of("John", "Jack", "Mary")));

    // TODO make it so that the parent activity 
    // instance doesn't have a name and doesn't have the empty variable declaration
    TestHelper.assertOpen(workflowInstance, "Review", "Review", "Review", "Review");
  }
}
