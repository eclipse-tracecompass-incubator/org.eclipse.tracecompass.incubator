/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.data;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.trace.TraceModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.xy.ITmfXYDataProvider;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Class to keep track of the analysis that have been run
 *
 * @author Loic Prieur-Drevon
 */
public class DataProviderManager {

    private @NonNull Table<TraceModel, String, ITmfXYDataProvider> fAnalysis = HashBasedTable.create();

    /**
     * Getter for an Analysis by trace name and analysis id
     *
     * @param traceName
     *            the name of the trace
     * @param id
     *            the ID from the analysis
     * @return the analysis module if it exists, else null
     */
    public ITmfXYDataProvider get(TraceModel traceName, @NonNull String id) {
        return fAnalysis.get(traceName, id);
    }

    /**
     * Put an analysis in the Analysis Manager
     *
     * @param traceName
     *            the name of the trace
     * @param id
     *            the analysis ID
     * @param analysis
     *            the analysis to add
     */
    public void put(TraceModel traceName, @NonNull String id, @NonNull ITmfXYDataProvider analysis) {
        fAnalysis.put(traceName, id, analysis);
    }

    /**
     * Get the traces which have already run an analysis
     *
     * @param id
     *            Analysis module ID
     * @return a Map of trace names to modules for this type of analysis
     */
    public @NonNull Set<TraceModel> getEntries(@NonNull String id) {
        Map<TraceModel, ITmfXYDataProvider> map = fAnalysis.columnMap().get(id);
        return map != null ? map.keySet() : Collections.emptySet();
    }

}
