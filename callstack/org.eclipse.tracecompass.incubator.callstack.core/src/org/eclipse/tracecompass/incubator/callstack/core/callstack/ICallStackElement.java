/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.callstack;

import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;

/**
 * An element that correspond to one group of the call stack hierarchy.
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
 * Let's have an actual application with the following callstack:
 *
 * <pre>
 * where 1e2 means at timestamp 1, entry of function named op2
 *   and 10x means at timestamp 10, exit of the function
 *
 * pid1 --- tid2   1e1 ------------- 10x  12e4------------20x
 *      |             3e2-------7x
 *      |               4e3--5x
 *      |-- tid3      3e2 --------------------------------20x
 *                       5e3--6x  7e2--------13x
 *
 * pid5 --- tid6   1e1 -----------------------------------20x
 *      |            2e3 ---------7x      12e4------------20x
 *      |                4e1--6x
 * </pre>
 *
 * The first group has 2 {@link ICallStackElement}s: "pid1" and "pid5". "pid1"
 * has 2 children elements: "tid2" and "tid3" while "pid5" has one child:
 * "tid6". The last group is the leaf group, so it has only one
 * {@link ICallStackLeafElement} which contains the {@link CallStack} object.
 *
 * @author Geneviève Bastien
 */
public interface ICallStackElement {

    /**
     * The default key to use for symbol resolution if none is available
     */
    int DEFAULT_SYMBOL_KEY = -1;

    /**
     * Get the elements at the next level of the callstack hierarchy from this
     * element
     *
     * @return The list of children elements in the hierarchy
     */
    Collection<ICallStackElement> getChildren();

    /**
     * Get the next group descriptor
     *
     * @return The next group descriptor
     */
    @Nullable
    ICallStackGroupDescriptor getNextGroup();

    /**
     * Get the name of this element
     *
     * @return The name of the element
     */
    String getName();

    /**
     * Get the key for symbol resolution at a given time
     *
     * @param time
     *            The time at which to get the symbol key
     * @return The symbol key at time
     */
    int getSymbolKeyAt(long time);

    /**
     * Set the symbol key element to use for this hierarchy
     *
     * @param element
     *            The symbol key element
     */
    void setSymbolKeyElement(ICallStackElement element);

    /**
     * Return whether this element is the symbol key element
     *
     * @return Whether the element is the symbol key
     */
    boolean isSymbolKeyElement();

    /**
     * Get the parent element, or <code>null</code> if this element corresponds
     * to the first group of the hierarchy
     *
     * @return The parent element
     */
    @Nullable
    ICallStackElement getParentElement();

    /**
     * Get the leaf elements in this callstack hierarchy
     *
     * @return The collection of leaf elements that contain callstacks
     */
    Collection<ICallStackLeafElement> getLeafElements();

    /**
     * Get the ID of the host this callstack is part of
     *
     * FIXME: Can a callstack cover multiple hosts? Definitely
     *
     * @return the ID of the host this callstack is for
     */
    String getHostId();

}
