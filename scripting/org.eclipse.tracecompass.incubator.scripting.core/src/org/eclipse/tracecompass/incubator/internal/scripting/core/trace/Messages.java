/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.core.trace;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for the trace scripting module
 *
 * @author Benjamin Saint-Cyr
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.scripting.core.trace.messages"; //$NON-NLS-1$

    /** Error message when the project doesn't exist */
    public static @Nullable String projectDoesNotExist = null;
    /** Error message when the folder doesn't exist */
    public static @Nullable String folderDoesNotExist = null;
    /** Error message when no trace type could be found */
    public static @Nullable String noTraceType = null;
    /** Error message when the trace does not exist in the project */
    public static @Nullable String traceDoesNotExist = null;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
