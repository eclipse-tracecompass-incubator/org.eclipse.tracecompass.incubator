/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.filters.core;

import java.io.IOException;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.TraceCompassActivator;
import org.eclipse.tracecompass.incubator.internal.filters.core.server.FilterServerFactory;

/**
 * Activator
 */
public class Activator extends TraceCompassActivator {

    /** The plug-in ID */
    public static final String PLUGIN_ID = "org.eclipse.tracecompass.incubator.filters.core"; //$NON-NLS-1$

    /** The LSP server instance */
    private static @Nullable FilterServerFactory fServer;

    /**
     * The constructor
     */
    public Activator() {
        super(PLUGIN_ID);
    }

    /**
     * Returns the instance of this plug-in
     *
     * @return The plugin instance
     */
    public static TraceCompassActivator getInstance() {
        return TraceCompassActivator.getInstance(PLUGIN_ID);
    }

    @Override
    protected void startActions() {
        try {
            fServer = new FilterServerFactory();
        } catch (IOException e) {
            getInstance().logError(e.getMessage());
        }
    }

    @Override
    protected void stopActions() {
        if (fServer != null) {
            fServer.dispose();
        }
    }
}
