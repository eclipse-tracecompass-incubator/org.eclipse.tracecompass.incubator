/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.core.data.provider;

import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * A class to manage the data providers create via scripts for traces. The data
 * providers will be created by the scripting modules and registered to this
 * manager.
 *
 * @author Geneviève Bastien
 */
public class ScriptingDataProviderManager {

    /**
     * ID shared by all scripting data providers
     */
    public static final String PROVIDER_ID = "org.eclipse.tracecompass.incubator.scripting.dataprovider"; //$NON-NLS-1$

    private static @Nullable ScriptingDataProviderManager INSTANCE;

    private final Multimap<ITmfTrace, ITmfTreeDataProvider<? extends ITmfTreeDataModel>> fInstances = HashMultimap.create();

    /**
     * Get the instance of the manager
     *
     * @return the singleton instance
     */
    public synchronized static ScriptingDataProviderManager getInstance() {
        ScriptingDataProviderManager instance = INSTANCE;
        if (instance == null) {
            instance = new ScriptingDataProviderManager();
            INSTANCE = instance;
        }
        return instance;
    }

    /**
     * Dispose the singleton instance if it exists
     */
    public static synchronized void dispose() {
        ScriptingDataProviderManager manager = INSTANCE;
        if (manager != null) {
            TmfSignalManager.deregister(manager);
            manager.fInstances.clear();
            INSTANCE = null;
        }
    }

    /**
     * Private constructor.
     */
    public ScriptingDataProviderManager() {
        TmfSignalManager.register(this);
    }

    /**
     * Get a data provider with ID for trace
     *
     * @param trace
     *            The trace to get the provider for
     * @param id
     *            The ID of the data provider
     * @return The data provider, or <code>null</code> if none has been created
     *         for the trace
     */
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> getProvider(ITmfTrace trace, String id) {
        Collection<ITmfTreeDataProvider<? extends ITmfTreeDataModel>> dps = fInstances.get(trace);
        for (ITmfTreeDataProvider<? extends ITmfTreeDataModel> dataProvider : dps) {
            if (dataProvider.getId().equals(PROVIDER_ID + ':' + id)) {
                return dataProvider;
            }
        }
        return null;
    }

    /**
     * Register a data provider that was created by a script
     *
     * @param trace
     *            The trace this data provider is for
     * @param provider
     *            The data provider
     */
    public void registerDataProvider(ITmfTrace trace, ITmfTreeDataProvider<? extends ITmfTreeDataModel> provider) {
        fInstances.put(trace, provider);
    }

    /**
     * Signal handler for the traceClosed signal.
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void traceClosed(final TmfTraceClosedSignal signal) {
        new Thread(() -> {
            synchronized (ScriptingDataProviderManager.this) {
                for (ITmfTrace trace : TmfTraceManager.getTraceSetWithExperiment(signal.getTrace())) {
                    fInstances.removeAll(trace).forEach(ITmfTreeDataProvider::dispose);
                }
            }
        }).start();
    }

}
