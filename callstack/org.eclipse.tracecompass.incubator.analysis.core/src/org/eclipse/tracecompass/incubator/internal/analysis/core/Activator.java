/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.analysis.core;

import org.eclipse.tracecompass.common.core.TraceCompassActivator;
import org.eclipse.tracecompass.incubator.analysis.core.aspects.AnalysisPidAspect;
import org.eclipse.tracecompass.incubator.analysis.core.aspects.AnalysisProcessNameAspect;
import org.eclipse.tracecompass.incubator.analysis.core.aspects.AnalysisThreadNameAspect;
import org.eclipse.tracecompass.incubator.analysis.core.aspects.AnalysisTidAspect;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Activator
 */
public class Activator extends TraceCompassActivator {

    /** The plug-in ID */
    public static final String PLUGIN_ID = "org.eclipse.tracecompass.incubator.analysis.core"; //$NON-NLS-1$

    /**
     * The constructor
     */
    public Activator() {
        super(PLUGIN_ID);
    }

    /**
     * Returns the instance of this plug-in
     *
     * @return The plugin instance
     */
    public static TraceCompassActivator getInstance() {
        return TraceCompassActivator.getInstance(PLUGIN_ID);
    }

    @Override
    protected void startActions() {
        TmfTraceUtils.registerEventAspect(AnalysisTidAspect.getInstance());
        TmfTraceUtils.registerEventAspect(AnalysisProcessNameAspect.getInstance());
        TmfTraceUtils.registerEventAspect(AnalysisThreadNameAspect.getInstance());
        TmfTraceUtils.registerEventAspect(AnalysisPidAspect.getInstance());
    }

    @Override
    protected void stopActions() {
        ModelManager.disposeModels();
    }

}

