/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.trace;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * A class representing a metric class. It is used to represent a group of
 * metrics
 *
 * @author Yoann Heitz
 */
public class MetricClass {

    private final long fId;
    private final List<Long> fMetricMembers;

    /**
     * Constructor for this class
     *
     * @param event
     *            an event containing the definition of a metric class
     */
    public MetricClass(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Long selfId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_SELF);
        fId = selfId != null ? selfId : IOtf2Constants.OTF2_UNKNOWN_METRIC_CLASS;

        long[] metricMembers = content.getFieldValue(long[].class, IOtf2Fields.OTF2_METRIC_MEMBERS);
        fMetricMembers = Arrays.stream(metricMembers).boxed().collect(Collectors.toList());
    }

    /**
     * Gets the ID of this metric class
     *
     * @return the ID of this metric class
     */
    public long getId() {
        return fId;
    }

    /**
     * Gets the metric members of this metric class
     *
     * @return the metric members of this metric class
     */
    public List<Long> getMetricMembers() {
        return fMetricMembers;
    }
}
