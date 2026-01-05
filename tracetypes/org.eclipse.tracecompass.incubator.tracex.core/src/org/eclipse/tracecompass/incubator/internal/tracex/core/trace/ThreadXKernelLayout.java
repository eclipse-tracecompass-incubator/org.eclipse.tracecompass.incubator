package org.eclipse.tracecompass.incubator.internal.tracex.core.trace;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.DefaultEventLayout;

public class ThreadXKernelLayout extends DefaultEventLayout {
    private static DefaultEventLayout instance = null;

    private ThreadXKernelLayout() {
        // do nothing
    }

    public static synchronized DefaultEventLayout getInstance(){
        if (ThreadXKernelLayout.instance == null) {
            ThreadXKernelLayout.instance = new ThreadXKernelLayout();
        }
        return ThreadXKernelLayout.instance;
    }

    @Override
    public @NonNull String eventIrqEntry() {
        return "TX_TRACE_ISR_ENTER";
    }

    @Override
    public @NonNull String eventIrqExit() {
        return "TX_TRACE_ISR_EXIT";
    }
    @Override
    public @NonNull String fieldIrq() {
        return "ISR Number";
    }

    @Override
    public @NonNull String eventSchedSwitch() {
        return "TX_TRACE_THREAD_RESUME";
    }

    @Override
    public @NonNull String fieldNextComm() {
        return "Thread Pointer";
    }
}
