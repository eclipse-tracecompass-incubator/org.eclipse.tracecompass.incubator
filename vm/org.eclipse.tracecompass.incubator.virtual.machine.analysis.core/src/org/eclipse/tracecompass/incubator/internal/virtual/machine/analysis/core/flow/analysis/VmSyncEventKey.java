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

import java.util.Objects;

import org.eclipse.tracecompass.tmf.core.event.matching.IEventMatchingKey;

/**
 * Event matching key for VmSync events
 *
 * @author Francois Belias
 */
public class VmSyncEventKey implements IEventMatchingKey {

    private final Long fCnt;
    private final Long fVmUid;
    private final int fHashCode;

    /**
     * Constructor
     *
     * @param cnt The counter value
     * @param vmUid The VM unique identifier
     */
    public VmSyncEventKey(Long cnt, Long vmUid) {
        fCnt = cnt;
        fVmUid = vmUid;
        fHashCode = Objects.hash(fCnt, fVmUid);
    }

    @Override
    public int hashCode() {
        return fHashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VmSyncEventKey)) {
            return false;
        }
        VmSyncEventKey key = (VmSyncEventKey) o;
        return Objects.equals(fCnt, key.fCnt) &&
               Objects.equals(fVmUid, key.fVmUid);
    }

    @Override
    public String toString() {
        return "VmSyncKey: cnt=" + fCnt + ", vm_uid=" + fVmUid; //$NON-NLS-1$ //$NON-NLS-2$
    }
}