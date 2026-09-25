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
import java.util.LinkedHashMap;
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
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceKnownSize;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

/**
 * TraceX trace type backed by a validated 32-bit binary layout and immutable
 * logical event index.
 */
public class TraceXTrace extends TmfTrace implements ITmfPropertiesProvider,
        ITmfTraceKnownSize {

    private static final int CONFIDENCE = 100;

    private @NonNull TmfLongLocation fLocation = new TmfLongLocation(0L);
    private @Nullable TraceXFile fInput;
    private @Nullable TraceXEventIndex fEventIndex;
    private @NonNull Map<@NonNull String, @NonNull String> fProperties = Collections.emptyMap();

    @Override
    public IStatus validate(@Nullable IProject project, @Nullable String path) {
        if (path == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No path supplied"); //$NON-NLS-1$
        }
        File file = new File(path);
        if (!file.isFile()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a TraceX file"); //$NON-NLS-1$
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
        TraceXFile input = null;
        try {
            input = openTrace(new File(path).toPath());
            TraceXEventIndex index = TraceXEventIndex.build(input,
                    this::isCancelled);
            TraceXLayout layout = input.getLayout();
            Map<@NonNull String, @NonNull String> properties = new LinkedHashMap<>(
                    layout.getProperties());
            properties.put("event_count", Long.toString(index.getEventCount())); //$NON-NLS-1$
            properties.put("event_order", "oldest-to-newest"); //$NON-NLS-1$ //$NON-NLS-2$
            fInput = input;
            fEventIndex = index;
            fProperties = Collections.unmodifiableMap(properties);
            fLocation = new TmfLongLocation(0L);
            setNbEvents(index.getEventCount());
        } catch (IOException e) {
            closeFailedInput(input, e);
            throw new TmfTraceException("Failed to initialize TraceX trace", e); //$NON-NLS-1$
        }
    }

    private static void closeFailedInput(@Nullable TraceXFile input,
            IOException failure) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
        }
    }

    @Override
    public synchronized void dispose() {
        TraceXFile input = fInput;
        fInput = null;
        fEventIndex = null;
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
        TraceXEventIndex index = fEventIndex;
        if (index == null || index.getEventCount() == 0) {
            return 0.0;
        }
        long rank = locationRank(location, 0);
        return (double) index.clampRank(rank) / index.getEventCount();
    }

    @Override
    public ITmfContext seekEvent(@Nullable ITmfLocation location) {
        TraceXEventIndex index = fEventIndex;
        long rank = index == null ? 0
                : index.clampRank(locationRank(location, 0));
        TmfContext context = new TmfContext();
        context.setLocation(new TmfLongLocation(rank));
        context.setRank(rank);
        return context;
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        TraceXEventIndex index = fEventIndex;
        long rank = index == null ? 0 : index.rankForRatio(ratio);
        return seekEvent(new TmfLongLocation(rank));
    }

    @Override
    public @Nullable ITmfEvent parseEvent(@Nullable ITmfContext context) {
        TraceXFile input = fInput;
        TraceXEventIndex index = fEventIndex;
        if (context == null || input == null || index == null || isCancelled()) {
            return null;
        }
        long rank = locationRank(context.getLocation(), context.getRank());
        if (rank < 0 || rank >= index.getEventCount()) {
            fLocation = new TmfLongLocation(index.getEventCount());
            return null;
        }
        try {
            TraceXEvent event = index.readEvent(input, rank);
            fLocation = new TmfLongLocation(rank + 1);
            return toTmfEvent(event, rank);
        } catch (IOException e) {
            Activator.getInstance().logError(
                    "Failed to read TraceX event at logical rank " + rank, e); //$NON-NLS-1$
            return null;
        }
    }

    private static long locationRank(@Nullable ITmfLocation location,
            long defaultRank) {
        if (location instanceof TmfLongLocation) {
            Long value = ((TmfLongLocation) location).getLocationInfo();
            if (value != null) {
                return value.longValue();
            }
        }
        return defaultRank;
    }

    private ITmfEvent toTmfEvent(TraceXEvent event, long rank) {
        Map<String, Object> fields = event.getFields();
        TmfEventField[] children = new TmfEventField[fields.size()];
        int position = 0;
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            children[position++] = new TmfEventField(field.getKey(),
                    field.getValue(), null);
        }
        TmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID,
                event, children);
        return new TmfEvent(this, rank,
                TmfTimestamp.fromNanos(event.getRawTimestamp()),
                new TmfEventType(event.getMetadata().getName(), null), content);
    }

    @Override
    public synchronized @Nullable ITmfEvent getNext(
            @Nullable ITmfContext context) {
        if (context == null || isCancelled()) {
            return null;
        }
        return super.getNext(context);
    }

    @Override
    public int size() {
        TraceXEventIndex index = fEventIndex;
        return index == null ? 0 : (int) index.getEventCount();
    }

    @Override
    public int progress() {
        TraceXEventIndex index = fEventIndex;
        if (index == null) {
            return 0;
        }
        Long rank = fLocation.getLocationInfo();
        return (int) index.clampRank(rank == null ? 0 : rank.longValue());
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

    /** @return whether initialization or iteration should stop */
    protected boolean isCancelled() {
        return Thread.currentThread().isInterrupted();
    }
}
