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

import static org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields.OTF2_METRIC_VALUE;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.counters.core.aspects.CounterAspect;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.MetricMember;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * Aspect for OTF2 counters
 *
 * @author Yoann Heitz
 */
public class Otf2CounterAspect extends CounterAspect {

    private final Set<Pair<Long, Long>> fIds = new HashSet<>();
    private final MetricMember fMetricMember;

    /**
     * Constructor for this aspect
     *
     * @param metricMember
     *            the metric member describing the different properties of the
     *            metric that should be resolved by this aspect
     */
    public Otf2CounterAspect(MetricMember metricMember) {
        super(OTF2_METRIC_VALUE, metricMember.getName(), metricMember.getCounterType(), Otf2ClusterAspect.class, Otf2PhysicalNodeAspect.class, Otf2ProcessAspect.class, Otf2ThreadAspect.class);
        fMetricMember = metricMember;
    }

    /**
     * When encountering an event containing the value for a given metric, the
     * metric to which this event refers is identified with 2 IDs : the first
     * one is the ID of the metric class in which is the metric. The second ID
     * is the index of the metric in this metric class. The same metric may
     * appears in several metric classes so we must store the different pair of
     * IDs representing this metric
     *
     * @param ids
     *            a pair of IDs that references this metric
     */
    public void addNewIds(Pair<Long, Long> ids) {
        fIds.add(ids);
    }

    @Override
    public @Nullable Number resolve(@NonNull ITmfEvent event) {
        Long metricClassId = event.getContent().getFieldValue(Long.class, IOtf2Fields.OTF2_METRIC);
        Long index = event.getContent().getFieldValue(Long.class, IOtf2Fields.OTF2_INDEX);
        if (metricClassId == null || index == null || !fIds.contains(new Pair<>(metricClassId, index))) {
            return null;
        }
        Number counterValue;
        switch (fMetricMember.getCounterType()) {
        case DOUBLE:
            counterValue = event.getContent().getFieldValue(Double.class, OTF2_METRIC_VALUE);
            break;
        case LONG:
            counterValue = event.getContent().getFieldValue(Long.class, OTF2_METRIC_VALUE);
            break;
        default:
            counterValue = null;
            break;
        }
        if (counterValue == null) {
            return null;
        }
        return fMetricMember.computeValueWithDefinedUnit(counterValue);
    }

    @Override
    public String getHelpText() {
        return fMetricMember.getDescription();
    }

    @Override
    public String getName() {
        return fMetricMember.getName() + " (" + fMetricMember.getUnit() + ")"; //$NON-NLS-1$//$NON-NLS-2$
    }

    @Override
    public boolean isCumulative() {
        int modeCode = fMetricMember.getMode() & IOtf2Constants.METRIC_MODE_MASK;
        return (modeCode < IOtf2Constants.OTF2_METRIC_ABSOLUTE_POINT
                || modeCode > IOtf2Constants.OTF2_METRIC_ABSOLUTE_NEXT);
    }
}
