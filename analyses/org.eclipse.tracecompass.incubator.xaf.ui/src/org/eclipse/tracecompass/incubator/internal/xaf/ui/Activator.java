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

package org.eclipse.tracecompass.incubator.internal.xaf.ui;

import java.net.URL;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    /** The plugin ID */
    public static final String PLUGIN_ID = "org.eclipse.tracecompass.incubator.xaf.ui"; //$NON-NLS-1$

    // The shared instance
    private static @Nullable Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
    }

    @Override
    public void start(@Nullable BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(@Nullable BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static @Nullable Activator getDefault() {
        return plugin;
    }

    public static @Nullable Image getImage(String url) {
        Activator currentPlugin = plugin;
        if(currentPlugin == null) {
            return null;
        }
        Bundle bundle = currentPlugin.getBundle();
        if (bundle == null) {
            return null;
        }
        String key = bundle.getSymbolicName() + "/" + url; //$NON-NLS-1$
        Image icon = currentPlugin.getImageRegistry().get(key);
        if (icon == null) {
            URL imageURL = bundle.getResource(url);
            ImageDescriptor descriptor = ImageDescriptor.createFromURL(imageURL);
            if (descriptor != null) {
                icon = descriptor.createImage();
                currentPlugin.getImageRegistry().put(key, icon);
            }
        }
        return icon;
    }
}

