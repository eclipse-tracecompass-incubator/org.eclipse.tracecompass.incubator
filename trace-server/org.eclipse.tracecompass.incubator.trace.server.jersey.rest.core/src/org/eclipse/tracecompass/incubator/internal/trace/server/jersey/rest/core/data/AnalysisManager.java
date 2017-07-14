/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.data;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Class to keep track of the analysis that have been run
 *
 * @author Loic Prieur-Drevon
 */
public class AnalysisManager {

    private Table<String, String, TmfAbstractAnalysisModule> fAnalysis = HashBasedTable.create();

    /**
     * Getter for an Analysis by trace name and analysis id
     *
     * @param traceName
     *            the name of the trace
     * @param id
     *            the ID from the analysis
     * @return the analysis module if it exists, else null
     */
    public TmfAbstractAnalysisModule get(String traceName, @NonNull String id) {
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
    public void put(String traceName, @NonNull String id, @NonNull TmfAbstractAnalysisModule analysis) {
        fAnalysis.put(traceName, id, analysis);
    }

    /**
     * Get the traces which have already run an analysis
     *
     * @param id
     *            Analysis module ID
     * @return a Map of trace names to modules for this type of analysis
     */
    public Map<String, TmfAbstractAnalysisModule> getEntries(@NonNull String id) {
        return fAnalysis.columnMap().get(id);
    }

}
