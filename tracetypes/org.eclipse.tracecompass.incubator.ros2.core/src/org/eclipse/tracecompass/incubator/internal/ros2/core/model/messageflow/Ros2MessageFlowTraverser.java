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

/**
 * Message flow traverser. Traverses the graph and applies the given visitor
 * along the way.
 *
 * @author Christophe Bedard
 */
public class Ros2MessageFlowTraverser {

    private final IRos2MessageFlowVisitor fVisitor;

    /**
     * Constructor
     *
     * @param visitor
     *            the visitor
     */
    public Ros2MessageFlowTraverser(IRos2MessageFlowVisitor visitor) {
        fVisitor = visitor;
    }

    /**
     * Traverse flow.
     *
     * @param initialSegment
     *            the initial segment
     */
    public void traverse(Ros2MessageFlowSegment initialSegment) {
        traverse(initialSegment, true, true);
    }

    /**
     * Traverse flow.
     *
     * @param initialSegment
     *            the initial segment
     * @param bothDirections
     *            whether to traverse backwards from the initial segment as well
     * @param skipDuplicateInitialSegment
     *            whether to skip visiting the initial segment when going
     *            backwards
     */
    public void traverse(Ros2MessageFlowSegment initialSegment, boolean bothDirections, boolean skipDuplicateInitialSegment) {
        traverseDirection(initialSegment, true, false);
        if (bothDirections) {
            traverseDirection(initialSegment, false, skipDuplicateInitialSegment);
        }
        fVisitor.postVisit();
    }

    private void traverseDirection(Ros2MessageFlowSegment segment, boolean isForward, boolean doSkip) {
        if (!doSkip) {
            fVisitor.visit(segment, isForward);
        }
        for (Ros2MessageFlowSegment nextSegment : (isForward ? segment.getNext() : segment.getPrevious())) {
            traverseDirection(nextSegment, isForward, false);
        }
    }
}
