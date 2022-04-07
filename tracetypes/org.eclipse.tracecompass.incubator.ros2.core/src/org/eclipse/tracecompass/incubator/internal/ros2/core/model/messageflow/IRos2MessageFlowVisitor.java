/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Message flow visitor. Receives a visit from each message flow segment one at
 * a time, and is notified when all visits are done.
 *
 * @author Christophe Bedard
 */
public interface IRos2MessageFlowVisitor {

    /**
     * Visit a segment.
     *
     * @param segment
     *            the visited segment
     */
    public default void visit(@NonNull Ros2MessageFlowSegment segment) {
        throw new NotImplementedException(StringUtils.EMPTY);
    }

    /**
     * Visit a segment in a specific direction.
     *
     * @param segment
     *            the visited segment
     * @param isForward
     *            whether the visits are currently forward (or backward)
     */
    public default void visit(@NonNull Ros2MessageFlowSegment segment, boolean isForward) {
        // Just call visit() without the direction by default
        visit(segment);
    }

    /**
     * Called after all visits are finished.
     */
    public default void postVisit() {
        // Do nothing by default
    }
}
