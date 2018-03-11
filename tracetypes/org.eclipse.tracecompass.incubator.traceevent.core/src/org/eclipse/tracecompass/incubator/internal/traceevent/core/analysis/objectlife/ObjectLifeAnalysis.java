/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.objectlife;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Object lifespan tracker
 *
 * @author Matthew Khouzam
 */
@NonNullByDefault
public class ObjectLifeAnalysis extends TmfStateSystemAnalysisModule {

    /**
     * ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.traceevent.analysis.objectlife"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public ObjectLifeAnalysis() {
        setId(ID);
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        return new ObjectLifeStateProvider(Objects.requireNonNull(trace));
    }

}
