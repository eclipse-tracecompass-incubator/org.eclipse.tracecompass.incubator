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
 * CTF fields name after conversion from OTF2
 *
 * These fields are used to access parameters from CTF events that were
 * converted from OTF2. They are defined in the OTF2-to-CTF converter:
 * https://github.com/dorsal-lab/OTF2-to-CTF-converter
 *
 * @author Yoann Heitz
 */
public interface IOtf2Fields {

    /**
     * String value field name
     */
    String OTF2_STRING_VALUE = "stringValue"; //$NON-NLS-1$

    /**
     * Number of members field name
     */
    String OTF2_NUMBER_OF_MEMBERS = "numberOfMembers"; //$NON-NLS-1$

    /**
     * Rank field name
     */
    String OTF2_RANK = "rank"; //$NON-NLS-1$

    /**
     * Name field name
     */
    String OTF2_NAME = "name"; //$NON-NLS-1$

    /**
     * Class name field name
     */
    String OTF2_CLASS_NAME = "className"; //$NON-NLS-1$

    /**
     * Communicator field name
     */
    String OTF2_COMMUNICATOR = "communicator"; //$NON-NLS-1$

    /**
     * Sender field name
     */
    String OTF2_SENDER = "sender"; //$NON-NLS-1$

    /**
     * Receiver field name
     */
    String OTF2_RECEIVER = "receiver"; //$NON-NLS-1$

    /**
     * Message tag field name
     */
    String OTF2_MESSAGE_TAG = "msgTag"; //$NON-NLS-1$

    /**
     * Request ID field name
     */
    String OTF2_REQUEST_ID = "requestID"; //$NON-NLS-1$

    /**
     * Collective operation code field name
     */
    String OTF2_COLLECTIVE_OPERATION = "collectiveOp"; //$NON-NLS-1$

    /**
     * Root field name
     */
    String OTF2_ROOT = "root"; //$NON-NLS-1$

    /**
     * Group field name
     */
    String OTF2_GROUP = "group"; //$NON-NLS-1$

    /**
     * Self field name
     */
    String OTF2_SELF = "self"; //$NON-NLS-1$

    /**
     * System tree parent field name
     */
    String OTF2_SYSTEM_TREE_PARENT = "systemTreeParent"; //$NON-NLS-1$

    /**
     * Location field name
     */
    String OTF2_LOCATION = "location"; //$NON-NLS-1$

    /**
     * Location group type field name
     */
    String OTF2_LOCATION_GROUP_TYPE = "locationGroupType"; //$NON-NLS-1$

    /**
     * Location type field name
     */
    String OTF2_LOCATION_TYPE = "locationType"; //$NON-NLS-1$

    /**
     * LocationID field name
     */
    String OTF2_LOCATION_ID = "locationID"; //$NON-NLS-1$

    /**
     * Region field name
     */
    String OTF2_REGION = "region"; //$NON-NLS-1$

    /**
     * Location group field name
     */
    String OTF2_LOCATION_GROUP = "locationGroup"; //$NON-NLS-1$

    /**
     * Message length field name
     */
    String OTF2_MESSAGE_LENGTH = "msgLength"; //$NON-NLS-1$

    /**
     * Size received field name
     */
    String OTF2_SIZE_RECEIVED = "sizeReceived"; //$NON-NLS-1$

    /**
     * Size sent field name
     */
    String OTF2_SIZE_SENT = "sizeSent"; //$NON-NLS-1$

    /**
     * Metric members field name
     */
    String OTF2_METRIC_MEMBERS = "metricMembers"; //$NON-NLS-1$

    /**
     * Description field name
     */
    String OTF2_DESCRIPTION = "description"; //$NON-NLS-1$

    /**
     * Metric type field name
     */
    String OTF2_METRIC_TYPE = "metricType"; //$NON-NLS-1$

    /**
     * Metric mode field name
     */
    String OTF2_METRIC_MODE = "metricMode"; //$NON-NLS-1$

    /**
     * Value type field name
     */
    String OTF2_VALUE_TYPE = "valueType"; //$NON-NLS-1$

    /**
     * Base field name
     */
    String OTF2_BASE = "base"; //$NON-NLS-1$

    /**
     * Exponent field name
     */
    String OTF2_EXPONENT = "exponent"; //$NON-NLS-1$

    /**
     * Unit field name
     */
    String OTF2_UNIT = "unit"; //$NON-NLS-1$

    /**
     * Metric value field name
     */
    String OTF2_METRIC_VALUE = "metricValue"; //$NON-NLS-1$

    /**
     * Metric field name
     */
    String OTF2_METRIC = "metric"; //$NON-NLS-1$

    /**
     * Index field name
     */
    String OTF2_INDEX = "index"; //$NON-NLS-1$
}
