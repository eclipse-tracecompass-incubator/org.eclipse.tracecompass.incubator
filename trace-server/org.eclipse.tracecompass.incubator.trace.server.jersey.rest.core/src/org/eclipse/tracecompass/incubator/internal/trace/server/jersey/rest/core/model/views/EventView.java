/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Encapsulate the result of a virtual table query
 *
 * @author Loic Prieur-Drevon
 */
@XmlRootElement
public class EventView {

    private ITmfTrace fTrace;
    private List<List<String>> fLines;
    private List<String> fColumns;
    private long fLow;
    private int fSize;
    private Map<String, String> fFilters;
    private long fFilteredSize;

    /**
     * empty constructor for Jackson
     */
    public EventView() {
    }

    /**
     * Build an EventView from the Trace that is queried and the queried range
     *
     * @param trace
     *            the queried trace
     * @param low
     *            lower bound for the query
     * @param size
     *            number of events to return
     * @param lines
     *            the array of event view values to include
     */
    public EventView(@NonNull ITmfTrace trace, long low, int size, List<List<String>> lines) {
        fTrace = trace;
        fColumns = Lists.newArrayList(Iterables.transform(trace.getEventAspects(), ITmfEventAspect::getName));
        fLow = low;
        fSize = size;
        fLines = lines;
        fFilters = Collections.emptyMap();
        fFilteredSize = trace.getNbEvents();
    }

    /**
     * Build an EventView from the Trace that is queried and the queried range
     *
     * @param trace
     *            the queried trace
     * @param low
     *            lower bound for the query
     * @param size
     *            number of events to return
     * @param filters
     *            the columns which will be queried, if null, the field will be
     *            populated by the trace's columns
     * @param lines
     *            the array of event view values to include
     * @param filteredSize
     *            the number of events that match the filters
     */
    public EventView(@NonNull ITmfTrace trace, long low, int size, MultivaluedMap<String, String> filters, List<List<String>> lines, long filteredSize) {
        fTrace = trace;
        fColumns = Lists.newArrayList(Iterables.transform(trace.getEventAspects(), ITmfEventAspect::getName));
        fLow = low;
        fSize = size;
        fFilteredSize = filteredSize;
        fLines = lines;
        fFilters = multiValuedMapToMap(filters);
    }

    /**
     * Getter for the trace
     *
     * @return this query's trace
     */
    @XmlElement
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Getter for the events.
     *
     * @return a list of the view of events returned by the query, from low to low +
     *         size, with a Map of column names to column values.
     */
    @XmlElementWrapper(name = "events")
    @XmlElement(name = "event")
    public List<List<String>> getLines() {
        return fLines;
    }

    /**
     * Getter for the column names
     *
     * @return get the list of column names
     */
    @XmlElement
    public List<String> getColumns() {
        return fColumns;
    }

    /**
     * Getter for the lower bound of the query
     *
     * @return the rank of the lowest event in this view
     */
    @XmlElement
    public long getLow() {
        return fLow;
    }

    /**
     * Getter for the number of events in this query
     *
     * @return the number of events in this query
     */
    @XmlElement
    public int getSize() {
        return fSize;
    }

    /**
     * Getter for the filter, for statelessness
     *
     * @return the columns which were queried
     */
    @XmlElement
    public Map<String, String> getFilters() {
        return fFilters;
    }

    /**
     * The total number of events in this trace after applying the filters
     *
     * @return number of filtered events in this trace
     */
    @XmlElement
    public long getFilteredSize() {
        return fFilteredSize;
    }

    private static Map<String, String> multiValuedMapToMap(MultivaluedMap<String, String> multivaluedMap) {
        Map<String, String> map = new HashMap<>();
        for (String key : multivaluedMap.keySet()) {
            String value = multivaluedMap.getFirst(key);
            if (value != null) {
                map.put(key, value);
            }
        }
        return map;
    }
}
