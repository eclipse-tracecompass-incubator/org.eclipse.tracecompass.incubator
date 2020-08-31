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

package org.eclipse.tracecompass.incubator.internal.kernel.core.io;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * File access anaylysis
 *
 * @author Matthew Khouzam
 */
public class IoAnalysis extends TmfStateSystemAnalysisModule {

    /**
     * ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.kernel.core.io"; //$NON-NLS-1$

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        if (trace instanceof IKernelTrace) {
            return new IoStateProvider((IKernelTrace) trace);
        }
        throw new IllegalStateException("Trace " + trace + "(" + (trace == null ? "null" : trace.getClass().getCanonicalName()) + ")" + " is not of the type IKernelTrace."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }

    @Override
    public String getId() {
        return ID;
    }
}
