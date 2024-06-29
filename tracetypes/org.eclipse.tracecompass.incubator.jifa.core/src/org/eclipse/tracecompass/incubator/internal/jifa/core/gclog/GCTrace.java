package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.counters.core.aspects.CounterAspect;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.GCEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCMemoryItem;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.GCLogParser;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.GCLogParserFactory;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfEventFieldAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

/**
 * GC Trace, encapsulates the GC model and makes it a {@link TmfTrace}
 */
public class GCTrace extends TmfTrace {

    /**
     * The Trace ID
     */
    public static final String ID = "org.eclipse.tracecompass.jifa.gclog";  //$NON-NLS-1$
    private @Nullable GCModel fModel;
    private TmfLongLocation fLocation = new TmfLongLocation(0L);
    private final @NonNull List<ITmfEventAspect<?>> fAspects = new ArrayList<>();

    @Override
    public IStatus validate(@Nullable IProject project, @Nullable String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(path)))) {
            new GCLogParserFactory().getParser(br);
        } catch (IllegalStateException | IOException e) {
            return new Status(IStatus.INFO, getClass(), e.getMessage());
        }
        return new TraceValidationStatus(10, this.getClass().getName());
    }

    @Override
    public void initTrace(@Nullable IResource resource, @Nullable String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        GCLogParserFactory factory = new GCLogParserFactory();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(path)))) {
            GCLogParser parser = factory.getParser(br);
            fModel = parser.parse(br);
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    @Override
    public void initTrace(@Nullable IResource resource, @Nullable String path, @Nullable Class<? extends ITmfEvent> type, @Nullable String name, @Nullable String traceTypeId) throws TmfTraceException {
        super.initTrace(resource, path, type, name, traceTypeId);
        GCLogParserFactory factory = new GCLogParserFactory();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(path)))) {
            GCLogParser parser = factory.getParser(br);
            fModel = parser.parse(br);
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        return fLocation;
    }

    @Override
    public double getLocationRatio(@Nullable ITmfLocation location) {
        GCModel model = fModel;
        if (location == null || model == null) {
            return 0.0;
        }
        return fLocation.getLocationInfo().doubleValue() / model.getAllEvents().size();
    }

    @Override
    public ITmfContext seekEvent(@Nullable ITmfLocation location) {
        if (location instanceof TmfLongLocation) {
            fLocation = (TmfLongLocation) location;
        } else {
            fLocation = new TmfLongLocation(0L);
        }
        return new TmfContext(fLocation);
    }

    @Override
    public @Nullable ITmfContext seekEvent(double ratio) {
        GCModel model = fModel;
        if (model == null) {
            return null;
        }
        return new TmfContext(new TmfLongLocation((long) (model.getAllEvents().size() * ratio)));
    }

    @Override
    public @Nullable ITmfEvent parseEvent(@Nullable ITmfContext context) {
        int index = getIndex(context);
        GCModel model = fModel;
        if (model != null && index < model.getAllEvents().size()) {
            GCEvent event = Objects.requireNonNull(model.getAllEvents().get(index));
            TmfEvent tmfEvent = convert(event);
            fLocation = new TmfLongLocation(index + 1L);
            return tmfEvent;
        }
        return null;
    }

    private TmfEvent convert(GCEvent event) {

        TmfEventType type = new TmfEventType(Objects.requireNonNull(event.getEventType().getName()), null);
        GCModel model = Objects.requireNonNull(fModel);
        ITmfTimestamp time = TmfTimestamp.fromNanos((long) ((event.getStartTime() + model.getReferenceTimestamp()) * 1e6));
        List<TmfEventField> fields = new ArrayList<>();
        fields.add(new TmfEventField("Allocation", event.getAllocation(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("GC Id", event.getGcid(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("Cause", event.getCause(), null)); //$NON-NLS-1$
        long pause = (long) (event.getPause() * 1e6);
        fields.add(new TmfEventField("Pause", pause != 0 ? pause : null, null)); //$NON-NLS-1$
        fields.add(new TmfEventField("Cause Interval", event.getCauseInterval(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("Reclamation", event.getReclamation(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("Cpu time", event.getCpuTime(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("Duration", event.getDuration(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("Level", event.getEventLevel(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("Promotion", event.getPromotion(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("End time", event.getEndTime(), null)); //$NON-NLS-1$
        GCMemoryItem[] memoryItems = event.getMemoryItems();
        if (memoryItems != null) {
            for (GCMemoryItem item : memoryItems) {
                if (item != null) {
                    fields.add(new TmfEventField("mem" + item.getArea().getName() + "-pre", item.getPreUsed(), null)); //$NON-NLS-1$ //$NON-NLS-2$
                    fields.add(new TmfEventField("mem" + item.getArea().getName() + "-post", item.getPostUsed(), null)); //$NON-NLS-1$ //$NON-NLS-2$
                    fields.add(new TmfEventField("mem" + item.getArea().getName() + "-capacity", item.getPostCapacity(), null)); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }
        return new TmfEvent(this, -1, time, type, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields.toArray(new TmfEventField[0])));
    }

    private static int getIndex(@Nullable ITmfContext context) {
        if (context != null) {
            Comparable<?> locationInfo = context.getLocation().getLocationInfo();
            if (locationInfo instanceof Number) {
                return ((Number) locationInfo).intValue();
            }
        }
        return 0;
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        if (fAspects.isEmpty()) {
            List<ITmfEventAspect<?>> aspects = fAspects;
            aspects.addAll((Collection<? extends ITmfEventAspect<?>>) super.getEventAspects());
            aspects.add(2, simpleAspect("Allocation")); //$NON-NLS-1$
            aspects.add(3, simpleAspect("Reclamation")); //$NON-NLS-1$
            aspects.add(3, simpleAspect("Promotion")); //$NON-NLS-1$
            aspects.add(4, simpleAspect("GC Id")); //$NON-NLS-1$
            aspects.add(5, simpleAspect("Cause")); //$NON-NLS-1$
            aspects.add(6, simpleAspect("Cause Interval")); //$NON-NLS-1$
            aspects.add(7, simpleAspect("Cpu time")); //$NON-NLS-1$
            aspects.add(8, simpleAspect("Duration")); //$NON-NLS-1$
            aspects.add(9, simpleAspect("Pause")); //$NON-NLS-1$
            aspects.add(10, simpleAspect("Level")); //$NON-NLS-1$
            int i = 10;
            for (MemoryArea mi : MemoryArea.values()) {

                aspects.add(i++, new CounterAspect("mem" + mi.getName() + "-pre", mi.getName() + " pre")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                aspects.add(i++, new CounterAspect("mem" + mi.getName() + "-post", mi.getName() + " post")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                aspects.add(i++, new CounterAspect("mem" + mi.getName() + "-capacity", mi.getName() + " capacity")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            }
            return aspects;
        }
        return fAspects;
    }

    private static TmfEventFieldAspect simpleAspect(String field) {
        Objects.requireNonNull(field);
        return new TmfEventFieldAspect(field, field, ITmfEvent::getContent);
    }

}
