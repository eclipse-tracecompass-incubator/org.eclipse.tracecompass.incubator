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

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.system.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Connection State Analysis provider
 *
 * Tree:
 *
 * <pre>
 * root
 *  |- ip                       - Cumulative data sent
 *  |   | 192.128.0.1           - Cumulative data sent
 *  |   |   +- /home            - Cumulative data sent
 *  |   |   \- /other           - Cumulative data sent
 *  |   \ ...                   - Cumulative data sent
 *  +- endpoint                 - Cumulative data sent
 *  |   | /home                 - Cumulative data sent
 *  |   | /other                - Cumulative data sent
 *  |   \ ...                   - Cumulative data sent
 *  \- user                     - Cumulative data sent
 *      | vivek                 - Cumulative data sent
 *      |   +- /home            - Cumulative data sent
 *      |   \- /other           - Cumulative data sent
 *      \ ...                   - Cumulative data sent
 * </pre>
 *
 * @author Matthew Khouzam
 */
public class HttpdConnectionStateProvider extends AbstractTmfStateProvider {

    private static final String ID = "org.eclipse.tracecompass.incubator.internal.system.core.analsysis.httpd.HttpdConnectionAnalysis"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param trace
     *            the trace of the state provider
     */
    public HttpdConnectionStateProvider(ITmfTrace trace) {
        super(trace, ID);
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new HttpdConnectionStateProvider(Objects.requireNonNull(getTrace()));
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ssb = getStateSystemBuilder();
        if (!event.getName().equals("HTTPd") || ssb == null) { //$NON-NLS-1$
            return;
        }
        String size = event.getContent().getFieldValue(String.class, "size (bytes)"); //$NON-NLS-1$
        String endpoint = event.getContent().getFieldValue(String.class, "endpoint"); //$NON-NLS-1$
        String ip = event.getContent().getFieldValue(String.class, "IP"); //$NON-NLS-1$
        String user = event.getContent().getFieldValue(String.class, "userid"); //$NON-NLS-1$
        if (size == null || endpoint == null || ip == null || user == null || size.equals("-")) { //$NON-NLS-1$
            return;
        }
        int rootQuark = ITmfStateSystem.ROOT_ATTRIBUTE;
        try {
            long bytes = Long.parseLong(size);
            int ipQuark = ssb.getQuarkRelativeAndAdd(rootQuark, "ip", ip); //$NON-NLS-1$
            int endpointQuark = ssb.getQuarkRelativeAndAdd(rootQuark, "endpoint", endpoint); //$NON-NLS-1$
            int ipEndpointQuark = ssb.getQuarkRelativeAndAdd(ipQuark, endpoint);
            int userIdQuark = ssb.getQuarkRelativeAndAdd(rootQuark, "userid", user); //$NON-NLS-1$
            int userEndpointQuark = ssb.getQuarkRelativeAndAdd(userIdQuark, endpoint);
            long nanos = event.getTimestamp().toNanos();
            StateSystemBuilderUtils.incrementAttributeLong(ssb, nanos, ipQuark, bytes);
            StateSystemBuilderUtils.incrementAttributeLong(ssb, nanos, endpointQuark, bytes);
            StateSystemBuilderUtils.incrementAttributeLong(ssb, nanos, userIdQuark, bytes);
            StateSystemBuilderUtils.incrementAttributeLong(ssb, nanos, userEndpointQuark, bytes);
            StateSystemBuilderUtils.incrementAttributeLong(ssb, nanos, ipEndpointQuark, bytes);
        } catch (NumberFormatException e) {
            Activator.getInstance().logInfo("Failed to convert ", e); //$NON-NLS-1$
        }
    }
}
