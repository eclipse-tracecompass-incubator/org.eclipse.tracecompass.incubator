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

package org.eclipse.tracecompass.incubator.internal.ros2.core.trace;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * Class for ROS 2 experiments, which combine UST ({@link Ros2Trace}) and kernel
 * traces.
 *
 * @author Christophe Bedard
 */
public class Ros2Experiment extends TmfExperiment {

    /**
     * Default constructor. Needed by the extension point.
     */
    public Ros2Experiment() {
        this(StringUtils.EMPTY, Collections.emptySet());
    }

    /**
     * Constructor
     *
     * @param id
     *            the ID of this experiment
     * @param traces
     *            the set of traces that are part of this experiment
     */
    public Ros2Experiment(String id, Set<ITmfTrace> traces) {
        super(ITmfEvent.class, id, traces.toArray(new ITmfTrace[traces.size()]), TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, null);
    }
}
