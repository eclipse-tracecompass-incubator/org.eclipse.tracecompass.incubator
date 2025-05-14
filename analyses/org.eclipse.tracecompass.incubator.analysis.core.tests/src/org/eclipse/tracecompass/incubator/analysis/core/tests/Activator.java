/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.tests;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * Activator for this plugin
 *
 * @author Geneviève Bastien
 */
public class Activator extends Plugin {
    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * The plug-in ID
     */
    public static final String PLUGIN_ID = "org.eclipse.tracecompass.incubator.analysis.core.tests"; //$NON-NLS-1$

    /**
     * The shared instance
     */
    private static Activator PLUGIN;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * The constructor
     */
    public Activator() {
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return PLUGIN;
    }

    // ------------------------------------------------------------------------
    // Operators
    // ------------------------------------------------------------------------

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        PLUGIN = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        PLUGIN = null;
        super.stop(context);
    }

    /**
     * Return a path to a file relative to this plugin's base directory
     *
     * @param relativePath
     *            The path relative to the plugin's root directory
     * @return The path corresponding to the relative path in parameter
     */
    public static IPath getAbsoluteFilePath(String relativePath) {
        Activator plugin = Activator.getDefault();
        if (plugin == null) {
            /*
             * Shouldn't happen but at least throw something to get the test to
             * fail early
             */
            throw new IllegalStateException();
        }
        URL location = FileLocator.find(plugin.getBundle(), new Path(relativePath), null);
        try {
            return new Path(FileLocator.toFileURL(location).getPath());
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }
}
