/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused;

/**
 * This file defines all the attribute names used in the handler. Both the
 * construction and query steps should use them.
 *
 * These should not be externalized! The values here are used as-is in the
 * history file on disk, so they should be kept the same to keep the file format
 * compatible. If a view shows attribute names directly, the localization should
 * be done on the viewer side.
 *
 * @author Cédric Biancheri
 */
@SuppressWarnings({"nls", "javadoc"})
public interface FusedAttributes {

    /* First-level attributes */
    String CPUS = "CPUs";
    String THREADS = "Threads";
    String HOSTS = "Hosts";

    /* Sub-attributes of the CPU nodes */
    String CURRENT_THREAD = "Current_thread";
    String STATUS = "Status";
    String IRQS = "IRQs";
    String SOFT_IRQS = "Soft_IRQs";
    String CONDITION = "Condition";
    String MACHINE_NAME = "Machine_name";
    String VIRTUAL_CPU = "Virtual_cpu";

    /* Sub-attributes of the Thread nodes */
    String PPID = "PPID";
    String VTID = "VTID";
    String VPPID = "VPPID";
    //static final String STATUS = "Status"
    String EXEC_NAME = "Exec_name";
    String NS_LEVEL = "ns_level";
    String NS_INUM = "ns_inum";
    String NS_MAX_LEVEL = "ns_max_level";

    /* Sub-attributes of a Machine */
    String CONTAINERS = "Containers";
    String PCPUS = "pCPUs";

    /* Sub-attributes of a Container (also used for machines) */
    String PARENT = "Parent";

    /** @since 1.0 */
    String PRIO = "Prio";
    String SYSTEM_CALL = "System_call";

    /* Misc stuff */
    String UNKNOWN = "Unknown";
    String THREAD_0_PREFIX = "0_";
    String THREAD_0_SEPARATOR = "_";
}