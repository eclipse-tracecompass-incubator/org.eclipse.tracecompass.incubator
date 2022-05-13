/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.otf2.core.trace;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.common.core.NonNullUtils;

/**
 * Messages
 *
 * @author Yoann Heitz
 */
@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.otf2.core.trace.messages"; //$NON-NLS-1$

    public static @Nullable String Otf2_ClusterAspectName;
    public static @Nullable String Otf2_ClusterAspectHelp;
    public static @Nullable String Otf2_PhysicalNodeAspectName;
    public static @Nullable String Otf2_PhysicalNodeAspectHelp;
    public static @Nullable String Otf2_ProcessAspectName;
    public static @Nullable String Otf2_ProcessAspectHelp;
    public static @Nullable String Otf2_ThreadAspectName;
    public static @Nullable String Otf2_ThreadAspectHelp;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

    /**
     * Helper method to expose externalized strings as non-null objects.
     */
    static String getMessage(@Nullable String msg) {
        return NonNullUtils.nullToEmptyString(msg);
    }
}
