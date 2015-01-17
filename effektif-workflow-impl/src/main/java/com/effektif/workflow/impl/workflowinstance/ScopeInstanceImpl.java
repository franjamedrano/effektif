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
package com.effektif.workflow.impl.workflowinstance;

import static com.effektif.workflow.impl.workflowinstance.ActivityInstanceImpl.STATE_STARTING;
import static com.effektif.workflow.impl.workflowinstance.ActivityInstanceImpl.STATE_STARTING_MULTI_CONTAINER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.effektif.workflow.api.WorkflowEngine;
import com.effektif.workflow.api.command.RequestContext;
import com.effektif.workflow.api.workflowinstance.ActivityInstance;
import com.effektif.workflow.api.workflowinstance.ScopeInstance;
import com.effektif.workflow.api.workflowinstance.TimerInstance;
import com.effektif.workflow.api.workflowinstance.VariableInstance;
import com.effektif.workflow.impl.ExpressionService;
import com.effektif.workflow.impl.Time;
import com.effektif.workflow.impl.plugin.DataType;
import com.effektif.workflow.impl.plugin.ServiceRegistry;
import com.effektif.workflow.impl.plugin.TypedValue;
import com.effektif.workflow.impl.type.AnyDataTypeImpl;
import com.effektif.workflow.impl.type.NumberTypeImpl;
import com.effektif.workflow.impl.type.TextTypeImpl;
import com.effektif.workflow.impl.workflow.ActivityImpl;
import com.effektif.workflow.impl.workflow.BindingImpl;
import com.effektif.workflow.impl.workflow.ScopeImpl;
import com.effektif.workflow.impl.workflow.VariableImpl;


public abstract class ScopeInstanceImpl extends BaseInstanceImpl {
  
  public static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

  public RequestContext requestContext;
  
  public ScopeImpl scope;
  public Long start;
  public Long end;
  public Long duration;
  public List<ActivityInstanceImpl> activityInstances;
  public List<VariableInstanceImpl> variableInstances;
  public Map<String, VariableInstanceImpl> variableInstancesMap;
  public List<TimerInstanceImpl> timerInstances;

  // As long as the workflow instance is not saved, the updates collection is null.
  // That means it's not yet necessary to collect the updates. 
  public ScopeInstanceUpdates updates;
  
  public ScopeInstanceImpl() {
  }

  public ScopeInstanceImpl(ScopeInstanceImpl parent, ScopeImpl scope, String id) {
    super(parent, id);
    this.requestContext = parent.requestContext;
    this.scope = scope;
    this.start = Time.now();
  }

  public abstract void setEnd(Long end); 
  
  public abstract void ended(ActivityInstanceImpl activityInstance);

  public abstract boolean isProcessInstance();
  
  protected void toScopeInstance(ScopeInstance scopeInstanceApi) {
    scopeInstanceApi.setId(id);
    scopeInstanceApi.setStart(start);
    scopeInstanceApi.setEnd(end);
    scopeInstanceApi.setDuration(duration);
    if (activityInstances!=null && !activityInstances.isEmpty()) {
      List<ActivityInstance> activityInstanceApis = new ArrayList<>();
      for (ActivityInstanceImpl activityInstanceImpl: this.activityInstances) {
        activityInstanceApis.add(activityInstanceImpl.toActivityInstance());
      }
      scopeInstanceApi.setActivityInstances(activityInstanceApis);
    }
    if (variableInstances!=null && !variableInstances.isEmpty()) {
      List<VariableInstance> variableInstanceApis = new ArrayList<>();
      for (VariableInstanceImpl variableInstanceImpl: this.variableInstances) {
        variableInstanceApis.add(variableInstanceImpl.toVariableInstance());
      }
      scopeInstanceApi.setVariableInstances(variableInstanceApis);
    }
    if (timerInstances!=null && !timerInstances.isEmpty()) {
      List<TimerInstance> timerInstanceApis = new ArrayList<>();
      for (TimerInstanceImpl timerInstanceImpl: this.timerInstances) {
        timerInstanceApis.add(timerInstanceImpl.toTimerInstance());
      }
      scopeInstanceApi.setTimerInstances(timerInstanceApis);
    }
  }

  public void execute(ActivityImpl activity) {
    createActivityInstance(activity);
  }

  public ActivityInstanceImpl createActivityInstance(ActivityImpl activity) {
    String activityInstanceId = workflowInstance.generateNextActivityInstanceId();
    ActivityInstanceImpl activityInstance = new ActivityInstanceImpl(this, activity, activityInstanceId);
    if (activity.isMultiInstance()) {
      activityInstance.setWorkState(STATE_STARTING_MULTI_CONTAINER);
    } else {
      activityInstance.setWorkState(STATE_STARTING);
    }
    workflowInstance.addWork(activityInstance);
    activityInstance.start = Time.now();
    if (updates!=null) {
      activityInstance.updates = new ActivityInstanceUpdates(true);
      if (parent!=null) {
        parent.propagateActivityInstanceChange();
      }
    }
    addActivityInstance(activityInstance);
    activityInstance.initializeVariableInstances();
    if (log.isDebugEnabled())
      log.debug("Created "+activityInstance);
    return activityInstance;
  }
  
  public void initializeForEachElement(VariableImpl elementVariableDefinition, Object value) {
    VariableInstanceImpl elementVariableInstance = createVariableInstance(elementVariableDefinition);
    elementVariableInstance.setValue(value);
  }

  public void addActivityInstance(ActivityInstanceImpl activityInstance) {
    if (activityInstances==null) {
      activityInstances = new ArrayList<>();
    }
    activityInstance.parent = this;
    activityInstances.add(activityInstance);
  }
  
  public void initializeVariableInstances() {
    if (scope.variables!=null && !scope.variables.isEmpty()) {
      for (VariableImpl variable: scope.variables.values()) {
        createVariableInstance(variable);
      }
    }
  }

  public VariableInstanceImpl createVariableInstance(VariableImpl variable) {
    String variableInstanceId = workflowInstance.generateNextVariableInstanceId();
    VariableInstanceImpl variableInstance = new VariableInstanceImpl(this, variable, variableInstanceId);
    variableInstance.workflowEngine = workflowEngine;
    variableInstance.workflowInstance = workflowInstance;
    variableInstance.type = variable.type;
    variableInstance.value = variable.initialValue;
    variableInstance.variable = variable;
    if (updates!=null) {
      variableInstance.updates = new VariableInstanceUpdates(true);
      updates.isVariableInstancesChanged = true;
      if (parent!=null) {
        parent.propagateActivityInstanceChange();
      }
    }
    addVariableInstance(variableInstance);
    return variableInstance;
  }

  public void addVariableInstance(VariableInstanceImpl variableInstance) {
    variableInstance.parent = this;
    if (variableInstances==null) {
      variableInstances = new ArrayList<>();
    }
    variableInstances.add(variableInstance);
  }
  
  @SuppressWarnings("unchecked")
  public <T> T getValue(BindingImpl<T> binding) {
    if (binding==null) {
      return null;
    }
    if (binding.value!=null) {
      return (T) binding.value;
    }
    if (binding.variableId!=null) {
      return (T) getVariable(binding.variableId);
    }
    if (binding.expression!=null) {
      ExpressionService expressionService = getServiceRegistry().getService(ExpressionService.class);
      return (T) expressionService.execute(binding.expression, this);
    }
    return null;
  }

  public <T> List<T> getValues(List<BindingImpl<T>> bindings) {
    List<T> values = new ArrayList<>();
    if (bindings!=null) {
      for (BindingImpl<T> binding: bindings) {
        values.add(getValue(binding));
      }
    }
    return values;
  }

  public <T> List<T> getValuesFlat(List<BindingImpl<T>> bindings) {
    List<?> values = getValues(bindings);
    List<T> flatValues = new ArrayList<>();
    if (values!=null) {
      for (Object value: values) {
        if (value instanceof Collection) {
          flatValues.addAll((Collection)value);
        } else {
          flatValues.add((T) value);
        }
      }
    }
    return flatValues;
  }

  public Object getVariable(String variableDefinitionId) {
    TypedValue typedValue = getVariableTypedValue(variableDefinitionId);
    return typedValue!=null ? typedValue.getValue() : null;
  }

  /** sets all entries individually, variableValues maps variable ids to values */
  public void setVariableValues(Map<String, Object> variableValues) {
    if (variableValues!=null) {
      for (String variableId: variableValues.keySet()) {
        Object value = variableValues.get(variableId);
        setVariableValue(variableId, value);
      }
    }
  }

  public void setVariableValue(String variableId, Object value) {
    if (variableInstances!=null) {
      VariableInstanceImpl variableInstance = getVariableInstanceLocal(variableId);
      if (variableInstance!=null) {
        variableInstance.setValue(value);
        if (updates!=null) {
          updates.isVariableInstancesChanged = true;
          if (parent!=null) { 
            parent.propagateActivityInstanceChange();
          }
        }
        return;
      }
    }
    if (parent!=null) {
      parent.setVariableValue(variableId, value);
      return;
    }
    createVariableInstanceByValue(value);
  }
  
  public void createVariableInstanceByValue(Object value) {
    VariableImpl variable = new VariableImpl();
    if (value instanceof String) {
      variable.type = new TextTypeImpl();
    } else if (value instanceof Number) {
      variable.type = new NumberTypeImpl();
    } else {
      variable.type = new AnyDataTypeImpl();
    }
    VariableInstanceImpl variableInstance = createVariableInstance(variable);
    variableInstance.setValue(value);
  }
  
  public VariableInstanceImpl findVariableInstance(String variableId) {
    if (variableInstances!=null) {
      VariableInstanceImpl variableInstance = getVariableInstanceLocal(variableId);
      if (variableInstance!=null) {
        return variableInstance;
      }
    }
    if (parent!=null) {
      return parent.findVariableInstance(variableId);
    }
    return null;
  }
  
  public TypedValue getVariableTypedValue(String variableId) {
    VariableInstanceImpl variableInstance = findVariableInstance(variableId);
    if (variableInstance!=null) {
      DataType type = variableInstance.variable.type;
      Object value = variableInstance.getValue();
      return new TypedValue(type, value);
    }
    throw new RuntimeException("Variable "+variableId+" is not defined in "+getClass().getSimpleName()+" "+toString());
  }
  
  protected VariableInstanceImpl getVariableInstanceLocal(String variableId) {
    ensureVariableInstancesMapInitialized();
    return variableInstancesMap.get(variableId);
  }

  protected void ensureVariableInstancesMapInitialized() {
    if (variableInstancesMap==null && variableInstances!=null) {
      variableInstancesMap = new HashMap<>();
      for (VariableInstanceImpl variableInstance: variableInstances) {
        variableInstancesMap.put(variableInstance.variable.id, variableInstance);
      }
    }
  }
  
  
  
  public abstract void end();

  public boolean hasOpenActivityInstances() {
    if (activityInstances==null) {
      return false;
    }
    for (ActivityInstanceImpl activityInstance: activityInstances) {
      if (!activityInstance.isEnded()) {
        return true;
      }
    }
    return false;
  }

  
  /** searches for the variable starting in this activity and upwards over the parent hierarchy */ 
  public void setVariableByName(String variableName, Object value) {
  }

  /** scans this activity and the nested activities */
  public ActivityInstanceImpl findActivityInstance(String activityInstanceId) {
    if (activityInstances!=null) {
      for (ActivityInstanceImpl activityInstance: activityInstances) {
        ActivityInstanceImpl theOne = activityInstance.findActivityInstance(activityInstanceId);
        if (theOne!=null) {
          return theOne;
        }
      }
    }
    return null;
  }
  
  public ActivityInstanceImpl findActivityInstanceByActivityId(String activityDefinitionId) {
    if (activityDefinitionId==null) {
      return null;
    }
    if (activityInstances!=null) {
      for (ActivityInstanceImpl activityInstance: activityInstances) {
        ActivityInstanceImpl theOne = activityInstance.findActivityInstanceByActivityId(activityDefinitionId);
        if (theOne!=null) {
          return theOne;
        }
      }
    }
    return null;
  }

  public ServiceRegistry getServiceRegistry() {
    return workflowEngine.getServiceRegistry();
  }

  // updates ////////////////////////////////////////////////////////////

  public boolean hasUpdates() {
    // As long as the workflow instance is not saved, the updates collection is null.
    // That means it's not yet necessary to collect the updates. 
    return updates!=null;
  }
  
  public ScopeInstanceUpdates getUpdates() {
    return updates;
  }
  
  public void trackUpdates(boolean isNew) {
    if (activityInstances!=null) {
      for (ActivityInstanceImpl activityInstance: activityInstances) {
        activityInstance.trackUpdates(isNew);
      }
    }
    if (variableInstances!=null) {
      for (VariableInstanceImpl variableInstance: variableInstances) {
        variableInstance.trackUpdates(isNew);
      }
    }
  }
  
  public void propagateActivityInstanceChange() {
    if (updates!=null) {
      updates.isActivityInstancesChanged = true;
      if (parent != null) {
        parent.propagateActivityInstanceChange();
      }
    }
  }

  public boolean hasActivityInstances() {
    return activityInstances!=null && !activityInstances.isEmpty();
  }
  public boolean isEnded() {
    return end!=null;
  }
  
  public boolean hasActivityInstance(String activityInstanceId) {
    if (hasActivityInstances()) {
      for (ActivityInstanceImpl activityInstance : activityInstances) {
        if (activityInstance.hasActivityInstance(activityInstanceId)) {
          return true;
        }
      }
    }
    return false;
  }
}