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
public final class EndpointConstants {

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

    private EndpointConstants() {
        // private constructor
    }
}
