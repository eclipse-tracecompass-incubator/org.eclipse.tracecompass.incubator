/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.connections;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.incubator.internal.ros.core.trace.layout.IRosEventLayout;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfCompositeAnalysisRequirement;

import com.google.common.collect.ImmutableSet;

/**
 * Analysis requirement implementation for ROS Connections analysis
 *
 * @author Christophe Bedard
 */
@NonNullByDefault
public class RosConnectionsAnalysisRequirement extends TmfCompositeAnalysisRequirement {

    /**
     * Constructor
     *
     * @param layout
     *            the event layout
     */
    public RosConnectionsAnalysisRequirement(IRosEventLayout layout) {
        super(getSubRequirements(layout), PriorityLevel.MANDATORY);
    }

    private static Collection<TmfAbstractAnalysisRequirement> getSubRequirements(IRosEventLayout layout) {
        // Requirement on new_connection event
        TmfAnalysisEventRequirement initEventReq = new TmfAnalysisEventRequirement(
                ImmutableSet.of(checkNotNull(layout.eventNewConnection())),
                PriorityLevel.MANDATORY);

        return ImmutableSet.of(initEventReq);
    }
}
