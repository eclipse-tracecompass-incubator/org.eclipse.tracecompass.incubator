/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.dependency;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.incubator.callstack.core.base.EdgeStateValue;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmStrings;
import org.eclipse.tracecompass.incubator.rocm.core.analysis.dependency.IDependencyMaker;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;

/**
 * This dependency maker adds default methods for dependencies
 *
 * @author Arnaud Fiorini
 */
public abstract class AbstractDependencyMaker implements IDependencyMaker {

    /**
     * DependencyMake constructor
     */
    protected AbstractDependencyMaker() {
    }

    /**
     * Creates and adds an arrow to the state system
     *
     * @param ssb
     *            The state system to add the arrows
     * @param startTime
     *            The start time of the arrow
     * @param endTime
     *            The end time of the arrow
     * @param id
     *            The id to identify the arrow
     * @param src
     *            The source hostThread
     * @param dest
     *            The destination hostThread
     */
    public static void addArrow(ITmfStateSystemBuilder ssb, Long startTime, Long endTime, int id,
            @NonNull HostThread src, @NonNull HostThread dest) {
        int edgeQuark = getAvailableEdgeQuark(ssb, startTime);
        Object edgeStateValue = new EdgeStateValue(id, src, dest);
        ssb.modifyAttribute(startTime, edgeStateValue, edgeQuark);
        ssb.modifyAttribute(endTime, (Object) null, edgeQuark);
    }

    private static int getAvailableEdgeQuark(ITmfStateSystemBuilder ssb, long startTime) {
        int edgeRoot = ssb.getQuarkAbsoluteAndAdd(RocmStrings.EDGES);
        List<@NonNull Integer> subQuarks = ssb.getSubAttributes(edgeRoot, false);

        for (int quark : subQuarks) {
            long start = ssb.getOngoingStartTime(quark);
            Object value = ssb.queryOngoing(quark);
            if (value == null && start <= startTime) {
                return quark;
            }
        }
        return ssb.getQuarkRelativeAndAdd(edgeRoot, Integer.toString(subQuarks.size()));
    }
}
