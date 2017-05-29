/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.callstack;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.CallStackSeries.IThreadIdResolver;

/**
 * This interface describes a group in the callstack. A group can either be a
 * source group under which other groups are, or a leaf group, under which is
 * the actual stack.
 *
 * Example: Let's take a trace that registers function entry and exit for
 * threads and where events also provide information on some other stackable
 * application component:
 *
 * The structure of this callstack in the state system could be as follows:
 *
 * <pre>
 *  Per PID
 *    [pid]
 *        [tid]
 *            callstack
 *               1  -> function name
 *               2  -> function name
 *               3  -> function name
 *  Per component
 *    [application component]
 *       [tid]
 *           callstack
 *               1 -> some string
 *               2 -> some string
 * </pre>
 *
 * There are 2 {@link CallStackSeries} in this example, one starting by "Per
 * PID" and another "Per component". For the first series, there could be 3
 * {@link ICallStackGroupDescriptor}: "Per PID/*", "*", "callstack".
 *
 * If the function names happen to be addresses in an executable and the PID is
 * the key to map those symbols to actual function names, then the first group
 * "Per PID/*" would be the symbol key group.
 *
 * Each group descriptor can get the corresponding {@link ICallStackElement}s,
 * ie, for the first group, it would be all the individual pids in the state
 * system, and for the second group, it would be the application components.
 * Each element that is not a {@link ICallStackLeafElement} will have a next
 * group descriptor that can fetch the elements under it. The last group will
 * resolve to leaf elements and each leaf elements has one {@link CallStack}
 * object.
 *
 * @author Geneviève Bastien
 */
public interface ICallStackGroupDescriptor {

    /**
     * Get the group descriptor at the next level.
     *
     * @return The next group or <code>null</code> if this is a leaf level
     */
    @Nullable ICallStackGroupDescriptor getNextGroup();

    /**
     * Get the elements corresponding to this group in the call stack hierarchy
     *
     * @param parent
     *            The element of the previous group, that will be parent to this
     *            group's elements
     * @param baseQuark
     *            The quark corresponding to the parent element
     * @param symbolKeyElement
     *            The symbol key element, or <code>null</code> if unavailable
     * @param threadIdProvider
     *            The thread ID provider
     * @return The list of elements corresponding to this group descriptor
     */
    List<ICallStackElement> getElements(@Nullable ICallStackElement parent, int baseQuark, @Nullable ICallStackElement symbolKeyElement, @Nullable IThreadIdResolver threadIdProvider);

    /**
     * Get whether the value of this group should be used as the key for the
     * symbol provider. For instance, for some callstack, the group
     * corresponding to the process ID would be the symbol key group.
     *
     * @return <code>true</code> if the values of this group are used as the
     *         symbol mapping key.
     */
    boolean isSymbolKeyGroup();

    /**
     * Get the human-readable name for this group descriptor
     *
     * @return The name of this group descriptor
     */
    String getName();

}
