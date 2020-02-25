/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.filters.ui.lspFilterTextbox;

/**
 * Interface to define a valid string event
 *
 * @author Jeremy Dube
 *
 */
public interface FilterValidityListener {
    /**
     * Called when the string is valid
     */
    void validFilter();

    /**
     * Called when the string is invalid
     */
    default void invalidFilter() {
        // Do nothing
    }
}
