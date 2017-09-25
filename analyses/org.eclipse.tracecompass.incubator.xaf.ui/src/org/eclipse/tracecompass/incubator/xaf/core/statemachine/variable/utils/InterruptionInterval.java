/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.Direction;

/**
 * Class linking an interruption reason to an interval
 *
 * @author Raphaël Beamonte
 */
public class InterruptionInterval implements Comparable<InterruptionInterval> {
    InterruptionReason reason;
    double minDuration = Double.MAX_VALUE;
    double maxDuration = Double.MIN_VALUE;
    private List<TimestampInterval> tsIntervals = new LinkedList<>();
    private int counter = 0;

    /**
     * Constructor
     *
     * @param ir
     *            The interruption reason on which to build that interval
     */
    public InterruptionInterval(InterruptionReason ir) {
        this.reason = ir;
    }

    /**
     * Constructor
     *
     * @param id
     *            The interruption duration on which to build that interval
     */
    public InterruptionInterval(InterruptionDuration id) {
        this.reason = id.getReason();
        this.minDuration = id.fDuration;
        this.maxDuration = id.fDuration;
        this.tsIntervals.addAll(id.getTsIntervals());
        this.counter = 1;
    }

    /**
     * Constructor
     *
     * @param ii
     *            The interruption interval on which to build that interval
     */
    public InterruptionInterval(InterruptionInterval ii) {
        this.reason = ii.getReason();
        this.minDuration = ii.minDuration;
        this.maxDuration = ii.maxDuration;
        this.tsIntervals.addAll(ii.getTsIntervals());
        this.counter = ii.counter;
    }

    /**
     * @param id
     *            The duration to add to this interval
     */
    public void addDuration(InterruptionDuration id) {
        if (id.fDuration > maxDuration) {
            maxDuration = id.fDuration;
        }
        if (id.fDuration < minDuration) {
            minDuration = id.fDuration;
        }
        this.tsIntervals.addAll(id.getTsIntervals());
        counter++;
    }

    /**
     * @param ii
     *            The interval to add to this interval
     */
    public void addInterval(InterruptionInterval ii) {
        if (ii.maxDuration > maxDuration) {
            maxDuration = ii.maxDuration;
        }
        if (ii.minDuration < minDuration) {
            minDuration = ii.minDuration;
        }
        this.tsIntervals.addAll(ii.getTsIntervals());
        counter += ii.counter;
    }

    /**
     * @param id
     *            The interruption duration to verify
     * @return Whether or not the interruption duration fits in the interval
     */
    public boolean isMatching(InterruptionDuration id) {
        return reason.equals(id.reason) &&
                id.fDuration >= minDuration &&
                id.fDuration <= maxDuration;
    }

    /**
     * @param id
     *            The interruption duration to compute from
     * @return The distance between the interruption duration and this interval
     */
    public double distance(InterruptionDuration id) {
        if (id.fDuration >= minDuration
                && id.fDuration <= maxDuration) {
            return 0;
        }

        return Math.min(Math.abs(id.fDuration - maxDuration),
                Math.abs(id.fDuration - minDuration));
    }

    /**
     * @param id
     *            The interruption duration to compute from
     * @param direction
     *            The direction to use in order to compute the distance
     * @return The distance between the interruption duration and this interval
     */
    public double distance(InterruptionDuration id, Direction direction) {
        if (direction == Direction.VARIABLE_UNDER_VALUE) {
            return Math.max(id.fDuration - maxDuration, 0);
        } else if (direction == Direction.VARIABLE_OVER_VALUE) {
            return Math.max(minDuration - id.fDuration, 0);
        } else {
            return distance(id);
        }
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
     * @return the minDuration
     */
    public double getMinDuration() {
        return minDuration;
    }

    /**
     * @param minDuration
     *            the minDuration to set
     */
    public void setMinDuration(double minDuration) {
        this.minDuration = minDuration;
    }

    /**
     * @return the maxDuration
     */
    public double getMaxDuration() {
        return maxDuration;
    }

    /**
     * @param maxDuration
     *            the maxDuration to set
     */
    public void setMaxDuration(double maxDuration) {
        this.maxDuration = maxDuration;
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
     * @return A list of the timestamp intervals added in this interval
     */
    public List<TimestampInterval> getTsIntervals() {
        return tsIntervals;
    }

    /**
     * @return the number of durations added in this interval
     */
    public int getCounter() {
        return counter;
    }

    /**
     * @param counter
     *            the number of durations added in this interval
     */
    public void setCounter(int counter) {
        this.counter = counter;
    }

    @Override
    public String toString() {
        return String.format("%s, [%f, %f]", //$NON-NLS-1$
                reason.toString(),
                getMinDuration(),
                getMaxDuration());
    }

    /**
     * @param iiToSubstract
     *            The interruption interval to substract
     * @return The new interruption interval after substraction
     */
    public InterruptionInterval substract(InterruptionInterval iiToSubstract) {
        InterruptionInterval iiResult = new InterruptionInterval(this);

        iiResult.setMaxDuration(getMaxDuration() - iiToSubstract.getMaxDuration());
        iiResult.setMinDuration(getMinDuration() - iiToSubstract.getMinDuration());

        return iiResult;
    }

    @Override
    public int compareTo(InterruptionInterval ii) {
        int cmp = reason.compareTo(ii.reason);
        if (cmp == 0) {
            cmp = new Double(maxDuration - minDuration).compareTo(ii.maxDuration - ii.minDuration);
        }
        if (cmp == 0) {
            cmp = new Double(maxDuration).compareTo(ii.maxDuration);
        }
        if (cmp == 0) {
            cmp = new Double(minDuration).compareTo(ii.minDuration);
        }
        return cmp;
    }

    /**
     * Comparator used to compare intervals only using their reason
     *
     * @author Raphaël Beamonte
     */
    public static class IntervalToReasonComparator implements Comparator<InterruptionInterval> {
        @Override
        public int compare(InterruptionInterval ii1, InterruptionInterval ii2) {
            return ii1.getReason().compareTo(ii2.getReason());
        }
    }
}