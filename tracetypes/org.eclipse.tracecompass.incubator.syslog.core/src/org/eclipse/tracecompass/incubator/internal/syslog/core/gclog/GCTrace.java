package org.eclipse.tracecompass.incubator.internal.syslog.core.gclog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tracecompass.incubator.internal.syslog.core.gclog.event.GCEvent;
import org.eclipse.tracecompass.incubator.internal.syslog.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.syslog.core.gclog.parser.GCLogParser;
import org.eclipse.tracecompass.incubator.internal.syslog.core.gclog.parser.GCLogParserFactory;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

public class GCTrace extends TmfTrace {

    private GCModel fModel;
    private TmfLongLocation fLocation = new TmfLongLocation(0L);

    @Override
    public IStatus validate(IProject project, String path) {
        return new File(path).canRead() ? new TraceValidationStatus(10, "syslog") : new Status(IStatus.INFO, getClass(), "not a gc log"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
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
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type, String name, String traceTypeId) throws TmfTraceException {
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
    public double getLocationRatio(ITmfLocation location) {
        return fLocation.getLocationInfo().doubleValue() / fModel.getAllEvents().size();
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {

        if (location instanceof TmfLongLocation) {
            fLocation = (TmfLongLocation) location;
        } else {
            fLocation = new TmfLongLocation(0L);
        }
        return new TmfContext(fLocation);
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        return new TmfContext(new TmfLongLocation((long) (fModel.getAllEvents().size() * ratio)));
    }

    @Override
    public ITmfEvent parseEvent(ITmfContext context) {
        int index = getIndex(context);
        if (index < fModel.getAllEvents().size()) {
            GCEvent event = fModel.getAllEvents().get(index);
            TmfEvent tmfEvent = convert(event);
            fLocation = new TmfLongLocation(index + 1);
            return tmfEvent;
        }
        return null;
    }

    private TmfEvent convert(GCEvent event) {

        TmfEventType type = new TmfEventType(event.getEventType().getName(), null);
        ITmfTimestamp time = TmfTimestamp.fromNanos((long) (event.getStartTime() * 1e6));
        List<TmfEventField> fields = new ArrayList<>();
        fields.add(new TmfEventField("Allocation", event.getAllocation(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("gc id", event.getGcid(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("cause", event.getCause(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("pause", event.getPause(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("cause Interval", event.getCauseInterval(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("Reclamation", event.getReclamation(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("Cpu time", event.getCpuTime(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("Duration", event.getDuration(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("Level", event.getEventLevel(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("Promotion", event.getPromotion(), null)); //$NON-NLS-1$
        fields.add(new TmfEventField("End time", event.getEndTime(), null)); //$NON-NLS-1$

        return new TmfEvent(this, -1, time, type, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields.toArray(new TmfEventField[0])));
    }

    private static int getIndex(ITmfContext context) {
        Comparable<?> locationInfo = context.getLocation().getLocationInfo();
        if (locationInfo instanceof Number) {
            return ((Number) locationInfo).intValue();
        }
        return 0;
    }

}
