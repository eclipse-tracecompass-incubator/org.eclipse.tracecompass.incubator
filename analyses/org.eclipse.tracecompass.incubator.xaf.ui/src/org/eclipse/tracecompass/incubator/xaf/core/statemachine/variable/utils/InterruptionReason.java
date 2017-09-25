/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

/**
 * Abstract class to represent an interruption. Derived classes will be used
 * with their specificity to represent interruption reasons such as our task
 * entering in blocked status or being preempted.
 *
 * @author Raphaël Beamonte
 */
public abstract class InterruptionReason implements Comparable<InterruptionReason> {
    /**
     * @return The full ID of the interruption reason
     */
    public abstract String getID();

    /**
     * @return The short ID of the interruption reason
     */
    public String getShortID() {
        return getID();
    }

    /**
     * @return The task ID of the interruption reason
     */
    public abstract String getTaskId();

    /**
     * @param ir
     *            The interruption reason to use for comparison
     * @return Whether another interruption reason fits or not in this one
     */
    public boolean fits(InterruptionReason ir) {
        return equals(ir);
    }
}