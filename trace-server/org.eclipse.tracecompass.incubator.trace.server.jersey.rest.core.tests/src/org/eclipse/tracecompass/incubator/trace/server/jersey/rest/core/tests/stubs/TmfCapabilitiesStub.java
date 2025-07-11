/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic Implementation of the serialized capabilities model used by clients.
 *
 * @author Bernd Hufmann
 */
public class TmfCapabilitiesStub {

    private final boolean fCanCreate;
    private final boolean fCanDelete;

    /**
     * Constructor
     *
     * @param canCreate
     *            canCreate capability
     * @param canDelete
     *            canDelete capability
     */
    public TmfCapabilitiesStub(@JsonProperty("canCreate") Boolean canCreate,
            @JsonProperty("canDelete") Boolean canDelete) {
        fCanCreate = canCreate == null ? false : canCreate;
        fCanDelete = canDelete == null ? false : canDelete;
    }

    /**
     * @return the canCreate capability
     */
    public boolean getCanCreate() {
        return fCanCreate;
    }

    /**
     * @return the canDelete capability
     */
    public boolean getCanDelete() {
        return fCanDelete;
    }
}
