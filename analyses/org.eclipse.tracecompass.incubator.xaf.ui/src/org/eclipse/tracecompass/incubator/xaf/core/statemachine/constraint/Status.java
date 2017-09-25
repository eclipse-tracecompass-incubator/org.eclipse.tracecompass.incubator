/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint;

/**
 * Constraint status information
 * @author Raphaël Beamonte
 */
public class Status {
    private StateMachineConstraint constraint = null;

    private StatusValue statusValue = null;

    private Comparable<?> initValue = null;

    private Double adaptiveValue = null;
    private Object actualLeftValue = null;
    private Object actualRightValue = null;

    /**
     * @param constraint The constraint for which we construct a ConstraintStatus object
     */
    public Status(StateMachineConstraint constraint) {
        this.constraint = constraint;
    }

    /**
     * @return The constraint aimed by this status
     */
    public StateMachineConstraint getConstraint() {
        return constraint;
    }

    /**
     * @param statusValue The status of the constraint
     * @param leftValue The left value
     * @param rightValue The right value
     * @param initValue The initial value
     */
    public void set(StatusValue statusValue, Object leftValue, Object rightValue, Comparable<?> initValue) {
        this.statusValue = statusValue;
        actualLeftValue = leftValue;
        actualRightValue = rightValue;
        this.initValue = initValue;
    }

    /**
     * Set the constraint status as VALID
     * @param leftValue The left value
     * @param rightValue The right value
     * @param initValue The initial value
     */
    public void setValid(Object leftValue, Object rightValue, Comparable<?> initValue) {
        statusValue = StatusValue.VALID;

        actualLeftValue = leftValue;
        actualRightValue = rightValue;
        this.initValue = initValue;
    }

    /**
     * Set the constraint status as UNCERTAIN
     */
    public void setUncertain() {
        statusValue = StatusValue.UNCERTAIN;

        actualLeftValue = null;
        actualRightValue = null;
    }

    /**
     * Set the constraint status as ADAPTIVE
     * @param adaptiveValue The adaptive value
     * @param leftValue The left value
     */
    public void setAdaptive(Double adaptiveValue, Object leftValue) {
        statusValue = StatusValue.ADAPTIVE;

        this.adaptiveValue = adaptiveValue;
        actualLeftValue = leftValue;
        actualRightValue = null;
    }

    /**
     * Set the constraint status as INVALID
     * @param leftValue The left value
     * @param rightValue The right value
     * @param initValue The initial value
     */
    public void setInvalid(Object leftValue, Object rightValue, Comparable<?> initValue) {
        statusValue = StatusValue.INVALID;

        actualLeftValue = leftValue;
        actualRightValue = rightValue;
        this.initValue = initValue;
    }

    /**
     * @return the status
     */
    public StatusValue getStatus() {
        return statusValue;
    }

    /**
     * @return the initial value
     */
    public Comparable<?> getInitValue() {
        return initValue;
    }

    /**
     * @return the constraint left operand
     */
    public String getConstraintLeftOperand() {
        return constraint.getVarName();
    }

    /**
     * @return the constraint right operand
     */
    public String getConstraintRightOperand() {
        return constraint.getValue();
    }

    /**
     * @return the constraint operator
     */
    public Operator getConstraintOperator() {
        return constraint.getOperator();
    }

    /**
     * @return the adaptive value
     */
    public Double getAdaptiveValue() {
        return adaptiveValue;
    }

    /**
     * @return the actual left value
     */
    public Object getActualLeftValue() {
        return actualLeftValue;
    }

    /**
     * @return the actual right value
     */
    public Object getActualRightValue() {
        return actualRightValue;
    }

    @Override
    public String toString() {
        return String.format("Constraint: %s\n  - Status: %s\n  - Actual left value: %s\n  - Actual right value: %s", //$NON-NLS-1$
                constraint.toString(),
                statusValue.name(),
                actualLeftValue.toString(),
                actualRightValue.toString());

    }
}
