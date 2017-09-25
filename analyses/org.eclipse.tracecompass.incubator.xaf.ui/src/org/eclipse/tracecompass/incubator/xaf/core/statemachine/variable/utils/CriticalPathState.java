/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsWorker;
import org.eclipse.tracecompass.common.core.NonNullUtils;

/**
 * Element representing a Critical Path state
 *
 * @author Raphaël Beamonte
 */
public class CriticalPathState extends InterruptionReason {
    private TmfEdge.EdgeType type;
    private OsWorker worker;

    /**
     * @param type
     *            The edge type of the critical path state
     * @param worker
     *            The worker related to the critical path state
     */
    public CriticalPathState(TmfEdge.EdgeType type, OsWorker worker) {
        this.type = type;
        this.worker = worker;
    }

    /**
     * @return The edge type of the critical path state
     */
    public TmfEdge.EdgeType getType() {
        return type;
    }

    /**
     * @return The worker related to the critical path state
     */
    public OsWorker getWorker() {
        return worker;
    }

    @Override
    public String getID() {
        // String id = worker.getName() + " (tid " +
        // worker.getHostThread().getTid() + ") " + type.name();
        return String.format("%s %s", worker.getName(), type.name()); //$NON-NLS-1$
    }

    @Override
    public String getTaskId() {
        return worker.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof CriticalPathState)) {
            return false;
        }

        CriticalPathState b = (CriticalPathState) o;
        return getID().equals(b.getID());
    }

    @Override
    public int hashCode() {
        int hash = 379;

        String id = getID();
        hash = 131 * hash + (id != null ? id.hashCode() : 0);

        return hash;
    }

    @Override
    public String toString() {
        return getID();
    }

    @Override
    public int compareTo(InterruptionReason ir) {
        if (ir instanceof CriticalPathState) {
            CriticalPathState cps = (CriticalPathState) ir;
            if (type == null) {
                if (cps.type == null) {
                    return 0;
                }
                return -1;
            }
            int cmp = type.compareTo(NonNullUtils.checkNotNull(cps.type));
            if (cmp == 0) {
                if (worker == null) {
                    if (cps.worker == null) {
                        return 0;
                    }
                    return -1;
                }
                // cmp =
                // worker.getHostThread().getTid().compareTo(cps.worker.getHostThread().getTid());
                // if (cmp == 0) {
                return worker.getName().compareTo(cps.worker.getName());
                // }
            }
            return cmp;
        }
        return 0;
    }
}