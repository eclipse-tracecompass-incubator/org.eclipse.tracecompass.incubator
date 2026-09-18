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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.tracex.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

/**
 * TraceX trace type backed by a validated 32-bit binary layout.
 *
 * Event-record decoding is intentionally deferred. This class owns the input
 * resource opened with the trace and exposes only control-header properties.
 */
public class TraceXTrace extends TmfTrace implements ITmfPropertiesProvider {

    private static final int CONFIDENCE = 100;

    private @NonNull TmfLongLocation fLocation = new TmfLongLocation(0L);
    private @Nullable TraceXFile fInput;
    private @NonNull Map<@NonNull String, @NonNull String> fProperties = Collections.emptyMap();

    @Override
    public IStatus validate(@Nullable IProject project, @Nullable String path) {
        if (path == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No path supplied"); //$NON-NLS-1$
        }
        File file = new File(path);
        if (!file.isFile() || !file.canRead()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a readable TraceX file"); //$NON-NLS-1$
        }
        try (TraceXFile input = openTrace(file.toPath())) {
            return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
        } catch (TraceXFormatException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    "Not a TraceX trace: " + e.getMessage(), e); //$NON-NLS-1$
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    "I/O error validating TraceX trace", e); //$NON-NLS-1$
        }
    }

    @Override
    @NonNullByDefault({})
    public void initTrace(@Nullable IResource resource, @Nullable String path,
            Class<? extends ITmfEvent> type) throws TmfTraceException {
        if (path == null) {
            throw new TmfTraceException("No path supplied"); //$NON-NLS-1$
        }
        super.initTrace(resource, path, type);
        try {
            TraceXFile input = openTrace(new File(path).toPath());
            fInput = input;
            TraceXLayout layout = input.getLayout();
            fProperties = layout.getProperties();
            fLocation = new TmfLongLocation(layout.getEventStartOffset());
        } catch (IOException e) {
            throw new TmfTraceException("Failed to parse TraceX control header", e); //$NON-NLS-1$
        }
    }

    @Override
    public synchronized void dispose() {
        TraceXFile input = fInput;
        fInput = null;
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
                Activator.getInstance().logError("Failed to close TraceX input", e); //$NON-NLS-1$
            }
        }
        super.dispose();
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

    @Override
    public @NonNull Map<@NonNull String, @NonNull String> getProperties() {
        return fProperties;
    }

    /**
     * Open the owned input. Overridable to exercise I/O ownership paths.
     *
     * @param path
     *            input path
     * @return opened and validated input
     * @throws IOException
     *             if the input cannot be opened or validated
     */
    protected TraceXFile openTrace(Path path) throws IOException {
        return TraceXFile.open(path);
    }
}
