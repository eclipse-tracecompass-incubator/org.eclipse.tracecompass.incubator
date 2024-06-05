/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal, Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparison.ui;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Activator class to controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    /** The plugin ID */
    public static final String PLUGIN_ID = "org.eclipse.tracecompass.incubator.executioncomparison.ui"; //$NON-NLS-1$

    // The shared instance
    private static @Nullable Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
        // Do nothing
    }

    @Override
    public void start(@Nullable BundleContext context) throws Exception {
        super.start(context);
        setDefault(this);
    }

    @Override
    public void stop(@Nullable BundleContext context) throws Exception {
        setDefault(null);
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return Objects.requireNonNull(plugin);
    }

    private static void setDefault(@Nullable Activator activator) {
        plugin = activator;
    }

}
