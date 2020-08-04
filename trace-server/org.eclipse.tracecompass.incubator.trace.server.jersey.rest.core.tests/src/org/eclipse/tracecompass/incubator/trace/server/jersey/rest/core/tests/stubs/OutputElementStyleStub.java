/**********************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Stub class for the output element style elements. It matches the trace
 * server protocol's <code>OutputElementStyle</code> schema
 *
 * @author Geneviève Bastien
 */
public class OutputElementStyleStub implements Serializable {

    private static final long serialVersionUID = -456191958917557424L;
    private final String fParentKey;
    private final Map<String, Object> fStyleValues;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param parentKey
     *            The parent style's key
     * @param styleValues
     *            The additional style values
     */
    @JsonCreator
    public OutputElementStyleStub(@JsonProperty("parentKey") String parentKey,
            @JsonProperty("values") Map<String, Object> styleValues) {
        fParentKey = parentKey;
        fStyleValues = styleValues;
    }

    /**
     * Get the parent style's key
     *
     * @return The parent key
     */
    public String getParentKey() {
        return fParentKey;
    }

    /**
     * Get the style's additional values
     *
     * @return The style values
     */
    public Map<String, Object> getStyleValues() {
        return fStyleValues;
    }

}
