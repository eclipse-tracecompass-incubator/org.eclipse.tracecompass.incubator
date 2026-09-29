/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.ui.view;

import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXDataProviderFactory;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;

/**
 * TraceX execution time-graph view.
 */
@SuppressWarnings("restriction")
public class TraceXView extends BaseDataProviderTimeGraphView {

    /** View extension identifier. */
    public static final String ID = "org.eclipse.tracecompass.incubator.tracex.ui.view"; //$NON-NLS-1$

    /** Constructor. */
    public TraceXView() {
        super(ID, new BaseDataProviderTimeGraphPresentationProvider(),
                TraceXDataProviderFactory.ID);
    }
}
