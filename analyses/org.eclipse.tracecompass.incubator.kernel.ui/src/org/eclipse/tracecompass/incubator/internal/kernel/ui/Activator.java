/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.ui;

import java.util.Objects;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    /** The plugin ID */
    public static final String PLUGIN_ID = "org.eclipse.tracecompass.incubator.kernel.ui"; //$NON-NLS-1$

    // The shared instance
    private static @Nullable Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
        // Do nothing
    }

    /**
     * Gets an image object using given path within plug-in.
     *
     * @param path
     *            path to image file
     *
     * @return image object
     */
    public @Nullable Image getImageFromPath(String path) {
        ImageDescriptor imageDescripterFromPath = getImageDescripterFromPath(path);
        return imageDescripterFromPath == null? null : imageDescripterFromPath.createImage();
    }

    /**
     * Gets an image descriptor using given path within plug-in.
     *
     * @param path
     *            path to image file
     *
     * @return image descriptor object
     */
    public @Nullable ImageDescriptor getImageDescripterFromPath(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
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
    public static Activator getDefault() {
        return Objects.requireNonNull(plugin);
    }

    /**
     * Logs a message with severity INFO in the runtime log of the plug-in.
     *
     * @param message
     *            A message to log
     */
    public void logInfo(String message) {
        getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
    }

    /**
     * Logs a message and exception with severity INFO in the runtime log of the
     * plug-in.
     *
     * @param message
     *            A message to log
     * @param exception
     *            A exception to log
     */
    public void logInfo(String message, Throwable exception) {
        getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message, exception));
    }

    /**
     * Logs a message and exception with severity WARNING in the runtime log of
     * the plug-in.
     *
     * @param message
     *            A message to log
     */
    public void logWarning(String message) {
        getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }

    /**
     * Logs a message and exception with severity WARNING in the runtime log of
     * the plug-in.
     *
     * @param message
     *            A message to log
     * @param exception
     *            A exception to log
     */
    public void logWarning(String message, Throwable exception) {
        getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message, exception));
    }

    /**
     * Logs a message and exception with severity ERROR in the runtime log of
     * the plug-in.
     *
     * @param message
     *            A message to log
     */
    public void logError(String message) {
        getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }

    /**
     * Logs a message and exception with severity ERROR in the runtime log of
     * the plug-in.
     *
     * @param message
     *            A message to log
     * @param exception
     *            A exception to log
     */
    public void logError(String message, Throwable exception) {
        getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
    }

}
