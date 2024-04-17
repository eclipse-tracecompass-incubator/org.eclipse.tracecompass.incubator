/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.instrumented;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.segment.interfaces.INamedSegment;

/**
 * CalledFunction Interface
 *
 * @author Matthew Khouzam
 * @author Sonia Farrah
 */
@Deprecated(since="0.10.0", forRemoval=true)
public interface ICalledFunction extends INamedSegment {

    /**
     * The symbol of the call stack function.
     *
     * @return The symbol of the called function
     *
     */
    Object getSymbol();

    /**
     * The segment's parent
     *
     * @return The parent, can be null
     *
     */
    @Nullable
    ICalledFunction getParent();

    /**
     * The segment's self Time
     *
     * @return The self time, should always be less than or equal to
     *         {@link ISegment#getLength()}
     */
    long getSelfTime();

    /**
     * The segment's time on CPU
     *
     * @return The CPU time, ie the time spent on the CPU by the thread who
     *         called this function. {@link IHostModel#TIME_UNKNOWN} if
     *         not available.
     */
    long getCpuTime();

    /**
     * The process ID of the traced application
     *
     * @return The process ID
     */
    int getProcessId();

    /**
     * The ID of the thread that was running this function. A negative value
     * means an unknows thread ID.
     *
     * @return The thread ID, a negative value means it is not known
     */
    int getThreadId();

}