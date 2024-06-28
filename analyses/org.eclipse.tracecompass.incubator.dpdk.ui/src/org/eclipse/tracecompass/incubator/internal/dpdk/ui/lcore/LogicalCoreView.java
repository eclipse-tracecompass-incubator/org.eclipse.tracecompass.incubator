/**********************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.ui.lcore;

import org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis.DpdkLogicalCoreDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;

/**
 * Showing the Logical core and service states across the trace duration
 *
 * @author Arnaud Fiorini
 */
@SuppressWarnings("restriction")
public class LogicalCoreView extends BaseDataProviderTimeGraphView {

    private static final String ID = "org.eclipse.tracecompass.incubator.dpdk.lcore.view"; //$NON-NLS-1$

    /**
     * Default constructor
     */
    public LogicalCoreView() {
        super(ID, new BaseDataProviderTimeGraphPresentationProvider(), DpdkLogicalCoreDataProvider.ID);
    }
}
