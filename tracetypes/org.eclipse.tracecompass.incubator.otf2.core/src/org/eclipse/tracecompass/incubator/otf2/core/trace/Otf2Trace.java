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

package org.eclipse.tracecompass.incubator.otf2.core.trace;

import java.util.Collection;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.otf2.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.filter.ITmfFilter;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceContext;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

/**
 * OTF2 Traces converted into CTF format. These traces are identified using the
 * tracer_name environment variable.
 *
 * @author Yoann Heitz
 */
public class Otf2Trace extends CtfTmfTrace {

    private static final Collection<ITmfEventAspect<?>> OTF2_ASPECTS = TmfBaseAspects.getBaseAspects();

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return OTF2_ASPECTS;
    }

    private static final int CONFIDENCE = 100;

    /**
     * Constructor
     */
    public Otf2Trace() {
        super();
    }

    @Override
    public @Nullable IStatus validate(final @Nullable IProject project, final @Nullable String path) {
        IStatus status = super.validate(project, path);
        if (status instanceof CtfTraceValidationStatus) {
            Map<String, String> environment = ((CtfTraceValidationStatus) status).getEnvironment();
            String domain = environment.get("tracer_name"); //$NON-NLS-1$
            if (domain == null || !domain.equals("\"otf2\"")) { //$NON-NLS-1$
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "This trace is not an OTF2 trace"); //$NON-NLS-1$
            }
            return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
        }
        return status;
    }

    @Override
    public TmfTraceContext createTraceContext(TmfTimeRange selection, TmfTimeRange windowRange, @Nullable IFile editorFile, @Nullable ITmfFilter filter) {
        return new TmfTraceContext(selection, windowRange, editorFile, filter);
    }

}