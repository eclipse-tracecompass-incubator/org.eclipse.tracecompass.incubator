/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.context;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * {@link ContextDataProviderFactory} factory, uses the data provider extension
 * point.
 *
 * @author Matthew Khouzam
 */
public class ContextDataProviderFactory implements IDataProviderFactory {

    @Override
    public @Nullable ContextDataProvider createProvider(@NonNull ITmfTrace trace) {
        ContextAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, ContextAnalysis.class, ContextAnalysis.ID);
        if (module != null) {
            module.schedule();
            return new ContextDataProvider(trace, module);
        }

        return null;
    }

}
