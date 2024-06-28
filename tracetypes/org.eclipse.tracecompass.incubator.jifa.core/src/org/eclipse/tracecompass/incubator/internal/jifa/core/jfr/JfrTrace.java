package org.eclipse.tracecompass.incubator.internal.jifa.core.jfr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.jifa.core.Activator;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.AllocatedMemoryExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.AllocationsExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.CPUSampleExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.CPUTimeExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.ClassLoadCountExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.ClassLoadWallTimeExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.Extractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.FileIOTimeExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.FileReadExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.FileWriteExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.JFRAnalysisContext;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.NativeExecutionExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.SocketReadSizeExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.SocketReadTimeExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.SocketWriteSizeExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.SocketWriteTimeExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.SynchronizationExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.ThreadParkExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.ThreadSleepTimeExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor.WallClockExtractor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.ActiveSetting;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedThread;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.SymbolBase;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.SymbolTable;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.request.AnalysisRequest;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

public class JfrTrace extends TmfTrace {

    private File fJfrTrace;
    private TmfLongLocation fLocation;
    private List<IItem> fEvents = new ArrayList<>();
    private SymbolTable<SymbolBase> fSymbols = new SymbolTable<>();
    private JFRAnalysisContext fJfrAnalysisContext;
    private List<Extractor> fExtractors;

    public JfrTrace() {
        // do nothin
    }

    @Override
    public IStatus validate(IProject project, String path) {
        char[] expected = { 'F', 'L', 'R', 0 };
        char[] data = new char[4];
        try (FileReader fr = new FileReader(new File(path))) {
            fr.read(data);
            if (Objects.deepEquals(data, expected)) {
                return new TraceValidationStatus(40, this.getClass().getCanonicalName());
            }
        } catch (FileNotFoundException e) {
            return new Status(IStatus.INFO, this.getClass().getCanonicalName(), "Trace File Not found"); //$NON-NLS-1$
        } catch (IOException e) {
            return new Status(IStatus.INFO, this.getClass().getCanonicalName(), "Failed to read file: " + e.getMessage()); //$NON-NLS-1$
        }
        return new Status(IStatus.INFO, this.getClass().getCanonicalName(), "Trace File failed to read"); // $NON-NLS-1 //$NON-NLS-1$
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        fJfrTrace = new File(path);
        try {
            fJfrAnalysisContext = new JFRAnalysisContext(new AnalysisRequest(fJfrTrace.toPath(), -1));
            IItemCollection rawEvents = JfrLoaderToolkit.loadEvents(fJfrTrace);
            for (IItemIterable item : rawEvents) {
                for (IItem event : item) {
                    fEvents.add(event);
                }
            }
            fExtractors = getExtractors(fJfrAnalysisContext);
        } catch (IOException | CouldNotLoadRecordingException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        if (fLocation == null) {
            fLocation = new TmfLongLocation(0);
        }
        return fLocation;
    }

    @Override
    public double getLocationRatio(@Nullable ITmfLocation location) {
        if (location == null || fEvents == null) {
            return 0.0;
        }
        return fLocation.getLocationInfo().doubleValue() / fEvents.size();
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
        if (fEvents == null) {
            return null;
        }
        return new TmfContext(new TmfLongLocation((long) (fEvents.size() * ratio)));
    }

    @Override
    public ITmfEvent parseEvent(ITmfContext context) {
        int index = getIndex(context);
        if (fEvents != null && index < fEvents.size()) {
            IItem event = fEvents.get(index);
            ITmfEvent tmfEvent = convert(event, index);
            fLocation = new TmfLongLocation(index + 1L);
            return tmfEvent;
        }
        return null;
    }

    private ITmfEvent convert(IItem event, long rank) {
        RecordedEvent recordedEvent = RecordedEvent.newInstance(event, fSymbols);

        TmfEventType type = new TmfEventType(recordedEvent.getEventType().getName(), null);
        RecordedThread threadInfo = recordedEvent.getThread();
        for(Extractor extractor : fExtractors) {
            extractor.process(recordedEvent);
        }
        List<ITmfEventField> fields = new ArrayList<>();
        if (threadInfo != null) {
            fields.add(new TmfEventField("ExecName", threadInfo.getJavaName(), null));
            fields.add(new TmfEventField("JTID", threadInfo.getJavaThreadId(), null));
            fields.add(new TmfEventField("TID", threadInfo.getOSThreadId(), null));
        }
        if (recordedEvent.getValue("duration") != null) {
            fields.add(new TmfEventField("Duration", recordedEvent.getDurationNano(), null));
        }
        ActiveSetting activeSetting = recordedEvent.getActiveSetting();
        if (activeSetting != null) {
            fields.add(new TmfEventField(activeSetting.getSettingName(), activeSetting, null));
        }
        TmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields.toArray(new TmfEventField[0]));
        return new TmfEvent(this, rank, TmfTimestamp.fromNanos(recordedEvent.getStartTimeNanos()), type, content);
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

    private static List<Extractor> getExtractors(JFRAnalysisContext context) {
        List<Extractor> extractors = new ArrayList<>();
        extractors.add(new CPUTimeExtractor(context));
        extractors.add(new CPUSampleExtractor(context));
        extractors.add(new WallClockExtractor(context));
        extractors.add(new NativeExecutionExtractor(context));
        extractors.add(new AllocationsExtractor(context));
        extractors.add(new AllocatedMemoryExtractor(context));

        extractors.add(new FileIOTimeExtractor(context));
        extractors.add(new FileReadExtractor(context));
        extractors.add(new FileWriteExtractor(context));

        extractors.add(new SocketReadTimeExtractor(context));
        extractors.add(new SocketReadSizeExtractor(context));
        extractors.add(new SocketWriteTimeExtractor(context));
        extractors.add(new SocketWriteSizeExtractor(context));

        extractors.add(new SynchronizationExtractor(context));
        extractors.add(new ThreadParkExtractor(context));

        extractors.add(new ClassLoadCountExtractor(context));
        extractors.add(new ClassLoadWallTimeExtractor(context));

        extractors.add(new ThreadSleepTimeExtractor(context));
        return extractors;
    }
}
