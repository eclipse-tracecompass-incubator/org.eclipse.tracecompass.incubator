/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.event;

import org.eclipse.osgi.util.NLS;

/**
 * Messages
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.ftrace.core.event.messages"; //$NON-NLS-1$
    /**
     * Name
     */
    public static String FtraceAspects_Name;
    /**
     * Name Description
     */
    public static String FtraceAspects_NameD;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
