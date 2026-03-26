package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Flow analysis of a thread. Should work on a native system and on a virtualized system
 *
 * @author philippe
 */
public class ThreadFlowInfo {
    final int tid;
    final String processName;
    final List<KernelEventInfo> events = new ArrayList<>();

    ThreadFlowInfo(int tid, String processName) {
        this.tid = tid;
        this.processName = processName;
    }

    void addEvent(KernelEventInfo evt) {
        events.add(evt);
    }

    void printFlow() {
        System.out.printf("  Thread %d (%d events):\n", tid, events.size()); //$NON-NLS-1$

        for (KernelEventInfo evt : events) {
            System.out.printf("    [%d] %s\n", evt.timestamp, evt.name); //$NON-NLS-1$
        }
    }
}
