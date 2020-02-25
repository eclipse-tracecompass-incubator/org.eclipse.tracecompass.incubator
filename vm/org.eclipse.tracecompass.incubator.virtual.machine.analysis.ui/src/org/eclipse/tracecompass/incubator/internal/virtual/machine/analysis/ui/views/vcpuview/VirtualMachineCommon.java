/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vcpuview;

/**
 * Provides some common elements for all virtual machine views
 *
 * @author Mohamad Gebai
 */
public final class VirtualMachineCommon {

    /** Type of resources/entries */
    public static enum Type {
        /** Entries for VMs */
        VM,
        /** Entries for VCPUs */
        VCPU,
        /** Entries for Threads */
        THREAD,
        /** Null resources (filler rows, etc.) */
        NULL
    }
}
