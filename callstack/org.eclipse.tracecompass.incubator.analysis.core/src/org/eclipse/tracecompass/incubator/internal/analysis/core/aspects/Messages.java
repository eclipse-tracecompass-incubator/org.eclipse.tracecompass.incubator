/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.analysis.core.aspects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for base items
 *
 * @author Matthew Khouzam
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.analysis.core.aspects.messages"; //$NON-NLS-1$
    public static @Nullable String ProcessNameAspect_description;
    public static @Nullable String ProcessNameAspect_name;
    public static @Nullable String ThreadNameAspect_description;
    public static @Nullable String ThreadNameAspect_name;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
