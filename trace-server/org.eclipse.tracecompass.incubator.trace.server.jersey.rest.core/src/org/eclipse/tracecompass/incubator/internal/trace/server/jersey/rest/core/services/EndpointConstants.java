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
    static final String BMR = "Bookmarks"; //$NON-NLS-1$
    static final String DIA = "Diagnostic"; //$NON-NLS-1$
    static final String DTR = "Data Tree"; //$NON-NLS-1$
    static final String EXP = "Experiments"; //$NON-NLS-1$
    static final String FEA = "Features"; //$NON-NLS-1$
    static final String FIL = "Filters"; //$NON-NLS-1$
    static final String STY = "Styles"; //$NON-NLS-1$
    static final String TGR = "TimeGraph"; //$NON-NLS-1$
    static final String TRA = "Traces"; //$NON-NLS-1$
    static final String VTB = "Virtual Tables"; //$NON-NLS-1$
    static final String XML = "XML"; //$NON-NLS-1$
    static final String X_Y = "XY"; //$NON-NLS-1$

    private EndpointConstants() {
        // private constructor
    }
}
