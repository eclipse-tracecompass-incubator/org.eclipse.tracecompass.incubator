/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
