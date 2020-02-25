/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracecompass.core.trace;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.tracecompass.core.Activator;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

/**
 * @author Geneviève Bastien
 */
public class LttngTraceCompassJulTrace extends CtfTmfTrace {

    private static final int CONFIDENCE = 101;
    /**
     * UST domain event created by a Java application.
     */
    private static final String LTTNG_JUL_EVENT = "lttng_jul:event"; //$NON-NLS-1$

    /**
     * Default constructor
     */
    public LttngTraceCompassJulTrace() {
        super(LttngTraceCompassJulEventFactory.instance());

    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation sets the confidence to 100 if the trace is a valid
     * CTF trace in the "ust" domain.
     */
    @Override
    public IStatus validate(final @Nullable IProject project, final @Nullable String path) {
        IStatus status = super.validate(project, path);
        if (status instanceof CtfTraceValidationStatus) {
            Map<String, String> environment = ((CtfTraceValidationStatus) status).getEnvironment();
            /* Make sure the domain is "ust" in the trace's env vars */
            String domain = environment.get("domain"); //$NON-NLS-1$
            if (domain == null || !domain.equals("\"ust\"")) { //$NON-NLS-1$
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a UST trace, so not a JUL trace"); //$NON-NLS-1$
            }

            // Augment the confidence if there is only one event that is a jul
            // event
            Collection<String> eventNames = ((CtfTraceValidationStatus) status).getEventNames();
            if (eventNames.size() == 1) {
                if (LTTNG_JUL_EVENT.equals(eventNames.iterator().next())) {
                    return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
                }
            }
        }
        return status;
    }

//    @Override
//    protected int getAverageEventSize() {
//        return 300;
//    }
}
