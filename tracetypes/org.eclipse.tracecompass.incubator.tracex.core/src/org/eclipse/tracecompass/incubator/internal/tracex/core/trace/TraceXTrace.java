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
import java.util.Objects;
import java.util.OptionalLong;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.tracex.core.Activator;
import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceKnownSize;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;

/**
 * TraceX trace type backed by a validated 32-bit binary layout and immutable
 * logical event index.
 */
public class TraceXTrace extends TmfTrace implements ITmfPropertiesProvider,
        ITmfTraceKnownSize {

    private static final int CONFIDENCE = 100;
    private static final String PROPERTY_TICKS_PER_SECOND = "ticks_per_second"; //$NON-NLS-1$
    private static final String PROPERTY_TIMER_DIRECTION = "timer_direction"; //$NON-NLS-1$
    private static final QualifiedName PERSISTED_TICKS_PER_SECOND = new QualifiedName(
            Activator.PLUGIN_ID, PROPERTY_TICKS_PER_SECOND);
    private static final QualifiedName PERSISTED_TIMER_DIRECTION = new QualifiedName(
            Activator.PLUGIN_ID, PROPERTY_TIMER_DIRECTION);

    private @NonNull TmfLongLocation fLocation = new TmfLongLocation(0L);
    private final Object fClockConfigurationLock = new Object();
    private @Nullable TraceXFile fInput;
    private @Nullable TraceXEventIndex fEventIndex;
    private @NonNull Map<@NonNull String, @NonNull String> fProperties = Collections.emptyMap();
    private @NonNull TraceXTimerDirection fTimerDirection = TraceXTimerDirection.INCREASING;
    private @Nullable Long fTicksPerSecond;

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
        loadConfiguration(resource);
        super.initTrace(resource, path, type);
        TraceXFile input = null;
        try {
            input = openTrace(new File(path).toPath());
            TraceXEventIndex index = buildTimeBase(input, fTimerDirection,
                    fTicksPerSecond);
            fInput = input;
            installTimeBase(index);
        } catch (IOException | IllegalArgumentException e) {
            closeFailedInput(input, e);
            throw new TmfTraceException("Failed to initialize TraceX trace", e); //$NON-NLS-1$
        }
    }

    private void loadConfiguration(@Nullable IResource resource)
            throws TmfTraceException {
        fTimerDirection = TraceXTimerDirection.INCREASING;
        fTicksPerSecond = null;
        if (resource == null) {
            return;
        }
        try {
            String direction = resource.getPersistentProperty(
                    PERSISTED_TIMER_DIRECTION);
            if (direction != null) {
                fTimerDirection = TraceXTimerDirection.parse(direction);
            }
            String frequency = resource.getPersistentProperty(
                    PERSISTED_TICKS_PER_SECOND);
            if (frequency != null) {
                fTicksPerSecond = TraceXTimeNormalizer.parseTicksPerSecond(
                        frequency);
            }
        } catch (CoreException | IllegalArgumentException e) {
            throw new TmfTraceException("Invalid persisted TraceX clock configuration", e); //$NON-NLS-1$
        }
    }

    private TraceXEventIndex buildTimeBase(TraceXFile input,
            TraceXTimerDirection direction, @Nullable Long ticksPerSecond)
            throws IOException {
        TraceXEventIndex index = TraceXEventIndex.build(input, direction,
                this::isCancelled);
        if (ticksPerSecond != null && index.getEventCount() > 0) {
            TraceXTimeNormalizer.toNanoseconds(
                    index.getRelativeTicks(index.getEventCount() - 1),
                    ticksPerSecond.longValue());
        }
        return index;
    }

    private void installTimeBase(TraceXEventIndex index) {
        fEventIndex = index;
        fLocation = new TmfLongLocation(0L);
        setNbEvents(index.getEventCount());
        setStartTime(TmfTimestamp.fromNanos(0));
        long end = index.getEventCount() == 0 ? 0
                : timestamp(index.getRelativeTicks(index.getEventCount() - 1));
        setEndTime(TmfTimestamp.fromNanos(end));
        rebuildProperties(index);
    }

    private void rebuildProperties(TraceXEventIndex index) {
        TraceXFile input = fInput;
        if (input == null) {
            fProperties = Collections.emptyMap();
            return;
        }
        Map<@NonNull String, @NonNull String> properties = new LinkedHashMap<>(
                input.getLayout().getProperties());
        properties.put("event_count", Long.toString(index.getEventCount())); //$NON-NLS-1$
        properties.put("event_buffer_status", //$NON-NLS-1$
                index.isCircular() ? "circular-full" : "partial"); //$NON-NLS-1$ //$NON-NLS-2$
        try {
            TraceXRegistry registry = input.getRegistry();
            long valid = registry.getRecords().stream()
                    .filter(TraceXRegistryRecord::isValid).count();
            long inUse = registry.getRecords().stream()
                    .filter(TraceXRegistryRecord::isInUse).count();
            properties.put("registry_record_count", //$NON-NLS-1$
                    Long.toString(registry.getRecords().size()));
            properties.put("registry_valid_count", Long.toString(valid)); //$NON-NLS-1$
            properties.put("registry_in_use_count", Long.toString(inUse)); //$NON-NLS-1$
            properties.put("registry_diagnostic_count", //$NON-NLS-1$
                    Integer.toString(registry.getDiagnostics().size()));
        } catch (IOException e) {
            Activator.getInstance().logError("Failed to read TraceX registry properties", e); //$NON-NLS-1$
        }
        properties.put("event_order", "oldest-to-newest"); //$NON-NLS-1$ //$NON-NLS-2$
        properties.put(PROPERTY_TIMER_DIRECTION,
                fTimerDirection.getPropertyValue());
        properties.put("raw_time_status", "preserved-unsigned-32-bit"); //$NON-NLS-1$ //$NON-NLS-2$
        Long frequency = fTicksPerSecond;
        if (frequency == null) {
            properties.put(PROPERTY_TICKS_PER_SECOND, "unavailable"); //$NON-NLS-1$
            properties.put("time_unit", "ordering-ticks"); //$NON-NLS-1$ //$NON-NLS-2$
            properties.put("physical_time", "unavailable"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            properties.put(PROPERTY_TICKS_PER_SECOND,
                    Long.toString(frequency.longValue()));
            properties.put("time_unit", "nanoseconds"); //$NON-NLS-1$ //$NON-NLS-2$
            properties.put("physical_time", "available-rounded-down"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        fProperties = Collections.unmodifiableMap(properties);
    }

    private static void closeFailedInput(@Nullable TraceXFile input,
            Throwable failure) {
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
                TmfTimestamp.fromNanos(timestamp(
                        event.getRelativeTicks().orElseThrow())),
                new TmfEventType(event.getMetadata().getName(), null), content);
    }

    private long timestamp(long relativeTicks) {
        Long frequency = fTicksPerSecond;
        return frequency == null ? relativeTicks
                : TraceXTimeNormalizer.toNanoseconds(relativeTicks,
                        frequency.longValue());
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

    /** @return configured target clock frequency, or empty when unknown */
    public OptionalLong getTicksPerSecond() {
        Long frequency = fTicksPerSecond;
        return frequency == null ? OptionalLong.empty()
                : OptionalLong.of(frequency.longValue());
    }

    /** @return configured trace-wide target timer direction */
    public TraceXTimerDirection getTimerDirection() {
        return fTimerDirection;
    }

    /**
     * Get the immutable decoded registry used by execution analyses.
     *
     * @return decoded registry
     * @throws IOException
     *             if the trace was disposed or the registry cannot be read
     */
    public synchronized TraceXRegistry getRegistry() throws IOException {
        TraceXFile input = fInput;
        if (input == null) {
            throw new IOException("TraceX trace is not initialized"); //$NON-NLS-1$
        }
        return input.getRegistry();
    }

    /**
     * Set and persist the target clock frequency, then rebuild time-dependent
     * trace state.
     *
     * @param value
     *            positive decimal ticks per second
     * @throws TmfTraceException
     *             if validation, persistence, or rebuilding fails
     * @throws IllegalArgumentException
     *             if the value is not a supported positive decimal frequency
     */
    public void setTicksPerSecond(String value)
            throws TmfTraceException {
        long frequency = TraceXTimeNormalizer.parseTicksPerSecond(value);
        synchronized (fClockConfigurationLock) {
            synchronized (this) {
                if (Long.valueOf(frequency).equals(fTicksPerSecond)) {
                    return;
                }
                replaceClockConfiguration(fTimerDirection,
                        Long.valueOf(frequency), PERSISTED_TICKS_PER_SECOND,
                        Long.toString(frequency));
            }
            invalidateTimeDependentData();
        }
    }

    /**
     * Clear the target clock frequency and restore ordering-tick timestamps.
     *
     * @throws TmfTraceException
     *             if persistence or rebuilding fails
     */
    public void clearTicksPerSecond() throws TmfTraceException {
        synchronized (fClockConfigurationLock) {
            synchronized (this) {
                if (fTicksPerSecond == null) {
                    return;
                }
                replaceClockConfiguration(fTimerDirection, null,
                        PERSISTED_TICKS_PER_SECOND, null);
            }
            invalidateTimeDependentData();
        }
    }

    /**
     * Set and persist the documented target timer direction.
     *
     * @param direction
     *            trace-wide timer direction
     * @throws TmfTraceException
     *             if persistence or rebuilding fails
     */
    public void setTimerDirection(TraceXTimerDirection direction)
            throws TmfTraceException {
        Objects.requireNonNull(direction);
        synchronized (fClockConfigurationLock) {
            synchronized (this) {
                if (direction == fTimerDirection) {
                    return;
                }
                replaceClockConfiguration(direction, fTicksPerSecond,
                        PERSISTED_TIMER_DIRECTION,
                        direction.getPropertyValue());
            }
            invalidateTimeDependentData();
        }
    }

    private void replaceClockConfiguration(TraceXTimerDirection direction,
            @Nullable Long frequency, QualifiedName persistedName,
            @Nullable String persistedValue) throws TmfTraceException {
        TraceXFile input = fInput;
        if (input == null) {
            throw new TmfTraceException("TraceX trace is not initialized"); //$NON-NLS-1$
        }
        try {
            TraceXEventIndex replacement = buildTimeBase(input, direction,
                    frequency);
            IResource resource = getResource();
            if (resource != null) {
                resource.setPersistentProperty(persistedName, persistedValue);
            }
            fTimerDirection = direction;
            fTicksPerSecond = frequency;
            installTimeBase(replacement);
        } catch (IOException | CoreException | IllegalArgumentException e) {
            throw new TmfTraceException(
                    "Failed to apply TraceX clock configuration: " + e.getMessage(), e); //$NON-NLS-1$
        }
    }

    /** Reset all artifacts whose contents depend on the trace time base. */
    protected void invalidateTimeDependentData() {
        for (IAnalysisModule module : getAnalysisModules()) {
            module.cancel();
            module.waitForCompletion();
            module.clearPersistentData();
        }
        DataProviderManager.getInstance().removeDataProvider(this,
                TraceXDataProviderFactory.ID);
        TmfTraceManager.deleteSupplementaryFiles(this);
        resetIndexer();
        indexTrace(true);
        for (IAnalysisModule module : getAnalysisModules()) {
            if (module.isAutomatic()) {
                module.schedule();
            }
        }
        TmfSignalManager.dispatchSignal(new TmfTraceUpdatedSignal(this, this,
                new TmfTimeRange(getStartTime(), getEndTime()), getNbEvents()));
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
