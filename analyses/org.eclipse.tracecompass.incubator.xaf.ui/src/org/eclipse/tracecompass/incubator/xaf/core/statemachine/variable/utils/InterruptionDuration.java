/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;

/**
 * Class linking an interruption reason to a duration
 *
 * @author Raphaël Beamonte
 */
public class InterruptionDuration implements Comparable<Object> {
    InterruptionReason reason;
    double fDuration;
    private List<TimestampInterval> tsIntervals = new LinkedList<>();

    /**
     * @param reason
     *            The interruption reason
     * @param duration
     *            The duration
     */
    public InterruptionDuration(InterruptionReason reason, double duration) {
        this.setReason(reason);
        this.setDuration(duration);
    }

    /**
     * @param reason
     *            The interruption reason
     * @param duration
     *            The duration
     * @param ti
     *            The timestamp interval on which that reason was active
     */
    public InterruptionDuration(InterruptionReason reason, double duration, TimestampInterval ti) {
        this.setReason(reason);
        this.setDuration(duration);
        tsIntervals.add(ti);
    }

    /**
     * @param reason
     *            The interruption reason
     * @param duration
     *            The duration
     * @param tiList
     *            The list of timestamp intervals on which that reason was
     *            active
     */
    public InterruptionDuration(InterruptionReason reason, double duration, List<TimestampInterval> tiList) {
        this.setReason(reason);
        this.setDuration(duration);
        tsIntervals.addAll(tiList);
    }

    /**
     * @return the reason
     */
    public InterruptionReason getReason() {
        return reason;
    }

    /**
     * @param reason
     *            the reason to set
     */
    public void setReason(InterruptionReason reason) {
        this.reason = reason;
    }

    /**
     * @return the duration
     */
    public double getDuration() {
        return fDuration;
    }

    /**
     * @param duration
     *            the duration to set
     */
    public void setDuration(double duration) {
        this.fDuration = duration;
    }

    /**
     * @param duration
     *            the duration to add
     */
    public void addDuration(double duration) {
        this.fDuration += duration;
    }

    /**
     * @param ti
     *            the timestamp interval to add
     */
    public void addTsInterval(TimestampInterval ti) {
        tsIntervals.add(ti);
    }

    /**
     * @param tiList
     *            the list of timestamp intervals to add
     */
    public void addTsInterval(List<TimestampInterval> tiList) {
        tsIntervals.addAll(tiList);
    }

    /**
     * @return The list of timestamp intervals
     */
    public List<TimestampInterval> getTsIntervals() {
        return tsIntervals;
    }

    @Override
    public String toString() {
        return reason.toString() + ", " + getDuration(); //$NON-NLS-1$
    }

    @Override
    public int hashCode() {
        final int prime = 61;
        int result = 1;
        result = prime * result + ((reason == null) ? 0 : reason.hashCode());
        result = prime * result + (Double.valueOf(fDuration).hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        InterruptionDuration other = (InterruptionDuration) obj;
        if (!reason.equals(other.reason) || fDuration != other.fDuration) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Object arg0) {
        if (arg0 instanceof InterruptionDuration) {
            double d = ((InterruptionDuration) arg0).getDuration();
            if (fDuration > d) {
                return 1;
            } else if (fDuration < d) {
                return -1;
            }
        }
        return 0;
    }

    /**
     * Accumulate a list of interruption durations into a list of interruption
     * durations for which the reasons are merged when possible.
     *
     * @param idList
     *            List of interruption durations to accumulate
     * @return An accumulated list of interruption duration
     */
    public static List<InterruptionDuration> accumulateList(final List<InterruptionDuration> idList) {
        List<InterruptionDuration> idListOutput = new ArrayList<>();

        received: for (InterruptionDuration id : idList) {
            for (InterruptionDuration idOutput : idListOutput) {
                if (idOutput.getReason().fits(id.getReason())) {
                    idOutput.addDuration(id.getDuration());
                    idOutput.addTsInterval(id.getTsIntervals());
                    continue received;
                }
            }
            idListOutput.add(new InterruptionDuration(id.getReason(), id.getDuration(), id.getTsIntervals()));
        }

        return idListOutput;
    }
}