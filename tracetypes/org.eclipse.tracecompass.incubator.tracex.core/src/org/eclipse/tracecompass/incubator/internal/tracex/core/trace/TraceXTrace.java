/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.core.trace;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.tracex.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

/**
 * TraceX trace type integration point.
 *
 * Parsing is intentionally deferred until the binary format implementation is
 * available. Consequently this trace type does not accept files yet.
 */
public class TraceXTrace extends TmfTrace {

    private static final String NOT_IMPLEMENTED = "TraceX parsing is not implemented yet"; //$NON-NLS-1$

    private final TmfLongLocation fLocation = new TmfLongLocation(0L);

    @Override
    public IStatus validate(@Nullable IProject project, @Nullable String path) {
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NOT_IMPLEMENTED);
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        return fLocation;
    }

    @Override
    public double getLocationRatio(@Nullable ITmfLocation location) {
        return 0.0;
    }

    @Override
    public ITmfContext seekEvent(@Nullable ITmfLocation location) {
        TmfContext context = new TmfContext();
        context.setLocation(fLocation);
        context.setRank(0);
        return context;
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        return seekEvent(fLocation);
    }

    @Override
    public @Nullable ITmfEvent parseEvent(@Nullable ITmfContext context) {
        return null;
    }
}
