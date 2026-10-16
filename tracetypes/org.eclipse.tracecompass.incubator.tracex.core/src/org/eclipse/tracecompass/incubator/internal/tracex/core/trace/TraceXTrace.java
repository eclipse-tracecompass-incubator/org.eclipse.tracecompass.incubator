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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.tracex.core.Activator;
import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXAnalysis;
import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
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
        ITmfTraceKnownSize, IKernelTrace {

    private static final int CONFIDENCE = 100;
    private static final String PROPERTY_TICKS_PER_SECOND = "ticks_per_second"; //$NON-NLS-1$
    private static final String PROPERTY_TIMER_DIRECTION = "timer_direction"; //$NON-NLS-1$
    private static final QualifiedName PERSISTED_CLOCK = new QualifiedName(
            Activator.PLUGIN_ID, "clock_configuration"); //$NON-NLS-1$
    private static final QualifiedName PERSISTED_TICKS_PER_SECOND = new QualifiedName(
            Activator.PLUGIN_ID, PROPERTY_TICKS_PER_SECOND);
    private static final QualifiedName PERSISTED_TIMER_DIRECTION = new QualifiedName(
            Activator.PLUGIN_ID, PROPERTY_TIMER_DIRECTION);

    /** TID reserved for the idle context (ThreadX null current thread). */
    public static final int IDLE_TID = 0;

    private @NonNull TmfLongLocation fLocation = new TmfLongLocation(0L);
    private final Object fClockConfigurationLock = new Object();
    private @Nullable TraceXFile fInput;
    private @Nullable TraceXEventIndex fEventIndex;
    /**
     * Stable mapping from a ThreadX thread object address to a small, dense
     * thread id usable as a Linux-style TID in the kernel state system and the
     * {@link org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect
     * LinuxTidAspect}. Built from the registry so it is deterministic.
     */
    private @NonNull Map<@NonNull Long, @NonNull Integer> fThreadIds = Collections.emptyMap();
    private @NonNull Map<@NonNull Integer, @NonNull String> fThreadNames = Collections.emptyMap();
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
            buildThreadIds(input);
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
            String clock = resource.getPersistentProperty(PERSISTED_CLOCK);
            if (clock != null) {
                String[] values = clock.split(":", -1); //$NON-NLS-1$
                if (values.length != 2) {
                    throw new IllegalArgumentException("Invalid TraceX clock configuration"); //$NON-NLS-1$
                }
                fTimerDirection = TraceXTimerDirection.parse(values[0]);
                fTicksPerSecond = values[1].isEmpty() ? null
                        : TraceXTimeNormalizer.parseTicksPerSecond(values[1]);
                return;
            }
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

    /**
     * TraceX scheduling event ids (ThreadX trace event ids). These are the
     * events {@link org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXKernelStateProvider
     * TraceXKernelStateProvider} treats as context switches; the "next thread"
     * is carried in a different information word per event.
     */
    private static final int EVT_THREAD_RESUME = 1;
    private static final int EVT_THREAD_SUSPEND = 2;
    private static final int EVT_TIME_SLICE = 5;
    private static final int EVT_RUNNING = 6;
    private static final int EVT_THREAD_RELINQUISH = 109;

    private ITmfEvent toTmfEvent(TraceXEvent event, long rank) {
        Map<String, Object> fields = event.getFields();
        boolean schedSwitch = isSchedulingEvent(event);
        TmfEventField[] children;
        if (schedSwitch) {
            children = buildSchedSwitchFields(event, fields);
        } else {
            children = new TmfEventField[fields.size()];
            int position = 0;
            for (Map.Entry<String, Object> field : fields.entrySet()) {
                children[position++] = new TmfEventField(field.getKey(),
                        field.getValue(), null);
            }
        }
        TmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID,
                event, children);
        String eventName = schedSwitch
                ? TraceXEventLayout.TRACEX_SCHED_SWITCH
                : event.getMetadata().getName();
        return new TmfEvent(this, rank,
                TmfTimestamp.fromNanos(timestamp(
                        event.getRelativeTicks().orElseThrow())),
                new TmfEventType(eventName, null), content);
    }

    /** @return whether this TraceX event represents a thread context switch */
    private static boolean isSchedulingEvent(TraceXEvent event) {
        switch (event.getNormalizedEventId()) {
        case EVT_THREAD_RESUME:
        case EVT_THREAD_SUSPEND:
        case EVT_TIME_SLICE:
        case EVT_THREAD_RELINQUISH:
            return true;
        case EVT_RUNNING:
            TraceXContext context = event.getContext();
            return context.getKind() == TraceXContext.Kind.THREAD
                    || context.getKind() == TraceXContext.Kind.IDLE;
        default:
            return false;
        }
    }

    /**
     * Resolve the TID being scheduled in for a scheduling event, mirroring the
     * word mapping used by the kernel state provider.
     */
    private int nextThreadTid(TraceXEvent event) {
        switch (event.getNormalizedEventId()) {
        case EVT_THREAD_RESUME:
        case EVT_THREAD_SUSPEND:
            return getThreadId(event.getInformationWord(4));
        case EVT_TIME_SLICE:
            return getThreadId(event.getInformationWord(1));
        case EVT_THREAD_RELINQUISH:
            return getThreadId(event.getInformationWord(2));
        case EVT_RUNNING:
            return getThreadId(event.getContext().getRawAddress());
        default:
            return -1;
        }
    }

    /**
     * Build the event-content fields for a synthetic {@code sched_switch}.
     *
     * <p>
     * The stock kernel CPU-usage analysis credits the time elapsed since the
     * previous switch to {@code prev_tid}. For TraceX, the thread running up to
     * this event is the event context thread, and the thread scheduled in is
     * derived from the event's information word. All original TraceX fields are
     * preserved as additional children so the Events table is unaffected.
     */
    private TmfEventField[] buildSchedSwitchFields(TraceXEvent event,
            Map<String, Object> fields) {
        int prevTid = getThreadId(event.getContext().getRawAddress());
        int nextTid = nextThreadTid(event);
        String prevComm = getThreadName(prevTid);
        String nextComm = getThreadName(nextTid);
        Integer prevPrio = event.getContext().getPriority();

        List<TmfEventField> children = new ArrayList<>(fields.size() + 7);
        // Synthetic sched_switch fields consumed by the kernel analyses.
        children.add(new TmfEventField(TraceXEventLayout.TRACEX_PREV_TID,
                Long.valueOf(prevTid), null));
        children.add(new TmfEventField(TraceXEventLayout.TRACEX_NEXT_TID,
                Long.valueOf(nextTid), null));
        children.add(new TmfEventField(TraceXEventLayout.TRACEX_PREV_COMM,
                prevComm == null ? "" : prevComm, null)); //$NON-NLS-1$
        children.add(new TmfEventField(TraceXEventLayout.TRACEX_NEXT_COMM,
                nextComm == null ? "" : nextComm, null)); //$NON-NLS-1$
        children.add(new TmfEventField(TraceXEventLayout.TRACEX_PREV_PRIO,
                Long.valueOf(prevPrio == null ? -1 : prevPrio.intValue()), null));
        children.add(new TmfEventField(TraceXEventLayout.TRACEX_NEXT_PRIO,
                Long.valueOf(-1), null));
        children.add(new TmfEventField(TraceXEventLayout.TRACEX_PREV_STATE,
                Long.valueOf(0), null));
        // Keep the original ThreadX event name visible even though the TMF
        // event type is renamed to the synthetic sched_switch name.
        children.add(new TmfEventField("tracex_event_name", //$NON-NLS-1$
                event.getMetadata().getName(), null));
        // Preserve the original TraceX fields (skip any that would collide).
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            String key = field.getKey();
            if (!isReservedSchedField(key)) {
                children.add(new TmfEventField(key, field.getValue(), null));
            }
        }
        return children.toArray(new TmfEventField[0]);
    }

    private static boolean isReservedSchedField(String key) {
        return TraceXEventLayout.TRACEX_PREV_TID.equals(key)
                || TraceXEventLayout.TRACEX_NEXT_TID.equals(key)
                || TraceXEventLayout.TRACEX_PREV_COMM.equals(key)
                || TraceXEventLayout.TRACEX_NEXT_COMM.equals(key)
                || TraceXEventLayout.TRACEX_PREV_PRIO.equals(key)
                || TraceXEventLayout.TRACEX_NEXT_PRIO.equals(key)
                || TraceXEventLayout.TRACEX_PREV_STATE.equals(key);
    }

    @Override
    public IKernelAnalysisEventLayout getKernelEventLayout() {
        return TraceXEventLayout.getInstance();
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

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return TraceXAspects.getAspects();
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
     * Build a stable, dense thread-id map from the registry so TraceX thread
     * object addresses can be exposed as Linux-style TIDs. Idle keeps
     * {@link #IDLE_TID}; every registered thread gets a positive id in registry
     * order.
     */
    private void buildThreadIds(TraceXFile input) {
        Map<@NonNull Long, @NonNull Integer> ids = new LinkedHashMap<>();
        Map<@NonNull Integer, @NonNull String> names = new LinkedHashMap<>();
        try {
            int next = 1;
            for (TraceXRegistryRecord record : input.getRegistry().getRecords()) {
                if (record.isValid()
                        && record.getType() == TraceXObjectType.THREAD
                        && !ids.containsKey(Long.valueOf(record.getAddress()))) {
                    Integer tid = Integer.valueOf(next++);
                    ids.put(Long.valueOf(record.getAddress()), tid);
                    names.put(tid, record.getName());
                }
            }
        } catch (IOException e) {
            Activator.getInstance().logError("Failed to read TraceX registry for thread ids", e); //$NON-NLS-1$
        }
        fThreadIds = Collections.unmodifiableMap(ids);
        fThreadNames = Collections.unmodifiableMap(names);
    }

    /**
     * Resolve a ThreadX thread object address to a stable Linux-style TID.
     *
     * @param address
     *            unsigned 32-bit thread object address, or {@code 0} for idle
     * @return a dense thread id, {@link #IDLE_TID} for the idle context, or
     *         {@code -1} when the address is not a known registered thread
     */
    public int getThreadId(long address) {
        if (address == 0) {
            return IDLE_TID;
        }
        Integer tid = fThreadIds.get(Long.valueOf(address));
        return tid == null ? -1 : tid.intValue();
    }

    /**
     * Get the display name of a registered thread id.
     *
     * @param tid
     *            thread id previously returned by {@link #getThreadId(long)}
     * @return the thread name, or {@code null} when unknown
     */
    public @Nullable String getThreadName(int tid) {
        return fThreadNames.get(Integer.valueOf(tid));
    }

    /**
     * Get the immutable thread-id-to-name map used to seed the kernel state
     * system threads.
     *
     * @return map from dense thread id to thread name
     */
    public @NonNull Map<@NonNull Integer, @NonNull String> getThreadNames() {
        return fThreadNames;
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
            applyClockConfiguration(fTimerDirection, Long.valueOf(frequency));
        }
    }

    /**
     * Clear the target frequency and restore ordering-tick timestamps.
     *
     * @throws TmfTraceException if persistence or rebuilding fails
     */
    public void clearTicksPerSecond() throws TmfTraceException {
        synchronized (fClockConfigurationLock) {
            applyClockConfiguration(fTimerDirection, null);
        }
    }

    /**
     * Set and persist the target timer direction.
     *
     * @param direction trace-wide timer direction
     * @throws TmfTraceException if persistence or rebuilding fails
     */
    public void setTimerDirection(TraceXTimerDirection direction)
            throws TmfTraceException {
        Objects.requireNonNull(direction);
        synchronized (fClockConfigurationLock) {
            applyClockConfiguration(direction, fTicksPerSecond);
        }
    }

    /**
     * Validate and apply direction and frequency as one persisted change.
     *
     * @param direction trace-wide timer direction
     * @param ticksPerSecond positive decimal frequency, or null for unknown time
     * @throws TmfTraceException if persistence or rebuilding fails
     * @throws IllegalArgumentException if the frequency is invalid
     */
    public void configureClock(TraceXTimerDirection direction,
            @Nullable String ticksPerSecond) throws TmfTraceException {
        Objects.requireNonNull(direction);
        Long frequency = ticksPerSecond == null ? null
                : Long.valueOf(TraceXTimeNormalizer.parseTicksPerSecond(ticksPerSecond));
        synchronized (fClockConfigurationLock) {
            applyClockConfiguration(direction, frequency);
        }
    }

    /** Apply a validated pair without exposing a partially changed clock. */
    private void applyClockConfiguration(TraceXTimerDirection direction,
            @Nullable Long frequency) throws TmfTraceException {
        synchronized (this) {
            TraceXFile input = fInput;
            if (input == null) {
                throw new TmfTraceException("TraceX trace is not initialized"); //$NON-NLS-1$
            }
            if (direction == fTimerDirection && Objects.equals(frequency, fTicksPerSecond)) {
                return;
            }
            try {
                TraceXEventIndex replacement = buildTimeBase(input, direction, frequency);
                IResource resource = getResource();
                if (resource != null) {
                    resource.setPersistentProperty(PERSISTED_CLOCK,
                            direction.getPropertyValue() + ":" //$NON-NLS-1$
                                    + (frequency == null ? "" : frequency.toString())); //$NON-NLS-1$
                }
                fTimerDirection = direction;
                fTicksPerSecond = frequency;
                installTimeBase(replacement);
            } catch (IOException | CoreException | IllegalArgumentException e) {
                throw new TmfTraceException(
                        "Failed to apply TraceX clock configuration: " + e.getMessage(), e); //$NON-NLS-1$
            }
        }
        invalidateTimeDependentData();
    }

    /** Reset all artifacts whose contents depend on the trace time base. */
    protected void invalidateTimeDependentData() {
        List<IAnalysisModule> usedExecution = new ArrayList<>();
        for (IAnalysisModule module : getAnalysisModules()) {
            if (module instanceof TraceXAnalysis && ((TraceXAnalysis) module).getStateSystem() != null) {
                usedExecution.add(module);
            }
            module.waitForCompletion();
            module.cancel();
            module.clearPersistentData();
        }
        TmfTraceManager.deleteSupplementaryFiles(this);
        resetIndexer();
        indexTrace(true);
        for (IAnalysisModule module : getAnalysisModules()) {
            if (module.isAutomatic()) {
                module.schedule();
            }
        }
        for (IAnalysisModule module : usedExecution) {
            module.schedule();
            // A reused state-system module retains its first initialization latch.
            module.waitForCompletion();
        }
        DataProviderManager.getInstance().removeDataProvider(this,
                TraceXDataProviderFactory.ID);
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
