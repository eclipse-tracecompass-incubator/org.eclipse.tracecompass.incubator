/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.context;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;

/**
 * Context analysis. An analysis to generate "regions of interest". These
 * regions of interest can be converted to bookmarks or other items.
 *
 * @author Matthew Khouzam
 */
public class ContextAnalysis extends TmfStateSystemAnalysisModule {

    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.context";

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new ContextStateProvider(Objects.requireNonNull(getTrace()));
    }
}
