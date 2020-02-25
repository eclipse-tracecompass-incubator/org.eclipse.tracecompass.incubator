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

package org.eclipse.tracecompass.incubator.internal.filters.core.shared;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Interface to apply observable pattern between the lsp api and the filter box
 *
 * @author Jeremy Dube
 *
 */
public interface LspObservable {
    /**
     * Method to register an observer which will be notified when the lsp server
     * sends a change
     *
     * @param observer
     *            the observer to be registered
     */
    void register(@NonNull LspObserver observer);
}
