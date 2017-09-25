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
 * The operator to apply between the variable
 * and the value
 * @author Raphaël Beamonte
 */
public enum Operator {
    /** Verify if varName.value == value */
    EQ("=="), //$NON-NLS-1$
    /** Verify if varName.value != value */
    NEQ("!="), //$NON-NLS-1$
    /** Verify if varName.value <= value */
    LEQ("<="), //$NON-NLS-1$
    /** Verify if varName.value < value */
    LT("<"), //$NON-NLS-1$
    /** Verify if varName.value >= value */
    GEQ(">="), //$NON-NLS-1$
    /** Verify if varName.value > value */
    GT(">"), //$NON-NLS-1$
    /** For unknown operators in adaptive mode */
    ADAPTIVE("??"), //$NON-NLS-1$
    ;

    private final String text;

    /**
     * Constructor to allow an enum entry to have a String equivalent
     * @param text The text version of the enum entry
     */
    private Operator(final String text) {
        this.text = text;
    }

    /**
     * @param compareTo The value to which we need to compare, which should be the result of a call to a compareTo method
     * @return Whether or not the constraint is satisfied
     */
    public boolean isSatisfied(int compareTo) {
        boolean ret = false;

        switch (this) {
        case EQ:
            ret = (compareTo == 0);
            break;
        case GEQ:
            ret = (compareTo >= 0);
            break;
        case GT:
            ret = (compareTo > 0);
            break;
        case LEQ:
            ret = (compareTo <= 0);
            break;
        case LT:
            ret = (compareTo < 0);
            break;
        case NEQ:
            ret = (compareTo != 0);
            break;
        case ADAPTIVE:
        default:
            break;
        }

        return ret;
    }

    /**
     * @return
     *          The direction of the current operator. The direction will be 'true'
     *          if the constraint is to have the variable greater than a value, 'false'
     *          if the constraint is to have the variable less than a value, and 'null'
     *          else.
     */
    public Direction getDirection() {
        Direction direction;

        switch (this) {
        case GEQ:
        case GT:
            direction = Direction.VARIABLE_OVER_VALUE;
            break;
        case LEQ:
        case LT:
            direction = Direction.VARIABLE_UNDER_VALUE;
            break;
        case EQ:
        case NEQ:
            direction = Direction.VARIABLE_EQ_VALUE;
            break;
        case ADAPTIVE:
        default:
            direction = null;
            break;
        }

        return direction;
    }

    /**
     * @return Whether or not the operator contains equal
     */
    public boolean hasEqual() {
        boolean ret;

        switch (this) {
        case EQ:
        case GEQ:
        case LEQ:
            ret = true;
            break;
        case GT:
        case LT:
        case NEQ:
        case ADAPTIVE:
        default:
            ret = false;
            break;
        }

        return ret;
    }

    /**
     * @return Whether or not the operator is adaptive
     */
    public boolean isAdaptive() {
        return this == ADAPTIVE;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return text;
    }

    /**
     * Returns an enum entry that has a given text value
     * @param text The text we are searching for
     * @return The enum entry that corresponds, or null if not found
     */
    public static Operator getOperatorFromText(String text) {
        for (Operator o : Operator.values()) {
            if (o.toString().equals(text)) {
                return o;
            }
        }
        return null;
    }
}