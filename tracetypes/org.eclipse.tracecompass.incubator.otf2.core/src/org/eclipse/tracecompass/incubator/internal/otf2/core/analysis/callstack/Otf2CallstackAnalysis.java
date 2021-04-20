/**********************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.callstack;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.AbstractOtf2Analysis;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.otf2.core.trace.Otf2Trace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * OTF2 callstack analysis
 *
 * @author Yoann Heitz
 *
 */
public class Otf2CallstackAnalysis extends InstrumentedCallStackAnalysis {

    private static final String ID_SUFFIX = ".callstack"; //$NON-NLS-1$

    @Override
    public String getId() {
        return AbstractOtf2Analysis.getAnalysisIdFromSuffix(ID_SUFFIX);
    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = Objects.requireNonNull(getTrace());
        if (trace instanceof Otf2Trace) {
            return new Otf2CallStackStateProvider(trace);
        }
        throw new IllegalStateException("Trace is not an OTF2 trace"); //$NON-NLS-1$
    }

    @Override
    protected Collection<Integer> getEdgeQuarks() {
        ITmfStateSystem ss = getStateSystem();
        if (ss == null) {
            return Collections.emptyList();
        }
        int edgeQuark = ss.optQuarkAbsolute(IOtf2Constants.EDGES);
        if (edgeQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return Collections.emptyList();
        }
        return ss.getSubAttributes(edgeQuark, false);
    }
}