package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * An example of a simple state provider for a simple state system analysis
 *
 * This module is also in the developer documentation of Trace Compass. If it is
 * modified here, the doc should also be updated.
 *
 * @author Francois Belias
 */
public class StateProvider extends AbstractTmfStateProvider {

    private static final @NonNull String PROVIDER_ID = "org.eclipse.tracecompass.incubator.internal.overhead.core.analysis"; //$NON-NLS-1$
    private static final int VERSION = 0;

    /**
     * Constructor
     *
     * @param trace
     *            The trace for this state provider
     */
    public StateProvider(@NonNull ITmfTrace trace) {
        super(trace, PROVIDER_ID);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new StateProvider(getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent event) {

        /**
         * Do what needs to be done with this event, here is an example that
         * updates the CPU state and TID after a sched_switch
         */
        if (event.getName().equals("sched_switch")) { //$NON-NLS-1$

            final long ts = event.getTimestamp().getValue();
            Long nextTid = event.getContent().getFieldValue(Long.class, "next_tid"); //$NON-NLS-1$
            Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
            if (cpu == null || nextTid == null) {
                return;
            }

            ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());
            int quark = ss.getQuarkAbsoluteAndAdd("CPUs", String.valueOf(cpu)); //$NON-NLS-1$
            // The main quark contains the tid of the running thread
            ss.modifyAttribute(ts, nextTid, quark);

            // The status attribute has an integer value
            int statusQuark = ss.getQuarkRelativeAndAdd(quark, "Status"); //$NON-NLS-1$
            Integer value = (nextTid > 0 ? 1 : 0);
            ss.modifyAttribute(ts, value, statusQuark);
        }
    }

}
