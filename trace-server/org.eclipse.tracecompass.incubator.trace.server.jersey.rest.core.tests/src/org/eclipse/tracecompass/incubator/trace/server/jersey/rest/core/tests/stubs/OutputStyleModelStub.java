/**********************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A stub class for Style Models returned by a data provider.
 *
 * @author Bernd Hufmann
 */
public class OutputStyleModelStub implements Serializable {

    private static final long serialVersionUID = -581429991766200547L;

    private Map<String, OutputElementStyleStub> fStyles;

    /**
     * Constructor.
     *
     * @param styleMap
     *            the style map
     *
     */
    public OutputStyleModelStub(@JsonProperty("styles") Map<String, OutputElementStyleStub> styleMap) {
        fStyles = styleMap;
    }

    /**
     * Get the style map associated to this model
     *
     * @return Style map
     */
    public Map<String, OutputElementStyleStub> getStyles() {
        return fStyles;
    }

}
