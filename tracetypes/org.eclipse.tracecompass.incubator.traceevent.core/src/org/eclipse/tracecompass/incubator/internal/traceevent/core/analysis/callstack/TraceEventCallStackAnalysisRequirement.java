/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.callstack;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.ITraceEventConstants;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventFieldRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfCompositeAnalysisRequirement;

/**
 * Requirements to run a trace event based analysis
 *
 * @author Matthew Khouzam
 *
 */
public class TraceEventCallStackAnalysisRequirement extends TmfCompositeAnalysisRequirement {

    /**
     * Default constructor
     */
    public TraceEventCallStackAnalysisRequirement() {
        super(getSubRequirements(), PriorityLevel.AT_LEAST_ONE);
    }

    private static Collection<@NonNull TmfAbstractAnalysisRequirement> getSubRequirements() {
        TmfAnalysisEventFieldRequirement entryReq = new TmfAnalysisEventFieldRequirement(
                StringUtils.EMPTY,
                Collections.singleton(ITraceEventConstants.DURATION));
        return Collections.singleton(entryReq);
    }

}
