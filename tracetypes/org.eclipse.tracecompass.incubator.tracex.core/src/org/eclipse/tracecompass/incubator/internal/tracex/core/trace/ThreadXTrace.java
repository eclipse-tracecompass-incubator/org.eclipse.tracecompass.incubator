package org.eclipse.tracecompass.incubator.internal.tracex.core.trace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.tracex.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfContentFieldAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.parsers.custom.CustomTxtTraceContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

public class ThreadXTrace extends TmfTrace implements IKernelTrace {

    public static final String ID = "org.eclipse.tracecompass.incubator.tracex.core.trace"; //$NON-NLS-1$

    private static final TmfLongLocation NULL_LOCATION = new TmfLongLocation(-1L);
    private static final TmfCpuAspect NULL_CORE_ASPECT = new TmfCpuAspect() {

        @Override
        public @Nullable Integer resolve(@NonNull ITmfEvent event) {
            return 0;
        }
    };

    private ThreadXControlHeader fHeader = null;
    private List<ThreadXObjectRegistryEntry> fTraceObjects = new ArrayList<>();
    private File fFile;
    private RandomAccessFile fFileInput;
    private long fDataOffset = 0;
    private List<ITmfEventAspect<?>> fAspects = new ArrayList<>();
    private Set<String> fAspectNames = new HashSet<>();

    @Override
    public IStatus validate(IProject project, String path) {
        try {
            ThreadXControlHeader header = ThreadXControlHeader.read(path);
            if (header.isHeaderId()) {
                return new TraceValidationStatus(51, Activator.PLUGIN_ID);
            }
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a TRX trace: Control Header ID mismatch"); //$NON-NLS-1$
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a TRX trace: " + e.getMessage(), e); //$NON-NLS-1$
        }
    }

    @Override
    protected void initialize(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initialize(resource, path, type);
        try {
            fHeader = ThreadXControlHeader.read(path);
            fFile = new File(path);
            int fileStart = fHeader.getObjectRegistryStartPointer();
            int fileEnd = fHeader.getObjectRegistryEndPointer();
            for (TraceXEventType eventType : TraceXEventType.values()) {
                for (String name : eventType.getEventType().getFieldNames()) {
                    if (!fAspectNames.contains(name)) {
                        fAspectNames.add(name);
                        fAspects.add(new TmfContentFieldAspect(name, name));
                    }
                }
            }
            if (fileStart < fFile.length() || fileEnd < fFile.length()) {
                try (RandomAccessFile fileInput = new RandomAccessFile(fFile, "r");) { //$NON-NLS-1$
                    fileInput.seek(fileStart);
                    while (fileInput.getFilePointer() < fileEnd) {
                        fileInput.readInt();
                        fTraceObjects.add(new ThreadXObjectRegistryEntry(null, null, (char) 0, (char) 0, fileEnd, fileStart, fileEnd, null));

                    }
                }
            }
            fDataOffset = fHeader.getBufferStartPointer();
            fFileInput = new RandomAccessFile(fFile, "r"); //$NON-NLS-1$
            fFileInput.seek(fDataOffset);
            fDataOffset = fFileInput.getFilePointer();
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        List<ITmfEventAspect<?>> aspects = new ArrayList<>();
        for (ITmfEventAspect<?> aspect : super.getEventAspects()) {
            aspects.add(aspect);
        }
        aspects.add(NULL_CORE_ASPECT);
        aspects.addAll(fAspects);
        return aspects;
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        long temp = -1;
        try {
            temp = fFileInput.getFilePointer();
        } catch (IOException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
        return new TmfLongLocation(temp);
    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        long size = fFile.length() - fDataOffset;
        long pos;
        try {
            pos = fFileInput.getFilePointer() - fDataOffset;
        } catch (IOException e) {
            pos = 0;
        }
        return 1.0 / size * pos;
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {
        final TmfContext context = new TmfContext(NULL_LOCATION, ITmfContext.UNKNOWN_RANK);
        if (NULL_LOCATION.equals(location) || fFile == null) {
            return context;
        }
        try {
            if (location == null) {
                fFileInput.seek(fDataOffset);
            } else if (location.getLocationInfo() instanceof Long) {
                fFileInput.seek((Long) location.getLocationInfo());
            }
            context.setLocation(new TmfLongLocation(fFileInput.getFilePointer()));
            return context;
        } catch (final FileNotFoundException e) {
            Activator.getInstance().logError("Error seeking event. File not found: " + getPath(), e); //$NON-NLS-1$
            return context;
        } catch (final IOException e) {
            Activator.getInstance().logError("Error seeking event. File: " + getPath(), e); //$NON-NLS-1$
            return context;
        }
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        if (fFile == null) {
            return new TmfContext(NULL_LOCATION, ITmfContext.UNKNOWN_RANK);
        }
        try {
            long pos = Math.round(ratio * fFile.length()) - fDataOffset;
            while (pos > 0) {
                fFileInput.seek(pos - 1);
                if (fFileInput.read() == '\n') {
                    break;
                }
                pos--;
            }
            final ITmfLocation location = new TmfLongLocation(pos);
            final ITmfContext context = seekEvent(location);
            context.setRank(ITmfContext.UNKNOWN_RANK);
            return context;
        } catch (final IOException e) {
            Activator.getInstance().logError("Error seeking event. File: " + getPath(), e); //$NON-NLS-1$
            return new CustomTxtTraceContext(NULL_LOCATION, ITmfContext.UNKNOWN_RANK);
        }
    }

    @Override
    public ITmfEvent parseEvent(ITmfContext context) {
        try {
            if (fFileInput.length() - fFileInput.getFilePointer() < 256) {
                return null;
            }
        } catch (IOException e) {
            Activator.getInstance().logError("Cannot read file.", e); //$NON-NLS-1$
        }
        try {
            byte data[] = new byte[32];
            fFileInput.read(data);
            ByteBuffer bb = ByteBuffer.wrap(data);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int threadPointer = bb.getInt();
            int threadPriority = bb.getInt();
            int eventId = bb.getInt();
            int timeStamp = bb.getInt();
            int informationField1 = bb.getInt();
            int informationField2 = bb.getInt();
            int informationField3 = bb.getInt();
            int informationField4 = bb.getInt();
            ThreadXEvent event = new ThreadXEvent(this, threadPointer, threadPriority, eventId, timeStamp, informationField1, informationField2, informationField3, informationField4);
            return event.makeEvent();
        } catch (IOException e) {
            Activator.getInstance().logError("Cannot read trace.", e); //$NON-NLS-1$
        }
        return null;
    }

    @Override
    public @NonNull IKernelAnalysisEventLayout getKernelEventLayout() {
        return ThreadXKernelLayout.getInstance();
    }

}
