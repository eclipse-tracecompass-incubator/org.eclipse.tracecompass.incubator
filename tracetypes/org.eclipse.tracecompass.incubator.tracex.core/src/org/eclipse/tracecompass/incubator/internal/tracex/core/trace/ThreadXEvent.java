package org.eclipse.tracecompass.incubator.internal.tracex.core.trace;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class ThreadXEvent {
    private final int fThreadPointer;
    private final int fThreadPriority;
    private final int fEventId;
    private final int fTimeStamp;
    private final int fInformationField1;
    private final int fInformationField2;
    private final int fInformationField3;
    private final int fInformationField4;
    private final ITmfTrace fParent;


    public ThreadXEvent(ITmfTrace parent, int threadPointer, int threadPriority, int eventId, int timeStamp, int informationField1, int informationField2, int informationField3, int informationField4) {
        super();
        fParent = parent;
        fThreadPointer = threadPointer;
        fThreadPriority = threadPriority;
        fEventId = eventId;
        fTimeStamp = timeStamp;
        fInformationField1 = informationField1;
        fInformationField2 = informationField2;
        fInformationField3 = informationField3;
        fInformationField4 = informationField4;
    }

    public ITmfEvent makeEvent() {
        ITmfEventType eventType = TraceXEventType.EVENT_TYPES[fEventId].getEventType();
        @NonNull String[] childRefs = eventType.getFieldNames().toArray(new String[0]);

        ITmfEventField[] children = new ITmfEventField[childRefs.length];

        if (children.length > 0) {
            children[0] = new TmfEventField(childRefs[0], fThreadPointer, null);
        }
        if (children.length > 1) {
            children[1] = new TmfEventField(childRefs[1], fThreadPriority, null);
        }
        if (children.length > 2) {
            children[2] = new TmfEventField(childRefs[2], fInformationField1, null);
        }
        if (children.length > 3) {
            children[3] = new TmfEventField(childRefs[3], fInformationField2, null);
        }
        if (children.length > 4) {
            children[4] = new TmfEventField(childRefs[4], fInformationField3, null);
        }
        if (children.length > 5) {
            children[5] = new TmfEventField(childRefs[5], fInformationField4, null);
        }
        if (fThreadPointer == -1) {
            children[0] = children[2];
            children[2] = children[children.length-1];
            children = Arrays.copyOf(children, children.length-1);
        }
        ITmfEventField root = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, children);
        return new TmfEvent(fParent, -1, TmfTimestamp.fromNanos(fTimeStamp), eventType, root);
    }

}
