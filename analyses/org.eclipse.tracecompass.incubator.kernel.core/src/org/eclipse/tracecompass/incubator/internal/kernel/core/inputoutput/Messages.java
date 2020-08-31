/**********************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.inputoutput;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Message bundle for the Disks I/O module
 *
 * @author Yonni Chen
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.kernel.core.inputoutput.messages"; //$NON-NLS-1$

    /**
     * Disk request data provider elevator queue title
     */
    public static @Nullable String DiskRequestsDataProvider_WaitingQueue;
    /**
     * Disk request data provider dispatch queue title
     */
    public static @Nullable String DiskRequestsDataProvider_DriverQueue;
    /** Title for the sector tooltip */
    public static @Nullable String DiskRequestDataProvider_Sector;
    /** Title for the number of sectors tooltip */
    public static @Nullable String DiskRequestDataProvider_NbSectors;
    /** Title for the type of request tooltip */
    public static @Nullable String DiskRequestDataProvider_RequestType;

    /**
     * Disk request data provider description
     */
    public static @Nullable String DiskRequestDataProviderFactory_descriptionText;

    /**
     * Disk Request data provider title
     */
    public static @Nullable String DiskRequestDataProviderFactory_title;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}