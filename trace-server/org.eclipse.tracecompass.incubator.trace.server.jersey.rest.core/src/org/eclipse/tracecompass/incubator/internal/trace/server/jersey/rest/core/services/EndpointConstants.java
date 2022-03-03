/*******************************************************************************
 * Copyright (c) 2021 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import static org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils.REQUESTED_COLUMN_IDS_KEY;
import static org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils.REQUESTED_ELEMENT_KEY;
import static org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils.REQUESTED_ITEMS_KEY;
import static org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils.REQUESTED_MARKER_CATEGORIES_KEY;
import static org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils.REQUESTED_MARKER_SET_KEY;
import static org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils.REQUESTED_TABLE_COUNT_KEY;
import static org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils.REQUESTED_TABLE_INDEX_KEY;
import static org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils.REQUESTED_TIME_KEY;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Class with list of endpoint constants.
 *
 * @author Bernd Hufmann
 */
@NonNullByDefault
public class EndpointConstants {

    /** Error message returned for a request with missing parameters */
    public static final String MISSING_PARAMETERS = "Missing query parameters"; //$NON-NLS-1$

    /** Error message returned for a request with invalid parameters */
    public static final String INVALID_PARAMETERS = "Invalid query parameters"; //$NON-NLS-1$

    /** Error message returned for a request for a non-existing data provider */
    public static final String NO_PROVIDER = "Analysis cannot run"; //$NON-NLS-1$

    /** Error message returned for a request for trace that doesn't exist */
    public static final String NO_SUCH_TRACE = "No such trace"; //$NON-NLS-1$

    /** Error message returned for a request with missing output Id */
    public static final String MISSING_OUTPUTID = "Missing parameter outputId"; //$NON-NLS-1$

    /**
     * Swagger constants redefined out of TmfEventTableDataProvider non-API
     * restriction. This is unlike constants in DataProviderParameterUtils.
     */
    @SuppressWarnings("restriction")
    private static final String TABLE_SEARCH_DIRECTION_KEY = org.eclipse.tracecompass.internal.provisional.tmf.core.model.events.TmfEventTableDataProvider.TABLE_SEARCH_DIRECTION_KEY;
    @SuppressWarnings("restriction")
    private static final String TABLE_SEARCH_EXPRESSION_KEY = org.eclipse.tracecompass.internal.provisional.tmf.core.model.events.TmfEventTableDataProvider.TABLE_SEARCH_EXPRESSION_KEY;

    /**
     * Swagger OpenAPI definitions used in the related annotations from
     * {@link DataProviderService}, in order of appearance.
     */
    static final String TITLE = "Trace Server Protocol"; //$NON-NLS-1$
    static final String DESC = "Open source REST API for viewing and analyzing any type of logs or traces. Its goal is to provide models to populate views, graphs, metrics, and more to help extract useful information from traces, in a way that is more user-friendly and informative than huge text dumps."; //$NON-NLS-1$
    static final String TERMS = "https://www.eclipse.org/tracecompass/"; //$NON-NLS-1$
    static final String EMAIL = "tracecompass-dev@eclipse.org"; //$NON-NLS-1$
    static final String LICENSE = "Apache 2"; //$NON-NLS-1$
    static final String LICENSE_URL = "http://www.apache.org/licenses/"; //$NON-NLS-1$
    static final String VERSION = "0.1.0"; //$NON-NLS-1$
    static final String SERVER = "https://localhost:8080/tsp/api"; //$NON-NLS-1$

    /**
     * Swagger @Tag-s next below in alphabetical order for maintainability.
     * 3-letters so they align in {@link DataProviderService}; readability.
     */
    static final String ANN = "Annotations"; //$NON-NLS-1$
    static final String DIA = "Diagnostic"; //$NON-NLS-1$
    static final String EXP = "Experiments"; //$NON-NLS-1$
    static final String STY = "Styles"; //$NON-NLS-1$
    static final String TGR = "TimeGraph"; //$NON-NLS-1$
    static final String TRA = "Traces"; //$NON-NLS-1$
    static final String VTB = "Virtual Tables"; //$NON-NLS-1$
    static final String X_Y = "XY"; //$NON-NLS-1$

    /**
     * Swagger @Parameter description constants, named after their parameter
     * name; alphabetical order.
     */
    static final String EXP_UUID = "UUID of the experiment to query"; //$NON-NLS-1$
    static final String MARKER_SET_ID = "The optional requested marker set's id"; //$NON-NLS-1$
    static final String OUTPUT_ID = "ID of the output provider to query"; //$NON-NLS-1$
    static final String TRACE_UUID = "UUID of the trace to query"; //$NON-NLS-1$

    /**
     * Swagger @RequestBody description constants, named after their parameter
     * name, without the common 'requested' prefix; alphabetical order.
     */
    static final String COLUMNS = "When '" + REQUESTED_COLUMN_IDS_KEY + "' is absent all columns are returned. When present it is the array of requested columnIds. "; //$NON-NLS-1$ //$NON-NLS-2$
    static final String COUNT = "The '" + REQUESTED_TABLE_COUNT_KEY + "' is the number of lines that should be returned. "; //$NON-NLS-1$ //$NON-NLS-2$
    static final String DIRECTION = "Use '" + TABLE_SEARCH_DIRECTION_KEY + "' to specify search direction [NEXT, PREVIOUS]. "; //$NON-NLS-1$ //$NON-NLS-2$
    static final String DIRECTION_COUNT = "If present, '" + REQUESTED_TABLE_COUNT_KEY + "' events are returned starting from the first matching event. " + //$NON-NLS-1$ //$NON-NLS-2$
            "Matching and not matching events are returned. " + //$NON-NLS-1$
            "Matching events will be tagged. " + //$NON-NLS-1$
            "If no matches are found, an empty list will be returned."; //$NON-NLS-1$
    static final String ELEMENT = " The object '" + REQUESTED_ELEMENT_KEY + "' is the element for which the tooltip is requested."; //$NON-NLS-1$ //$NON-NLS-2$
    static final String EXPRESSIONS = "Use '" + TABLE_SEARCH_EXPRESSION_KEY + "' for search providing a map of <columnId, regular expression>. Returned lines that match the search expression will be tagged. "; //$NON-NLS-1$ //$NON-NLS-2$
    static final String INDEX = "If '" + REQUESTED_TABLE_INDEX_KEY + "' is used it is the starting index of the lines to be returned. "; //$NON-NLS-1$ //$NON-NLS-2$
    static final String ITEMS = "The array '" + REQUESTED_ITEMS_KEY + "' is the list of entryId being requested."; //$NON-NLS-1$ //$NON-NLS-2$
    static final String ITEMS_TT = "The array '" + REQUESTED_ITEMS_KEY + "' is an array with a single entryId being requested. "; //$NON-NLS-1$ //$NON-NLS-2$
    static final String ITEMS_XY = "The array '" + REQUESTED_ITEMS_KEY + "' is the list of entryId or seriesId being requested."; //$NON-NLS-1$ //$NON-NLS-2$
    static final String MARKER_CATEGORIES = "The array '" + REQUESTED_MARKER_CATEGORIES_KEY + "' is the list of requested annotation categories; if absent, all annotations are returned."; //$NON-NLS-1$ //$NON-NLS-2$
    static final String MARKER_SET = "The string '" + REQUESTED_MARKER_SET_KEY + "' is the optional requested marker set's id. "; //$NON-NLS-1$ //$NON-NLS-2$
    static final String ONE_OF = "One of '" + REQUESTED_TABLE_INDEX_KEY + "' or '" + REQUESTED_TIME_KEY + "' should be present. "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    static final String TABLE_TIMES = "If '" + REQUESTED_TIME_KEY + "' is used it should contain an array with a single timestamp. The returned lines starting at the given timestamp (or the nearest following) will be returned. "; //$NON-NLS-1$ //$NON-NLS-2$
    static final String TIMES = "The array '" + REQUESTED_TIME_KEY + "' is the explicit array of requested sample times."; //$NON-NLS-1$ //$NON-NLS-2$
    static final String TIMES_TREE = "When '" + REQUESTED_TIME_KEY + "' is absent the tree for the full range is returned. When present it specifies a range as [start, end]."; //$NON-NLS-1$ //$NON-NLS-2$
    static final String TIMES_TT = "The array '" + REQUESTED_TIME_KEY + "' is an array with a single timestamp. "; //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Swagger @RequestBody example constants, named after their parameter name,
     * without the common 'requested' prefix; alphabetical order.
     */
    static final String COLUMNS_EX = "\"" + REQUESTED_COLUMN_IDS_KEY + "\": [0, 1, 2],"; //$NON-NLS-1$ //$NON-NLS-2$
    static final String COUNT_EX = "\"" + REQUESTED_TABLE_COUNT_KEY + "\": 100,"; //$NON-NLS-1$ //$NON-NLS-2$
    static final String DIRECTION_EX = "\"" + TABLE_SEARCH_DIRECTION_KEY + "\": \"NEXT\""; //$NON-NLS-1$ //$NON-NLS-2$
    static final String ELEMENT_EX = "\"" + REQUESTED_ELEMENT_KEY + "\": {\"elementType\": \"state\", \"time\": 111100000, \"duration\": 100000}"; //$NON-NLS-1$ //$NON-NLS-2$
    static final String EXPRESSIONS_EX = "\"" + TABLE_SEARCH_EXPRESSION_KEY + "\": {\"1\": \"cpu.*\"},"; //$NON-NLS-1$ //$NON-NLS-2$
    static final String INDEX_EX = "\"" + REQUESTED_TABLE_INDEX_KEY + "\": 0,"; //$NON-NLS-1$ //$NON-NLS-2$
    static final String ITEMS_EX = "\"" + REQUESTED_ITEMS_KEY + "\": [1, 2]"; //$NON-NLS-1$ //$NON-NLS-2$
    static final String ITEMS_EX_TT = "\"" + REQUESTED_ITEMS_KEY + "\": [1],"; //$NON-NLS-1$ //$NON-NLS-2$
    static final String MARKER_CATEGORIES_EX = "\"" + REQUESTED_MARKER_CATEGORIES_KEY + "\": [\"category1\", \"category2\"]"; //$NON-NLS-1$ //$NON-NLS-2$
    static final String MARKER_SET_EX = "\"" + REQUESTED_MARKER_SET_KEY + "\": \"markerSetId\","; //$NON-NLS-1$ //$NON-NLS-2$
    static final String TIMES_EX = "\"" + REQUESTED_TIME_KEY + "\": [111200000, 111300000, 111400000, 111500000]"; //$NON-NLS-1$ //$NON-NLS-2$
    static final String TIMES_EX_TREE = "\"" + REQUESTED_TIME_KEY + "\": [111111111, 222222222]"; //$NON-NLS-1$ //$NON-NLS-2$
    static final String TIMES_EX_TT = "\"" + REQUESTED_TIME_KEY + "\": [111200000],"; //$NON-NLS-1$ //$NON-NLS-2$

    /** Swagger @ApiResponse description constants reused, or centralized. */
    static final String CANNOT_READ = "Cannot read this trace type"; //$NON-NLS-1$
    static final String CONSISTENT_PARENT = "The returned model must be consistent, parentIds must refer to a parent which exists in the model."; //$NON-NLS-1$
    static final String NAME_EXISTS = "There was already a trace with this name"; //$NON-NLS-1$
    static final String NOT_SUPPORTED = "Trace type not supported"; //$NON-NLS-1$
    static final String NO_SUCH_EXPERIMENT = "No such experiment"; //$NON-NLS-1$
    static final String PROVIDER_NOT_FOUND = "Experiment or output provider not found"; //$NON-NLS-1$
    static final String TREE_ENTRIES = "Unique entry point for output providers, to get the tree of visible entries"; //$NON-NLS-1$

    private EndpointConstants() {
        // private constructor
    }
}
