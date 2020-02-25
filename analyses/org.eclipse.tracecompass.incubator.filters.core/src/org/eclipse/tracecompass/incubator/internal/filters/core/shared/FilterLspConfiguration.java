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

/**
 * Configuration of LSP server
 *
 * @author Maxime Thibault
 *
 */
public interface FilterLspConfiguration {
    /**
     * IP address to run the server
     */
    String HOSTNAME = "127.0.0.1"; //$NON-NLS-1$
    /**
     * Default port for the server
     */
    Integer PORT = 9090;
}
