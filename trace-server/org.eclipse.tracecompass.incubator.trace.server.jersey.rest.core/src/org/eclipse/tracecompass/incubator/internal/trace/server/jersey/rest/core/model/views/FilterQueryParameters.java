/**********************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Support search and filter expressions for timegraph views
 *
 * @author Hriday Panchasara
 */
public class FilterQueryParameters {

    /**
     * Strategy of search.
    */
    public enum FilterQueryStrategy {
        /** Sampled (Default search) */
        SAMPLED,
        /** Deep (Full search) */
        DEEP
    }

    /**
     * Optional parameter that enables the full search (deep search) or not.
     * If set to 'deep' then the data provider is expected to perform a
     * full search of elements that occur between the usual elements found
     * at the requested times (e.g. in gaps). If any element matches the
     * specified regex filter, at least one such matching element per gap
     * should be returned. It is not expected to return all matching elements
     * found in a single gap, for performance reasons. If set to 'sampled' then
     * the gaps in between samples of the requested times are not searched.
     */
    private FilterQueryStrategy strategy;

    /**
    * Map of filter expressions.
    * Key is a CoreFilterProperty, value is an array of the desired search query.
    * E.g. {"1": ["openat", "duration>10ms"]}.
    */
    private Map<Integer, Collection<String>> filterExpressionsMap;

    /**
     * Default Constructor
     */
    public FilterQueryParameters() {
        this.strategy = FilterQueryStrategy.SAMPLED;
        this.filterExpressionsMap = new HashMap<>();
    }

    /**
     * Setter for strategy
     *
     * @param strategy
     *              the search strategy for the filter expressions.
     */
    public void setStrategy(FilterQueryStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Setter for the filter expressions map
     *
     * @param filterExpressionsMap
     *              the filter expressions for a timegraph view.
     */
    public void setFilterExpressionsMap(Map<Integer, Collection<String>> filterExpressionsMap) {
        this.filterExpressionsMap = filterExpressionsMap;
    }

    /**
     * @return A boolean value isDeepSearch
     */
    @Hidden
    public Boolean isDeepSearch() {
        return strategy != null && strategy.equals(FilterQueryStrategy.DEEP);
    }

    /**
     * @return Map of filter expressions
     */
    public Map<Integer, Collection<String>> getFilterExpressionsMap() {
        return filterExpressionsMap;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        return "{\"strategy\":" + strategy + ", \"filter_expressions_map\":" + filterExpressionsMap + "}";
    }
}