/**********************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis;

/**
 * OTF2 Global definitions names
 *
 * @author Yoann Heitz
 */
public interface IOtf2GlobalDefinitions {

    /**
     * String definition name
     */
    String OTF2_STRING = "String"; //$NON-NLS-1$

    /**
     * Region definition name
     */
    String OTF2_REGION = "Region"; //$NON-NLS-1$

    /**
     * LocationGroup definition name
     */
    String OTF2_LOCATION_GROUP = "LocationGroup"; //$NON-NLS-1$

    /**
     * Location definition name
     */
    String OTF2_LOCATION = "Location"; //$NON-NLS-1$

    /**
     * System tree node definition name
     */
    String OTF2_SYSTEM_TREE_NODE = "SystemTreeNode"; //$NON-NLS-1$

    /**
     * Communicator definition name
     */
    String OTF2_COMM = "Comm"; //$NON-NLS-1$

    /**
     * Group definition name
     */
    String OTF2_GROUP = "Group"; //$NON-NLS-1$

    /**
     * Group member name
     */
    String OTF2_GROUP_MEMBER = "GroupMember"; //$NON-NLS-1$

    /**
     * Metric member name
     */
    String OTF2_METRIC_MEMBER = "MetricMember"; //$NON-NLS-1$

    /**
     * Metric class name
     */
    String OTF2_METRIC_CLASS = "MetricClass"; //$NON-NLS-1$
}
