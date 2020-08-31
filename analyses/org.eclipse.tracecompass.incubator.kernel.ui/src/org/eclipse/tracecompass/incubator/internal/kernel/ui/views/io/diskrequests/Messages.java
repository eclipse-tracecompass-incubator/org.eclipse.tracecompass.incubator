/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.ui.views.io.diskrequests;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for the disk requests view
 *
 * @author Geneviève Bastien
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.io.diskrequests.messages"; //$NON-NLS-1$

    // Labels for the request tooltip
    /** Sector label */
    public static @Nullable String DiskRequests_Sector;
    /** Request size label */
    public static @Nullable String DiskRequests_Size;

    // Labels for the view
    /** Dispatch queue label */
    public static @Nullable String DiskRequestsView_DispatchQueue;
    /** Elevator queue label */
    public static @Nullable String DiskRequestsView_ElevatorQueue;
    /** Multiple states label */
    public static @Nullable String DiskRequestsView_multipleStates;
    /** Next request action tooltip */
    public static @Nullable String DiskRequestsView_nextEventActionNameText;
    /** Next request action tooltip */
    public static @Nullable String DiskRequestsView_nextEventActionToolTipText;
    /** Previous request action label */
    public static @Nullable String DiskRequestsView_previousEventActionNameText;
    /** Previous request action tooltip */
    public static @Nullable String DiskRequestsView_previousEventActionToolTipText;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
