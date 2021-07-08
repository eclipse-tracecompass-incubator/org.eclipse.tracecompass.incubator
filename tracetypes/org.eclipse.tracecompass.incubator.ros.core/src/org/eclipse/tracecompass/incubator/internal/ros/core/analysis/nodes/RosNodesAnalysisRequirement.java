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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.nodes;

import java.util.Collection;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.incubator.internal.ros.core.trace.layout.IRosEventLayout;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventFieldRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfCompositeAnalysisRequirement;

import com.google.common.collect.ImmutableSet;

/**
 * Analysis requirement implementation for ROS Connections analysis
 *
 * @author Christophe Bedard
 */
@NonNullByDefault
public class RosNodesAnalysisRequirement extends TmfCompositeAnalysisRequirement {

    /**
     * Constructor
     *
     * @param layout
     *            the event layout
     */
    public RosNodesAnalysisRequirement(IRosEventLayout layout) {
        super(getSubRequirements(layout), PriorityLevel.AT_LEAST_ONE);
    }

    private static Collection<TmfAbstractAnalysisRequirement> getSubRequirements(IRosEventLayout layout) {
        // Requirement on init_node event
        TmfAnalysisEventRequirement initEventReq = new TmfAnalysisEventRequirement(
                ImmutableSet.of(Objects.requireNonNull(layout.eventInitNode())),
                PriorityLevel.MANDATORY);

        // Optional requirement on shutdown_node event
        TmfAnalysisEventRequirement shutdownEventReq = new TmfAnalysisEventRequirement(
                ImmutableSet.of(Objects.requireNonNull(layout.eventShutdownNode())),
                PriorityLevel.OPTIONAL);
        shutdownEventReq.addInformation("The shutdown_node event is optional because it isn't part of tracetools for the moment."); //$NON-NLS-1$

        // Requirement for shutdown_node procname field
        TmfAnalysisEventFieldRequirement shutdownProcnameReq = new TmfAnalysisEventFieldRequirement(
                Objects.requireNonNull(layout.eventShutdownNode()),
                ImmutableSet.of(Objects.requireNonNull(layout.contextProcname())),
                PriorityLevel.MANDATORY);

        return ImmutableSet.of(initEventReq, shutdownEventReq, shutdownProcnameReq);
    }
}
