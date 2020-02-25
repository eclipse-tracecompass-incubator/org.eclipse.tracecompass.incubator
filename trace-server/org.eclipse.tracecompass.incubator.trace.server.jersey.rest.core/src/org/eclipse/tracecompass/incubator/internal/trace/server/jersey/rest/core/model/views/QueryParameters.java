/**********************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views;

import java.util.List;
import java.util.Map;

/**
 * Definition of a parameters object received by the server from a client.
 *
 * @author Simon Delisle
 */
public class QueryParameters {
    private Map<String, Object> parameters;
    private List<Filter> filters;

    /**
     * Constructor for Jackson
     */
    public QueryParameters() {
        // Default constructor for Jackson
    }

    /**
     * Constructor.
     *
     * @param parameters
     *            Map of parameters
     * @param filters
     *            List of filters
     */
    public QueryParameters(Map<String, Object> parameters, List<Filter> filters) {
        this.parameters = parameters;
        this.filters = filters;
    }

    /**
     * @return Map of parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * @return List of filters
     */
    public List<Filter> getFilters() {
        return filters;
    }
}
