/*******************************************************************************
 * Copyright (c) 2017 �cole Polytechnique de Montr�al
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui;

import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	/** The plugin ID */
	public static final String PLUGIN_ID = "org.eclipse.tracecompass.incubator.virtual.machine.analysis.ui"; //$NON-NLS-1$

	// The shared instance
	private static @Nullable Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
    public void start(@Nullable BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
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
	    Activator plugin2 = plugin;
	    if (plugin2 == null) {
	        throw new NullPointerException("The activator should not be null at this point"); //$NON-NLS-1$
	    }
		return plugin2;
	}

	   // ------------------------------------------------------------------------
    // Logging helpers
    // ------------------------------------------------------------------------

    /**
     * Logs a message with severity INFO in the runtime log of the plug-in.
     *
     * @param message
     *            A message to log
     */
    public void logInfo(@Nullable String message) {
        getLog().log(new Status(IStatus.INFO, PLUGIN_ID, nullToEmptyString(message)));
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
    public void logInfo(@Nullable String message, Throwable exception) {
        getLog().log(new Status(IStatus.INFO, PLUGIN_ID, nullToEmptyString(message), exception));
    }

    /**
     * Logs a message and exception with severity WARNING in the runtime log of
     * the plug-in.
     *
     * @param message
     *            A message to log
     */
    public void logWarning(@Nullable String message) {
        getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, nullToEmptyString(message)));
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
    public void logWarning(@Nullable String message, Throwable exception) {
        getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, nullToEmptyString(message), exception));
    }

    /**
     * Logs a message and exception with severity ERROR in the runtime log of
     * the plug-in.
     *
     * @param message
     *            A message to log
     */
    public void logError(@Nullable String message) {
        getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, nullToEmptyString(message)));
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
    public void logError(@Nullable String message, Throwable exception) {
        getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, nullToEmptyString(message), exception));
    }

}

