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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow.RosMessageFlowSegment;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;

/**
 * Extension of {@link TimeGraphEntryModel} to keep a reference to the
 * corresponding {@link RosMessageFlowSegment}
 *
 * @author Christophe Bedard
 */
public class RosMessageFlowSegmentEntryModel extends TimeGraphEntryModel {

    private final RosMessageFlowSegment fSegment;

    /**
     * Constructor
     *
     * @param id
     *            Entry ID
     * @param parentId
     *            Parent ID
     * @param startTime
     *            Start time
     * @param endTime
     *            End time
     * @param segment
     *            the corresponding segment
     */
    public RosMessageFlowSegmentEntryModel(long id, long parentId, long startTime, long endTime, RosMessageFlowSegment segment) {
        super(id, parentId, StringUtils.EMPTY, startTime, endTime);
        fSegment = segment;
    }

    /**
     * @return the segment
     */
    public RosMessageFlowSegment getSegment() {
        return fSegment;
    }
}
