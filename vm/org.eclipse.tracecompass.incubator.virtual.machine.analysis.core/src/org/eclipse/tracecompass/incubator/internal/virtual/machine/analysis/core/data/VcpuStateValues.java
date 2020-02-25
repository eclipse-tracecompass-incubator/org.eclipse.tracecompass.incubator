/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Mohamad Gebai - Initial API and implementation
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.data;

/**
 * State system values used by the VM analysis
 *
 * @author Mohamad Gebai
 */
public interface VcpuStateValues {

    /* VCPU Status */
    /** The virtual CPU state is unknown */
    int VCPU_UNKNOWN = 0;
    /** The virtual CPU is idle */
    int VCPU_IDLE = 1;
    /** The virtual CPU is running */
    int VCPU_RUNNING = 2;
    /** Flag for when the virtual CPU is in hypervisor mode */
    int VCPU_VMM = 128;
    /** Flag for when the virtual CPU is preempted */
    int VCPU_PREEMPT = 256;

}
