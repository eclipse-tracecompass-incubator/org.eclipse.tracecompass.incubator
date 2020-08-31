/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.ui.views.io.diskrequests;

import org.eclipse.tracecompass.incubator.internal.kernel.core.inputoutput.DiskRequestDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;

/**
 * Main implementation for the disk requests view
 *
 * @author Houssem Daoud
 */
public class DiskRequestsView extends BaseDataProviderTimeGraphView {

    /** View ID. */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.views.diskrequests"; //$NON-NLS-1$

//    private static final Comparator<ITimeGraphEntry> ASCENDING = (o1, o2) -> {
//        return ((DiskRequestsQueuedEntry) o1).compareTo(o2);
//    };
//    private static final Comparator<ITimeGraphEntry> DESCENDING = (o1, o2) -> {
//        return ((DiskRequestsQueuedEntry) o2).compareTo(o1);
//    };

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public DiskRequestsView() {
        super(ID, new BaseDataProviderTimeGraphPresentationProvider(), DiskRequestDataProvider.ID);
    }

}
