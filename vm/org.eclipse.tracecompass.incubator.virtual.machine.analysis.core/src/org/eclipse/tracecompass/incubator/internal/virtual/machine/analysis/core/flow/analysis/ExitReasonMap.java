/*******************************************************************************
 * Copyright (c) 2026 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;

import java.util.HashMap;
import java.util.Map;


/**
 * Mapping of Exit reason number to their text description
 *
 * @author Francois Belias
 */
public class ExitReasonMap {

    private static final Map<Integer, String> exitReasonMap = new HashMap<>();

    static {
        // VMX specific reasons
        exitReasonMap.put(0x80000000, "VMX_EXIT_REASONS_FAILED_VMENTRY"); //$NON-NLS-1$
        exitReasonMap.put(0x08000000, "VMX_EXIT_REASONS_SGX_ENCLAVE_MODE"); //$NON-NLS-1$

        // General exit reasons
        exitReasonMap.put(0, "EXIT_REASON_EXCEPTION_NMI"); //$NON-NLS-1$
        exitReasonMap.put(1, "EXIT_REASON_EXTERNAL_INTERRUPT"); //$NON-NLS-1$
        exitReasonMap.put(2, "EXIT_REASON_TRIPLE_FAULT"); //$NON-NLS-1$
        exitReasonMap.put(3, "EXIT_REASON_INIT_SIGNAL"); //$NON-NLS-1$
        exitReasonMap.put(4, "EXIT_REASON_SIPI_SIGNAL"); //$NON-NLS-1$

        exitReasonMap.put(7, "EXIT_REASON_INTERRUPT_WINDOW"); //$NON-NLS-1$
        exitReasonMap.put(8, "EXIT_REASON_NMI_WINDOW"); //$NON-NLS-1$
        exitReasonMap.put(9, "EXIT_REASON_TASK_SWITCH"); //$NON-NLS-1$
        exitReasonMap.put(10, "EXIT_REASON_CPUID"); //$NON-NLS-1$
        exitReasonMap.put(12, "EXIT_REASON_HLT"); //$NON-NLS-1$
        exitReasonMap.put(13, "EXIT_REASON_INVD"); //$NON-NLS-1$
        exitReasonMap.put(14, "EXIT_REASON_INVLPG"); //$NON-NLS-1$
        exitReasonMap.put(15, "EXIT_REASON_RDPMC"); //$NON-NLS-1$
        exitReasonMap.put(16, "EXIT_REASON_RDTSC"); //$NON-NLS-1$
        exitReasonMap.put(18, "EXIT_REASON_VMCALL"); //$NON-NLS-1$
        exitReasonMap.put(19, "EXIT_REASON_VMCLEAR"); //$NON-NLS-1$
        exitReasonMap.put(20, "EXIT_REASON_VMLAUNCH"); //$NON-NLS-1$
        exitReasonMap.put(21, "EXIT_REASON_VMPTRLD"); //$NON-NLS-1$
        exitReasonMap.put(22, "EXIT_REASON_VMPTRST"); //$NON-NLS-1$
        exitReasonMap.put(23, "EXIT_REASON_VMREAD"); //$NON-NLS-1$
        exitReasonMap.put(24, "EXIT_REASON_VMRESUME"); //$NON-NLS-1$
        exitReasonMap.put(25, "EXIT_REASON_VMWRITE"); //$NON-NLS-1$
        exitReasonMap.put(26, "EXIT_REASON_VMOFF"); //$NON-NLS-1$
        exitReasonMap.put(27, "EXIT_REASON_VMON"); //$NON-NLS-1$
        exitReasonMap.put(28, "EXIT_REASON_CR_ACCESS"); //$NON-NLS-1$
        exitReasonMap.put(29, "EXIT_REASON_DR_ACCESS"); //$NON-NLS-1$
        exitReasonMap.put(30, "EXIT_REASON_IO_INSTRUCTION"); //$NON-NLS-1$
        exitReasonMap.put(31, "EXIT_REASON_MSR_READ"); //$NON-NLS-1$
        exitReasonMap.put(32, "EXIT_REASON_MSR_WRITE"); //$NON-NLS-1$
        exitReasonMap.put(33, "EXIT_REASON_INVALID_STATE"); //$NON-NLS-1$
        exitReasonMap.put(34, "EXIT_REASON_MSR_LOAD_FAIL"); //$NON-NLS-1$
        exitReasonMap.put(36, "EXIT_REASON_MWAIT_INSTRUCTION"); //$NON-NLS-1$
        exitReasonMap.put(37, "EXIT_REASON_MONITOR_TRAP_FLAG"); //$NON-NLS-1$
        exitReasonMap.put(39, "EXIT_REASON_MONITOR_INSTRUCTION"); //$NON-NLS-1$
        exitReasonMap.put(40, "EXIT_REASON_PAUSE_INSTRUCTION"); //$NON-NLS-1$
        exitReasonMap.put(41, "EXIT_REASON_MCE_DURING_VMENTRY"); //$NON-NLS-1$
        exitReasonMap.put(43, "EXIT_REASON_TPR_BELOW_THRESHOLD"); //$NON-NLS-1$
        exitReasonMap.put(44, "EXIT_REASON_APIC_ACCESS"); //$NON-NLS-1$
        exitReasonMap.put(45, "EXIT_REASON_EOI_INDUCED"); //$NON-NLS-1$
        exitReasonMap.put(46, "EXIT_REASON_GDTR_IDTR"); //$NON-NLS-1$
        exitReasonMap.put(47, "EXIT_REASON_LDTR_TR"); //$NON-NLS-1$
        exitReasonMap.put(48, "EXIT_REASON_EPT_VIOLATION"); //$NON-NLS-1$
        exitReasonMap.put(49, "EXIT_REASON_EPT_MISCONFIG"); //$NON-NLS-1$
        exitReasonMap.put(50, "EXIT_REASON_INVEPT"); //$NON-NLS-1$
        exitReasonMap.put(51, "EXIT_REASON_RDTSCP"); //$NON-NLS-1$
        exitReasonMap.put(52, "EXIT_REASON_PREEMPTION_TIMER"); //$NON-NLS-1$
        exitReasonMap.put(53, "EXIT_REASON_INVVPID"); //$NON-NLS-1$
        exitReasonMap.put(54, "EXIT_REASON_WBINVD"); //$NON-NLS-1$
        exitReasonMap.put(55, "EXIT_REASON_XSETBV"); //$NON-NLS-1$
        exitReasonMap.put(56, "EXIT_REASON_APIC_WRITE"); //$NON-NLS-1$
        exitReasonMap.put(57, "EXIT_REASON_RDRAND"); //$NON-NLS-1$
        exitReasonMap.put(58, "EXIT_REASON_INVPCID"); //$NON-NLS-1$
        exitReasonMap.put(59, "EXIT_REASON_VMFUNC"); //$NON-NLS-1$
        exitReasonMap.put(60, "EXIT_REASON_ENCLS"); //$NON-NLS-1$
        exitReasonMap.put(61, "EXIT_REASON_RDSEED"); //$NON-NLS-1$
        exitReasonMap.put(62, "EXIT_REASON_PML_FULL"); //$NON-NLS-1$
        exitReasonMap.put(63, "EXIT_REASON_XSAVES"); //$NON-NLS-1$
        exitReasonMap.put(64, "EXIT_REASON_XRSTORS"); //$NON-NLS-1$
        exitReasonMap.put(67, "EXIT_REASON_UMWAIT"); //$NON-NLS-1$
        exitReasonMap.put(68, "EXIT_REASON_TPAUSE"); //$NON-NLS-1$
        exitReasonMap.put(74, "EXIT_REASON_BUS_LOCK"); //$NON-NLS-1$
        exitReasonMap.put(75, "EXIT_REASON_NOTIFY"); //$NON-NLS-1$
    }

    /**
     * @param code The code of the exit type
     * @return The text describing the exit type
     */
    public static String getExitReasonName(int code) {
        return exitReasonMap.getOrDefault(code, "UNKNOWN_EXIT_REASON"); //$NON-NLS-1$
    }
}