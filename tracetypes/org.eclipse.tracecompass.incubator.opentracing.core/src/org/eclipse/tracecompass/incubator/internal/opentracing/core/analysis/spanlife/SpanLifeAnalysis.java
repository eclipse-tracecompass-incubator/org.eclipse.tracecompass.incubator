/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Spans life tracker
 *
 * @author Katherine Nadeau
 */
public class SpanLifeAnalysis extends TmfStateSystemAnalysisModule {

    /**
     * ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.opentracing.analysis.spanlife"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public SpanLifeAnalysis() {
        setId(ID);
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        return new SpanLifeStateProvider(Objects.requireNonNull(trace));
    }

}
