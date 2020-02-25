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

package org.eclipse.tracecompass.incubator.internal.kernel.ui.views.fileaccess;

import org.eclipse.osgi.util.NLS;

/**
 * Messages
 *
 * @author Matthew Khouzam
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.kernel.ui.views.fileaccess.messages"; //$NON-NLS-1$
    /**
     * Advanced mode label
     */
    public static String FileAccessByFileView_advanced;
    /**
     * Advanced mode description
     */
    public static String FileAccessByFileView_advancedDescription;
    /**
     * All threads
     */
    public static String FileAccessByFileView_all;
    /**
     * Prompt to follow a thread
     */
    public static String FileAccessByFileView_follow;
    /**
     * Read
     */
    public static String FileAccessByFileView_read;
    /**
     * Write
     */
    public static String FileAccessByFileView_write;
    /**
     * Resource, file
     */
    public static String FileAccessByFileView_resource;
    /**
     * Thread
     */
    public static String FileAccessByFileView_thread;
    /**
     * Title
     */
    public static String FileAccessByFileView_title;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        // do nothing
    }
}
