/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.ui.views.messageflow;

import org.eclipse.osgi.util.NLS;

/**
 * Messages for ROS message flow UI
 *
 * @author Christophe Bedard
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.ros.ui.views.messageflow.messages"; //$NON-NLS-1$

    /** Multiple states for an event */
    public static String RosMessageFlowPresentationProvider_MultipleStates;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
