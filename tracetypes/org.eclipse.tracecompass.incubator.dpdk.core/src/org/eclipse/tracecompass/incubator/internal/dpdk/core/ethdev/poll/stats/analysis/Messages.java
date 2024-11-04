/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.poll.stats.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for the {@link DpdkPollStatsAnalysis} on-demand analysis
 *
 * @author Adel Belkhiri
 */
@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.poll.stats.analysis.messages"; //$NON-NLS-1$

    public static @Nullable String EthdevPollStats_AnalysisName;
    public static @Nullable String EthdevPollStats_QueueLabel;
    public static @Nullable String EthdevPollStats_ThreadLabel;

    public static @Nullable String EthdevPollStats_MinimumValueLabel;
    public static @Nullable String EthdevPollStats_MaximumValueLabel;
    public static @Nullable String EthdevPollStats_AverageValueLabel;
    public static @Nullable String EthdevPollStats_StandardDeviationLabel;
    public static @Nullable String EthdevPollStats_CountLabel;

    static @NonNull String getMessage(@Nullable String msg) {
        if (msg == null) {
            return ""; //$NON-NLS-1$
        }
        return msg;
    }

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
