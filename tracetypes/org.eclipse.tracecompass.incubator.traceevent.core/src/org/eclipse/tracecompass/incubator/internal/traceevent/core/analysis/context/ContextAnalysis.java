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

    /** Analysis ID */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.context"; //$NON-NLS-1$

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new ContextStateProvider(Objects.requireNonNull(getTrace()));
    }
}
