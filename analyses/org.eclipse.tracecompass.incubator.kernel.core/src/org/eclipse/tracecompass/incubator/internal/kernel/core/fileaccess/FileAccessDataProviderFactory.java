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

package org.eclipse.tracecompass.incubator.internal.kernel.core.fileaccess;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.kernel.core.io.IoAnalysis;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * {@link FileAccessDataProviderFactory} factory, uses the data provider extension
 * point.
 *
 * @author Matthew Khouzam
 */
public class FileAccessDataProviderFactory implements IDataProviderFactory {

    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(FileAccessDataProvider.ID)
            .setName("File Access By File")
            .setDescription("Shows the files in the file system and the threads who access them. This view can be filtered per thread")
            .setProviderType(ProviderType.TIME_GRAPH)
            .build();

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        IoAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, IoAnalysis.class, IoAnalysis.ID);
        if (module != null) {
            module.schedule();
            return new FileAccessDataProvider(trace, module);
        }
        return null;
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        IoAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, IoAnalysis.class, IoAnalysis.ID);
        return module != null ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }

}
