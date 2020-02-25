/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.ui.actions;

import org.eclipse.osgi.util.NLS;

/**
 * Messages for ROS actions
 *
 * @author Christophe Bedard
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.ros.ui.actions.messages"; //$NON-NLS-1$

    /** Name text for HideRosoutAction */
    public static String HideRosoutAction_NameText;
    /** Tooltip text for HideRosoutAction */
    public static String HideRosoutAction_ToolTipText;

    /** Name text for FollowMessageAction */
    public static String FollowMessageAction_NameText;
    /** Tooltip text for FollowMessageAction */
    public static String FollowMessageAction_ToolTipText;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
