/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import org.eclipse.tracecompass.common.core.NonNullUtils;

/**
 * Element representing a CPU Frequency state
 *
 * @author Raphaël Beamonte
 */
public class CpuFrequency extends InterruptionReason {
    private Long freq;

    /**
     * Constructor
     *
     * @param freq
     *            The frequency represented by this object
     */
    public CpuFrequency(Long freq) {
        this.freq = freq;
    }

    /**
     * @return The frequency represented by this object
     */
    public Long getFreq() {
        return freq;
    }

    @Override
    public String getID() {
        return freq.toString();
    }

    @Override
    public String getTaskId() {
        return ""; //$NON-NLS-1$
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof CpuFrequency)) {
            return false;
        }

        CpuFrequency b = (CpuFrequency) o;
        return getID().equals(b.getID());
    }

    @Override
    public int hashCode() {
        int hash = 317;

        String id = getID();
        hash = 97 * hash + (id != null ? id.hashCode() : 0);

        return hash;
    }

    @Override
    public String toString() {
        return getID();
    }

    @Override
    public int compareTo(InterruptionReason ir) {
        if (ir instanceof CpuFrequency) {
            CpuFrequency cps = (CpuFrequency) ir;
            if (freq == null) {
                if (cps.freq == null) {
                    return 0;
                }
                return -1;
            }
            return freq.compareTo(NonNullUtils.checkNotNull(cps.freq));
        }
        return 0;
    }
}