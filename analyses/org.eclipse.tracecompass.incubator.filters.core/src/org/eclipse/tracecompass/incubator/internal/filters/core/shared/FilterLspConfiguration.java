/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
