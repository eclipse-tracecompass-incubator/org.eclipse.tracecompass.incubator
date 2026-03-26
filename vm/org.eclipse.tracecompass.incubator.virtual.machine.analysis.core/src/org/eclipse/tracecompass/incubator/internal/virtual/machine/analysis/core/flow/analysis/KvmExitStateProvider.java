
/*******************************************************************************
 * KVM Exit State Provider
 * This state provider processes trace events related to KVM exits
 *******************************************************************************/



package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;


import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for the KVM exit analysis module.
 * This provider processes KVM exit events from a trace and updates the state system
 * with information about KVM exits per CPU.
 *
 * @author Francois Belias
 */
public class KvmExitStateProvider extends AbstractTmfStateProvider {

    private static final int VERSION = 1;
    private static final String ID = "org.eclipse.tracecompass.incubator.internal.overhead.core.analysis"; //$NON-NLS-1$

    // Event names that we're interested in
    private static final String KVM_EXIT = "kvm_x86_exit"; //$NON-NLS-1$
    private static final String KVM_ENTRY = "kvm_x86_entry"; //$NON-NLS-1$


    // Field names in the KVM exit event
    private static final String EXIT_REASON = "exit_reason"; //$NON-NLS-1$
    private static final String VCPU_ID = "vcpu_id"; //$NON-NLS-1$
    private static final String CPU_ID = "context.cpu_id"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param trace
     *            The trace to analyze
     */
    public KvmExitStateProvider(@NonNull ITmfTrace trace) {
        super(trace, ID);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new KvmExitStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        if (ss == null) {
            return;
        }

        final String eventName = event.getName();
        final long timestamp = event.getTimestamp().toNanos();
        final ITmfEventField content = event.getContent();

        // Process KVM exit events
        if (eventName.equals(KVM_EXIT)) {
            // Get the CPU ID and exit reason
            Object cpuObj = getFieldValue(content, CPU_ID);
            Object exitReasonObj = getFieldValue(content, EXIT_REASON);
            Object vcpuIdObj = getFieldValue(content, VCPU_ID);

            if (cpuObj == null) {
                return;
            }

            Integer cpuId = getIntegerValue(cpuObj);
            Integer exitReason = getIntegerValue(exitReasonObj);
            Integer vcpuId = getIntegerValue(vcpuIdObj);

            if (cpuId == null) {
                return;
            }

            // Create the CPU attribute if it doesn't exist
            int cpuQuark = ss.getQuarkAbsoluteAndAdd("CPUs", String.valueOf(cpuId)); //$NON-NLS-1$

            // Create and increment the KVM exits counter for this CPU
            int exitCountQuark = ss.getQuarkRelativeAndAdd(cpuQuark, "kvm_exits"); //$NON-NLS-1$
            incrementExitCounter(ss, exitCountQuark, timestamp);

            // Track the VCPU if available
            if (vcpuId != null) {
                int vcpuQuark = ss.getQuarkRelativeAndAdd(cpuQuark, "vcpu"); //$NON-NLS-1$
                ss.modifyAttribute(timestamp, vcpuId, vcpuQuark);

                // Also track per-VCPU exit information
                int vcpusQuark = ss.getQuarkAbsoluteAndAdd("VCPUs"); //$NON-NLS-1$
                int specificVcpuQuark = ss.getQuarkRelativeAndAdd(vcpusQuark, String.valueOf(vcpuId));
                int vcpuExitCountQuark = ss.getQuarkRelativeAndAdd(specificVcpuQuark, "kvm_exits"); //$NON-NLS-1$
                incrementExitCounter(ss, vcpuExitCountQuark, timestamp);

                // If we have exit reason information, track it
                if (exitReason != null) {
                    int reasonQuark = ss.getQuarkRelativeAndAdd(specificVcpuQuark, "exit_reasons", ExitReasonMap.getExitReasonName(exitReason)); //$NON-NLS-1$
                    incrementExitCounter(ss, reasonQuark, timestamp);
                }


                // Track which physical CPU this VCPU is running on
                int vcpuOnCpuQuark = ss.getQuarkRelativeAndAdd(specificVcpuQuark, "on_cpu"); //$NON-NLS-1$
                ss.modifyAttribute(timestamp, cpuId, vcpuOnCpuQuark);
            }

            // Track that the CPU is currently in KVM exit mode
            int stateQuark = ss.getQuarkRelativeAndAdd(cpuQuark, "kvm_state"); //$NON-NLS-1$
            ss.modifyAttribute(timestamp, "exit", stateQuark); //$NON-NLS-1$

        } else if (eventName.equals(KVM_ENTRY)) {
            // Get the CPU ID
            Object cpuObj = getFieldValue(content, CPU_ID);
            if (cpuObj == null) {
                return;
            }

            Integer cpuId = getIntegerValue(cpuObj);
            if (cpuId == null) {
                return;
            }

            // Update the CPU state to show it's back in guest mode
            int cpuQuark = ss.getQuarkAbsoluteAndAdd("CPUs", String.valueOf(cpuId)); //$NON-NLS-1$
            int stateQuark = ss.getQuarkRelativeAndAdd(cpuQuark, "kvm_state"); //$NON-NLS-1$
            ss.modifyAttribute(timestamp, "entry", stateQuark); //$NON-NLS-1$
        }
    }

    /**
     * Increment a counter in the state system, handling the case where it doesn't exist yet
     */
    private static void incrementExitCounter(ITmfStateSystemBuilder ss, int quark, long timestamp) {
        Object currentValue = ss.queryOngoing(quark);
        int newValue = 1;

        if (currentValue instanceof Integer) {
            newValue = ((Integer) currentValue) + 1;
        }

        ss.modifyAttribute(timestamp, newValue, quark);
    }

    /**
     * Safely get a field value from the event content
     */
    private static @Nullable Object getFieldValue(ITmfEventField content, String fieldName) {
        ITmfEventField field = content.getField(fieldName);
        return (field != null) ? field.getValue() : null;
    }

    /**
     * Convert an object to Integer if possible
     */
    private static @Nullable Integer getIntegerValue(@Nullable Object obj) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Long) {
            return ((Long) obj).intValue();
        } else if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}