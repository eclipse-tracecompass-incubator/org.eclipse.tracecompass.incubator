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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.IRosModel;

/**
 * Interface that represents the model for ROS message flow, which is a "graph"
 * of the path a message goes take through ROS nodes, including
 * publisher/subscriber queues and callbacks
 *
 * @author Christophe Bedard
 */
public interface IRosMessageFlowModel extends IRosModel {

    /**
     * @return true if the model is complete, false otherwise
     */
    boolean isModelDone();

    /**
     * TODO eventually support building segments going backwards
     *
     * @return get the first segment of the message flow, or {@code null} if it
     *         has not been generated
     * @see
     */
    @Nullable
    RosMessageFlowSegment getFirstSegment();
}
