/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.ui.views.fileaccess;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.kernel.ui.views.fileaccess.messages"; //$NON-NLS-1$
    public static String FileAccessByFileView_8;
    public static String FileAccessByFileView_advanced;
    public static String FileAccessByFileView_all;
    public static String FileAccessByFileView_follow;
    public static String FileAccessByFileView_read;
    public static String FileAccessByFileView_resource;
    public static String FileAccessByFileView_thread;
    public static String FileAccessByFileView_title;
    public static String FileAccessByFileView_write;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
