/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.Direction;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Set of interruption duration objects
 *
 * @author Raphaël Beamonte
 */
public class InterruptionDurationSet implements Cloneable {
    Multimap<InterruptionReason, InterruptionDuration> map = ArrayListMultimap.create();

    /**
     * Constructor
     */
    public InterruptionDurationSet() {
    }

    /**
     * Constructor
     *
     * @param idList
     *            The list of interruption durations to add in the set
     */
    public InterruptionDurationSet(final List<InterruptionDuration> idList) {
        for (InterruptionDuration id : idList) {
            add(id);
        }
    }

    /**
     * Add an interruption duration to the set
     *
     * @param id
     *            The interruption duration to add in the set
     */
    public void add(InterruptionDuration id) {
        map.put(id.reason, id);
    }

    /*
     * public InterruptionReasonSet getReasonSet() { InterruptionReasonSet irs =
     * new InterruptionReasonSet();
     *
     * for (Entry<InterruptionReason, Collection<InterruptionDuration>> entry :
     * map.asMap().entrySet()) { irs.add(entry.getKey(),
     * entry.getValue().size()); }
     *
     * return irs; }
     */

    /**
     * @return The set of interruption reason that have at least one
     *         interruption duration in this interruption duration set
     */
    public @NonNull Set<InterruptionReason> getReasonSet() {
        return map.keySet();
    }

    @Override
    public InterruptionDurationSet clone() {
        InterruptionDurationSet iis = new InterruptionDurationSet();
        iis.map.putAll(map);

        return iis;
    }

    /**
     * @return The set of entries linking an interruption reason to the
     *         collection of interruption duration matching that interruption
     *         reason
     */
    public Set<Entry<InterruptionReason, Collection<InterruptionDuration>>> entrySetList() {
        return map.asMap().entrySet();
    }

    /**
     * @return The size of the interruption duration set
     */
    public int size() {
        return map.size();
    }

    /**
     * @return The total duration of the interruption duration set
     */
    public double getDuration() {
        double duration = 0;
        for (InterruptionDuration id : map.values()) {
            duration += id.getDuration();
        }
        return duration;
    }

    /**
     * @return The collection of interruption durations of the interruption
     *         duration set
     */
    public Collection<InterruptionDuration> getDurations() {
        return map.values();
    }

    @Override
    public String toString() {
        String str = "DurationSet: [\n"; //$NON-NLS-1$
        for (Entry<InterruptionReason, Collection<InterruptionDuration>> entry : map.asMap().entrySet()) {
            double sumTime = 0;
            for (InterruptionDuration id : entry.getValue()) {
                sumTime += id.getDuration();
            }

            str += String.format("\t[%s, %d, %f]\n", //$NON-NLS-1$
                    entry.getKey(),
                    entry.getValue().size(),
                    sumTime);
        }
        str += "]"; //$NON-NLS-1$
        return str;
    }

    /**
     * @return The keymap of the interruption duration set
     */
    public Map<InterruptionReason, Integer> keyMap() {
        return StateMachineVariableHelpers.keyMap(map);
    }

    /**
     * Substract an interruption duration set from this interruption duration
     * set
     *
     * @param idsToSubstract
     *            The interruption duration set to substract from this one
     * @param direction
     *            The direction of the substraction
     * @return The interruption interval set of difference
     */
    public InterruptionIntervalSetDiff substract(InterruptionDurationSet idsToSubstract, Direction direction) {
        InterruptionIntervalSet iisLocal = new InterruptionIntervalSet(direction);
        iisLocal.addDurationSet(this);

        InterruptionIntervalSet iisToSubstract = new InterruptionIntervalSet(direction);
        iisToSubstract.addDurationSet(idsToSubstract);

        return iisLocal.substract(iisToSubstract);
    }

    /**
     * Substract an interruption duration set from this interruption duration
     * set
     *
     * @param idsToSubstract
     *            The interruption duration set to substract from this one
     * @return The interruption interval set of difference
     */
    public InterruptionIntervalSetDiff substract(InterruptionDurationSet idsToSubstract) {
        InterruptionIntervalSet iisLocal = new InterruptionIntervalSet();
        iisLocal.addDurationSet(this);

        InterruptionIntervalSet iisToSubstract = new InterruptionIntervalSet();
        iisToSubstract.addDurationSet(idsToSubstract);

        return iisLocal.substract(iisToSubstract);
    }

    /**
     * Substract an interruption interval set from this interruption duration
     * set
     *
     * @param iisToSubstract
     *            The interruption interval set to substract from this one
     * @param direction
     *            The direction of the substraction
     * @return The interruption interval set of difference
     */
    public InterruptionIntervalSetDiff substract(InterruptionIntervalSet iisToSubstract, Direction direction) {
        InterruptionIntervalSet iisLocal = new InterruptionIntervalSet(direction);
        iisLocal.addDurationSet(this);

        return iisLocal.substract(iisToSubstract);
    }

    /**
     * Substract an interruption interval set from this interruption duration
     * set
     *
     * @param iisToSubstract
     *            The interruption interval set to substract from this one
     * @return The interruption interval set of difference
     */
    public InterruptionIntervalSetDiff substract(InterruptionIntervalSet iisToSubstract) {
        InterruptionIntervalSet iisLocal = new InterruptionIntervalSet(iisToSubstract.getDirection());
        iisLocal.addDurationSet(this);

        return iisLocal.substract(iisToSubstract);
    }
}