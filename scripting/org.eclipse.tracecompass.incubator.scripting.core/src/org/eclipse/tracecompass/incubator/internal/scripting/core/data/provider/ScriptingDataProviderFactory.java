/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.core.data.provider;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Data provider factory for EASE scripting data providers. This makes them
 * available to the rest of Trace Compass through the
 * {@link IDataProviderFactory} interface. In practice, it does not create the
 * provider but rather gets from the {@link ScriptingDataProviderManager}
 * instance a data provider previously created by a script.
 *
 * @author Geneviève Bastien
 */
public class ScriptingDataProviderFactory implements IDataProviderFactory {

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        // The scripting data providers have an additional ID, this method
        // should return nothing
        return null;
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace, @NonNull String secondaryId) {
        return ScriptingDataProviderManager.getInstance().getProvider(trace, secondaryId);
    }
}
