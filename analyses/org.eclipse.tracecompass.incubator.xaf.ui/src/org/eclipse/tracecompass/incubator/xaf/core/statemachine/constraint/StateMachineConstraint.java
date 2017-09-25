/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint;

import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineVariableException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Representation of a constraint in our StateMachine:
 * We match a variable with a value, using an operator
 *
 * @author Raphaël Beamonte
 */
public class StateMachineConstraint {

    /** The variable name */
    String varName;
    /** The operator */
    Operator operator;
    /** The value type */
    ValueType valueType;
    /** The value */
    String value;

    /**
     * Create a new state machine constraint
     * @param varName The name of the variable on which that constraint applies
     * @param operator The operator used in that constraint
     * @param valueType The type of value the variable is compared to
     * @param value The value the variable is compared to
     */
    public StateMachineConstraint(String varName, Operator operator, ValueType valueType, String value) {
        this.varName = varName;
        this.operator = operator;
        this.valueType = valueType;
        this.value = value;
    }

    /**
     * @return the variable name
     */
    public String getVarName() {
        return varName;
    }

    /**
     * @return Whether the constraint can be used or not
     */
    public boolean canBeUsed() {
        return true;
    }

    /**
     * @return the variable class
     */
    public Class<?> getVarClass() {
        String varType = getVarName().split("/", 2)[0]; //$NON-NLS-1$
        return NonNullUtils.checkNotNull(StateMachineVariable.VARIABLE_TYPES.get(varType));
    }

    /**
     * @param varName the variable name
     */
    public void setVarName(String varName) {
        this.varName = varName;
    }

    /**
     * @return the operation
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * @param operator the operation
     */
    public void setOperation(Operator operator) {
        this.operator = operator;
    }

    /**
     * @return the value type
     */
    public ValueType getValueType() {
        return valueType;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param valueType the value type
     * @param value the value
     */
    public void setValue(ValueType valueType, String value) {
        this.valueType = valueType;
        this.value = value;
    }

    /**
     * @return
     *          The direction of the current constraint. The direction will be 'true'
     *          if the constraint is to have the variable greater than a value, 'false'
     *          if the constraint is to have the variable less than a value, and 'null'
     *          else.
     */
    public Direction getDirection() {
        return getOperator().getDirection();
    }

    /**
     * Verify the constraint's status
     * @param variables List of current variables in the instance to valid the constraint
     * @param event The event we received, as its information could be useful to verify the constraint
     * @param stateMachineInstance the state machine instance
     * @return The status of the constraint, whether it is valid, uncertain or invalid
     */
    public Status verify(Map<String, StateMachineVariable> variables, ITmfEvent event, StateMachineInstance stateMachineInstance) {
        Status cs = new Status(this);

        StateMachineVariable v = variables.get(getVarName());

        if (v == null) {
            cs.setUncertain();
        } else {
            Comparable<?> initValue = null;
            Comparable<?> val = getValue();
            if (getValueType() == ValueType.VARIABLE) {
                try {
                    StateMachineVariable var = variables.get(val);
                    if (var != null) {
                        val = var.getValue(event, stateMachineInstance);
                        initValue = var.getValue();
                    } else {
                        cs.setUncertain();
                    }
                } catch (StateMachineVariableException e) {
                    cs.setUncertain();
                }
            }

            if (cs.getStatus() == null) {
                Integer comparison;
                try {
                    comparison = v.compareTo(val, event, stateMachineInstance);
                    String actualLeftValue = v.getFormattedValue(val, event, stateMachineInstance);

                    if (comparison != null && !getOperator().isSatisfied(comparison)) {
                        cs.setInvalid(actualLeftValue, null, initValue); //TODO: get right value when variable
                    } else {
                        cs.setValid(actualLeftValue, null, initValue); //TODO: get right value when variable
                    }
                } catch (StateMachineVariableException e) {
                    cs.setUncertain();
                }
            }
        }

        return cs;
    }

    /**
     * Analyze the reason why the invalid instances are invalid on a given interval
     * @param startNode The number of the node at the start of the interval
     * @param endNode The number of the node at the end of the interval
     * @param valid The valid instances
     * @param invalid The invalid instances
     * @param uncertain The uncertain instances
     */
    public void analyze(int startNode, int endNode, List<StateMachineInstance> valid, List<StateMachineInstance> invalid, List<StateMachineInstance> uncertain) {
        class MyBuilder {
            public String build(List<StateMachineInstance> list) {
                StringBuilder sb = new StringBuilder();
                for (StateMachineInstance inst : list) {
                    if (sb.length() != 0) {
                        sb.append(", "); //$NON-NLS-1$
                    }
                    sb.append(inst.getVTid());
                }
                return sb.toString();
            }
        }
        MyBuilder b = new MyBuilder();
        String validstr = b.build(valid);
        String invalidstr = b.build(invalid);
        String uncertainstr = b.build(uncertain);

        System.out.printf("CONSTRAINT ANALYZE / INTVL: %d-%d / VALID: %d (%s) / INVALID: %d (%s) / UNCERTAIN: %d (%s)\n", //$NON-NLS-1$
                startNode,
                endNode,
                valid.size(),
                validstr,
                invalid.size(),
                invalidstr,
                uncertain.size(),
                uncertainstr
                );

        // We need to simplify the instances.
    }

    @Override
    public String toString() {
        StringBuilder path = new StringBuilder();

        path.append(getVarName());
        path.append(" "); //$NON-NLS-1$
        path.append(getOperator().toString());
        path.append(" "); //$NON-NLS-1$
        switch (getValueType()) {
        case VARIABLE:
            path.append("(VARIABLE)"); //$NON-NLS-1$
            break;
        case CONSTANT:
        default:
            break;
        }
        path.append(getValue());

        return path.toString();
    }

    /**
     * Allows to return the worst case between two status given as parameters
     * @param s1 The first {@link StatusValue}
     * @param s2 The second {@link StatusValue}
     * @return The worst case between the two {@link StatusValue}
     */
    static public StatusValue worstStatus(StatusValue s1, StatusValue s2) {
        StatusValue worst = StatusValue.VALID;
        if (s1 == StatusValue.INVALID || s2 == StatusValue.INVALID) {
            worst = StatusValue.INVALID;
        } else if (s1 == StatusValue.UNCERTAIN || s2 == StatusValue.UNCERTAIN) {
            worst =  StatusValue.UNCERTAIN;
        }
        return worst;
    }
}
