/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.trace.TraceModel;

/**
 * Object to inject to store trace name to model mapping
 *
 * @author Loic Prieur-Drevon
 *
 */
public class TraceManager {

    private final Map<String, TraceModel> fMap = new HashMap<>();

    private static TraceManager fInstance = null;

    private TraceManager() {
    }

    /**
     * Gets the Trace Manager instance
     *
     * @return the Trace manager instance
     */
    public static TraceManager getInstance() {
        TraceManager instance = fInstance;
        if (fInstance == null) {
            instance = new TraceManager();
            fInstance = instance;
        }
        return instance;
    }

    /**
     * Gets the Trace Model
     *
     * @param name
     *            the name of the trace
     * @return the trace model instance
     *
     */
    public @Nullable TraceModel get(String name) {
        return fMap.get(name);
    }

    /**
     * Gets all the trace models
     *
     * @return trace models
     */
    public Collection<TraceModel> getTraces() {
        return fMap.values();
    }

    /**
     * Adds a trace model
     *
     * @param name
     *            the name of the trace
     * @param model
     *            the model to add
     */
    public void put(String name, TraceModel model) {
        fMap.put(name, model);
    }

    /**
     * Removes a trace model
     *
     * @param name
     *            the name of the trace
     *
     * @return the removed model or null
     */
    public @Nullable TraceModel remove(String name) {
        TraceModel model = fMap.remove(name);
        if (model != null) {
            model.dispose();
        }
        return model;
    }
}
