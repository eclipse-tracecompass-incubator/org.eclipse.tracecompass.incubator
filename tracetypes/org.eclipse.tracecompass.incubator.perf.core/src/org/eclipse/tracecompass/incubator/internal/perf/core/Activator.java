/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.core;

import org.eclipse.tracecompass.common.core.TraceCompassActivator;

/**
 * Activator for the perf.core plug-in.
 */
public class Activator extends TraceCompassActivator {

    /** The plug-in ID */
    public static final String PLUGIN_ID = "org.eclipse.tracecompass.incubator.perf.core"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public Activator() {
        super(PLUGIN_ID);
    }

    /**
     * Return the instance of this plug-in
     *
     * @return the plug-in instance
     */
    public static TraceCompassActivator getInstance() {
        return TraceCompassActivator.getInstance(PLUGIN_ID);
    }

    @Override
    protected void startActions() {
        // nothing to do
    }

    @Override
    protected void stopActions() {
        // nothing to do
    }
}
