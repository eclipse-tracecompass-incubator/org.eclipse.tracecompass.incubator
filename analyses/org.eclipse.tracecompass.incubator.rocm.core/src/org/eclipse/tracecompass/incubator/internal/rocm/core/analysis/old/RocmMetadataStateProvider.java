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

package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.rocm.core.trace.old.RocmTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * This state provider stores each function name in the state system ordered
 * with the function id.
 *
 * Attribute tree:
 *
 * <pre>
 * |- Function names -> begins at the start of the trace, each ns is a different function name.
 * </pre>
 *
 * @author Arnaud Fiorini
 */
public class RocmMetadataStateProvider extends AbstractTmfStateProvider {

    private static final String ID = "org.eclipse.tracecompass.incubator.rocm.core.stateprovider.functionname"; //$NON-NLS-1$

    /**
     * Attribute name for the function name map.
     */
    public static final String FUNCTION_NAMES = "Function Names"; //$NON-NLS-1$

    /**
     * @param trace
     *            the trace to analyze
     */
    public RocmMetadataStateProvider(@NonNull ITmfTrace trace) {
        super(trace, ID);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new RocmCallStackStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        if (event.getName().endsWith("function_name")) { //$NON-NLS-1$
            ITmfStateSystemBuilder ssb = getStateSystemBuilder();
            if (ssb == null) {
                return;
            }
            int functionNameQuark = ssb.getQuarkAbsoluteAndAdd(FUNCTION_NAMES);
            int apiQuark = ssb.getQuarkRelativeAndAdd(functionNameQuark,
                    ((Integer) ((RocmTrace) event.getTrace()).getApiId(event.getName().split("_")[0] + "_api")).toString()); //$NON-NLS-1$ //$NON-NLS-2$
            String functionName = event.getContent().getFieldValue(String.class, RocmStrings.NAME);
            Integer cid = event.getContent().getFieldValue(Integer.class, RocmStrings.CORRELATION_ID);
            if (functionName == null || cid == null) {
                return;
            }
            ssb.modifyAttribute(ssb.getStartTime() + cid, functionName, apiQuark);
        }
    }

    /**
     * Static function to get the function id from an api event cid.
     *
     * @param event
     *            An API event
     * @return functionId
     */
    public static int getFunctionId(@NonNull ITmfEvent event) {
        int nApi = ((RocmTrace) event.getTrace()).getNApi();
        Integer cid = event.getContent().getFieldValue(Integer.class, RocmStrings.CID);
        if (cid == null) {
            cid = event.getContent().getFieldValue(Integer.class, RocmStrings.CORRELATION_ID);
        }
        if (cid == null) {
            return -1;
        }
        int apiId;
        if (event.getName().endsWith("function_name")) { //$NON-NLS-1$
            apiId = ((RocmTrace) event.getTrace()).getApiId(event.getName().split("_")[0] + "_api"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            apiId = ((RocmTrace) event.getTrace()).getApiId(event.getName());
        }
        return cid * nApi + apiId;
    }
}
