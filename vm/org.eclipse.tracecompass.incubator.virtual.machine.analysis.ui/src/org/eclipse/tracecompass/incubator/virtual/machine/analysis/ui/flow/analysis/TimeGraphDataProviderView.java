/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.virtual.machine.analysis.ui.flow.analysis;

import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.core.data.provider.TimeGraphDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;

/**
 */
@SuppressWarnings("restriction")
public class TimeGraphDataProviderView extends BaseDataProviderTimeGraphView {

    /** View ID. */
    public static final String ID = "org.eclipse.tracecompass.overhead.dataprovider.tgview"; //$NON-NLS-1$

    /**
     * Default constructor
     */
    public TimeGraphDataProviderView() {
       super(ID, new BaseDataProviderTimeGraphPresentationProvider(), TimeGraphDataProvider.ID);
    }

}