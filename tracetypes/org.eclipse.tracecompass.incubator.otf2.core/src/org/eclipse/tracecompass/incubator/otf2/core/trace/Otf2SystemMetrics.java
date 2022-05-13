/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.otf2.core.trace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.MetricClass;
import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.MetricMember;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * A class that gets filled with the required informations to represent the
 * different counters that are defined in the definitions
 *
 * @author Yoann Heitz
 */
public class Otf2SystemMetrics {

    private Set<MetricClass> fMetricClasses = new HashSet<>();
    private Map<Long, MetricMember> fMetricMembers = new HashMap<>();

    /**
     * Adds a new metric member to the map containing the different metric
     * members
     *
     * @param event
     *            the event containing the definition of this new metricMember
     * @param stringIds
     *            the map associating the string IDs to their values for this
     *            trace
     */
    public void addMetricMember(ITmfEvent event, Map<Integer, String> stringIds) {
        MetricMember metricMember = new MetricMember(event, stringIds);
        fMetricMembers.put(metricMember.getId(), metricMember);
    }

    /**
     * Adds a new metric class to the set containing the different metric
     * classes
     *
     * @param event
     *            the event containing the definition of this new metric class
     */
    public void addMetricClass(ITmfEvent event) {
        MetricClass metricClass = new MetricClass(event);
        fMetricClasses.add(metricClass);
    }

    /**
     * Constructs the different counter aspects for this trace
     *
     * @return an iterator with the different counter aspects
     */
    public Iterator<Otf2CounterAspect> getCounterAspects() {
        Map<MetricMember, Otf2CounterAspect> counterAspects = new HashMap<>();
        for (MetricClass metricClass : fMetricClasses) {
            long metricClassId = metricClass.getId();
            long index = 0L;
            for (Iterator<Long> iterator = metricClass.getMetricMembers().iterator(); iterator.hasNext(); index++) {
                Long metricMemberId = iterator.next();
                MetricMember metricMember = fMetricMembers.get(metricMemberId);
                if (metricMember != null) {
                    Otf2CounterAspect counterAspect = counterAspects.get(metricMember);
                    if (counterAspect == null) {
                        counterAspect = new Otf2CounterAspect(metricMember);
                        counterAspects.put(metricMember, counterAspect);
                    }
                    counterAspect.addNewIds((new Pair<>(metricClassId, index)));
                }
            }
        }
        return Objects.requireNonNull(counterAspects.values().iterator());
    }
}
