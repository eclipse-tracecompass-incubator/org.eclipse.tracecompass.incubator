/*******************************************************************************
 * Copyright (c) 2021 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.system.core.analsysis.httpd;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Retry connection state provider. Sees if a connection is first failed, then
 * succeeds.
 *
 * @author Matthew Khouzam
 */
public class HttpdRetryConnectionStateProvider extends AbstractTmfStateProvider {

    private static final String ID = "org.eclipse.tracecompass.incubator.internal.system.core.analsysis.httpd.HttpdRetryConnectionAnalysis"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param trace
     *            trace to use for state provider
     */
    public HttpdRetryConnectionStateProvider(ITmfTrace trace) {
        super(trace, ID);
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new HttpdRetryConnectionStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        if (!event.getName().equals("HTTPd")) { //$NON-NLS-1$
            return;
        }
        String response = event.getContent().getFieldValue(String.class, "response"); //$NON-NLS-1$
        String endpoint = event.getContent().getFieldValue(String.class, "endpoint"); //$NON-NLS-1$
        String userid = event.getContent().getFieldValue(String.class, "userid"); //$NON-NLS-1$
        ITmfStateSystemBuilder ssb = getStateSystemBuilder();
        if (response == null || ssb == null || endpoint == null || userid == null) {
            return;
        }
        long nanos = event.getTimestamp().toNanos();
        // 40x failure, like 401
        if (response.startsWith("4")) { //$NON-NLS-1$
            int ipq = ssb.getQuarkAbsoluteAndAdd(endpoint);
            StateSystemBuilderUtils.incrementAttributeInt(ssb, nanos, ipq, 1);
        }
        // succeed like 200
        if (response.startsWith("2")) { //$NON-NLS-1$
            int ipq = ssb.optQuarkAbsolute(endpoint);
            if (ipq != ITmfStateSystem.INVALID_ATTRIBUTE) {
                int sig = ssb.getQuarkRelativeAndAdd(ipq, userid);
                Object current = ssb.queryOngoing(ipq);
                ssb.modifyAttribute(nanos, 0, ipq);
                ssb.modifyAttribute(nanos, current, sig);
            }

        }
    }

}
