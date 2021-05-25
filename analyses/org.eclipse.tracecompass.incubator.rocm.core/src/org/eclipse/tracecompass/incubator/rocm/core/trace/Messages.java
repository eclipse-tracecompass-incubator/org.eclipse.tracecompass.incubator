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

package org.eclipse.tracecompass.incubator.rocm.core.trace;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Aspect names for the ROCm GPU traces
 *
 * @author Arnaud Fiorini
 */
@org.eclipse.jdt.annotation.NonNullByDefault
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.rocm.core.trace.messages"; //$NON-NLS-1$

    /** Name for the GPU aspect which displays the GPU id number */
    public static @Nullable String AspectName_GPU;

    /** Description of the GPU aspect */
    public static @Nullable String AspectHelpText_GPU;

    /** Name of the PID aspect which displays the PID (Process Id) */
    public static @Nullable String AspectName_PID;

    /** Name of the TID aspect which displays the TID (Thread Id) */
    public static @Nullable String AspectName_TID;

    /** Name of the Queue Id which displays the id of the GPU queue */
    public static @Nullable String AspectName_QueueID;

    /** Name of the Stream Id which displays the id of the HIP streams */
    public static @Nullable String AspectName_StreamID;

    /**
     * Name of the aspect which displays the index of the operation in the GPU
     * queue
     */
    public static @Nullable String AspectName_QueueIndex;

    /**
     * Function name which displays the function called for each event in the
     * trace
     */
    public static @Nullable String AspectName_FunctionName;

    /**
     * Nanosecond normalized timestamp
     *
     * @since 6.2
     */
    public static @Nullable String AspectName_Timestamp_Nanoseconds;
    /**
     * Explanation of why use a nanosecond normalized timestamp
     *
     * @since 6.2
     */
    public static @Nullable String AspectName_Timestamp_Nanoseconds_Help;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

    /**
     * Helper method to expose externalized strings as non-null objects.
     */
    static String getMessage(@Nullable String msg) {
        if (msg == null) {
            return ""; //$NON-NLS-1$
        }
        return msg;
    }
}
