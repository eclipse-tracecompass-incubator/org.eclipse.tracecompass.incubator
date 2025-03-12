package org.eclipse.tracecompass.incubator.internal.perfetto.core.event;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.perfetto.core.trace.Perfetto;
import org.eclipse.tracecompass.tmf.core.event.ITmfCustomAttributes;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfCallsite;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfModelLookup;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfSourceLookup;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 *
 */
public class PerfettoEvent extends TmfEvent implements ITmfSourceLookup, ITmfModelLookup, ITmfCustomAttributes{

    /**
     * Field to override {@link TmfEvent#getName()}, to bypass the type-getting
     */
    private final String fEventName;

    /** Lazy-loaded field for the type, overriding TmfEvent's field */
    private transient @Nullable TmfEventType fEventType;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Inner constructor to create "null" events. Don't use this directly in
     * normal usage, use {@link CtfTmfEventFactory#getNullEvent(CtfTmfTrace)} to
     * get an instance of an empty event.
     *
     * There is no need to give higher visibility to this method than package
     * visible.
     *
     * @param trace
     *            The trace associated with this event
     */
    PerfettoEvent(Perfetto trace) {
        super(trace,
                ITmfContext.UNKNOWN_RANK,
                TmfTimestamp.fromNanos(-1),
                null,
                new TmfEventField("", null, new PerfettoEventData[0])); //$NON-NLS-1$
        fEventName = "N/A";
    }

    /**
     * Default constructor. Do not use directly, but it needs to be present
     * because it's used in extension points, and the framework will use this
     * constructor to get the class type.
     *
     * Should not be called by normal code.
     */
    public PerfettoEvent() {
        super();
        fEventName = "N/A";
    }

    // TODO: Use the extra names and values and make sure to use the ITmfEventType, ITmfEventField
    /**
     * @param trace
     * @param rank
     * @param name
     * @param timestamp
     * @param eventData
     */
    public PerfettoEvent(Perfetto trace, long rank, String name, long timestamp, TmfEventType type, PerfettoEventData eventData) {
        super(trace, rank, TmfTimestamp.fromNanos(timestamp), type, eventData);
        fEventName = name;
    }

    @Override
    public @NonNull Set<@NonNull String> listCustomAttributes() {
        // TODO Auto-generated method stub
        return new HashSet<>();
    }

    @Override
    public @Nullable String getCustomAttribute(@NonNull String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getModelUri() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public @Nullable ITmfCallsite getCallsite() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public synchronized ITmfEventType getType() {
        TmfEventType type = fEventType;
        if (type == null) {
            type = new TmfEventType(fEventName, getContent());

            /*
             * Register the event type in the owning trace, but only if there is
             * one
             */
            getTrace().registerEventType(type);
            fEventType = type;
        }
        return type;
    }

    @Override
    public String getName() {
        return fEventName;
    }

    @Override
    public Perfetto getTrace() {
        // TODO Auto-generated method stub
        return (Perfetto) super.getTrace();
    }

}
