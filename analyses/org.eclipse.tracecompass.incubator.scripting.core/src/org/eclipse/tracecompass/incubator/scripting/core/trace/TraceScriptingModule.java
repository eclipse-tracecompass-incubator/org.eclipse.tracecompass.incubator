/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.trace;

import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/**
 * Scripting modules to open and manipulate traces
 *
 * @author Benjamin Saint-Cyr
 */
public class TraceScriptingModule {

    /**
     * Get the currently active trace, ie the last trace opened in the UI
     *
     * @return The current trace or <code>null</code> if no trace is opened
     */
    @WrapToScript
    public @Nullable ITmfTrace getActiveTrace() {
        return TmfTraceManager.getInstance().getActiveTrace();
    }
}
