/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparision.core;

import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for Syscall latency analysis.
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.internal.analysis.os.linux.core.latency.messages"; //$NON-NLS-1$

    /** Execution Duration aspect name */
    public static @Nullable String SegmentAspectName_Duration;

    /** Execution Duration aspect help text */
    public static @Nullable String SegmentAspectHelpText_Duration;

    /** Execution SelfTime aspect help text */
    public static @Nullable String SegmentAspectHelpText_SelfTime;

    /** Execution SelfTime aspect name */
    public static @Nullable String SegmentAspectName_SelfTime;

    /** Execution CpuTime aspect help text */
    public static @Nullable String SegmentAspectHelpText_CpuTime;

    /** Execution CpuTime aspect name */
    public static @Nullable String SegmentAspectName_CpuTime;




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
            return StringUtils.EMPTY;
        }
        return msg;
    }
}
