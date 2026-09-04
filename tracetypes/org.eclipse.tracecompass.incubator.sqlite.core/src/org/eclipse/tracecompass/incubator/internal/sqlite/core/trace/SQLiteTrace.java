/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.sqlite.core.trace;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.sqlite.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

/**
 * SQLite trace.
 */
public class SQLiteTrace extends TmfTrace {

    private static final int CONFIDENCE = 10;

    @Override
    public IStatus validate(IProject project, String path) {
        File file = new File(path);
        // TODO: read a small header/magic to confirm this is a SQLite trace.
        // Return a low/zero confidence Status.ERROR when it is not.
        if (!file.exists()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "File does not exist: " + path); //$NON-NLS-1$
        }
        return new TraceValidationStatus(CONFIDENCE, Activator.class.getCanonicalName());
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        // TODO: open the trace file(s) and set up parser state.
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        // TODO: return the current read position.
        return new TmfLongLocation(0L);
    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        // TODO: return progress in [0, 1] for the given location.
        return 0.0;
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {
        // TODO: create/position a context at the given location.
        return new TmfContext(location != null ? location : new TmfLongLocation(0L));
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        // TODO: create/position a context at the given ratio.
        return new TmfContext(new TmfLongLocation(0L));
    }

    @Override
    public @Nullable ITmfEvent parseEvent(ITmfContext context) {
        // TODO: parse and return the event at the context, or null at end.
        return null;
    }
}
