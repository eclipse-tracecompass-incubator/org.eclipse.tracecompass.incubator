/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.scripting.ui.project.handlers;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages class
 *
 * @author Bernd Hufmann
 *
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.scripting.ui.project.handlers.messages"; //$NON-NLS-1$
    /** Menu entry for run/debug as menu*/
    public static @Nullable String Scripting_RunAsScriptName;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
