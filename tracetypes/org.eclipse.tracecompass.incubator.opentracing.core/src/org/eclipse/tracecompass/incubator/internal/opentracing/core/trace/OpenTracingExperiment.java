/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.trace;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * Experiment with open tracing traces and lttng traces
 *
 * @author Katherine Nadeau
 *
 */
public class OpenTracingExperiment extends TmfExperiment {

    /**
     * Constructor
     */
    @Deprecated
    public OpenTracingExperiment() {
        super();
    }

    /**
     * Constructor of an open tracing experiment
     *
     * @param type
     *            The event type
     * @param path
     *            The experiment path
     * @param traces
     *            The experiment set of traces
     * @param indexPageSize
     *            The experiment index page size. You can use
     *            {@link TmfExperiment#DEFAULT_INDEX_PAGE_SIZE} for a default value.
     * @param resource
     *            The resource associated to the experiment. You can use 'null' for
     *            no resources (tests, etc.)
     */
    public OpenTracingExperiment(final Class<? extends ITmfEvent> type,
            final String path,
            final ITmfTrace[] traces,
            final int indexPageSize,
            final @Nullable IResource resource) {
        super(type, path, traces, indexPageSize, resource);
    }
}
