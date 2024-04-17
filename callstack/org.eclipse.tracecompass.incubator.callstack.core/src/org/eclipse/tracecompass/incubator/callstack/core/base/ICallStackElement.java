/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.base;

import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.ITree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeGroupDescriptor;

/**
 * Interface that classes representing a single element in the callstack
 * hierarchy must implement. Typically, a {@link ICallStackElement} will be
 * associated with a {@link ICallStackGroupDescriptor}. It will have children
 * that will correspond to the next group in the hierarchy.
 *
 * The actual data of the various available analyses containing those elements
 * will be available only at the leaf elements.
 *
 * @author Geneviève Bastien
 */
@Deprecated(since="0.10.0", forRemoval=true)
public interface ICallStackElement extends ITree {

    /**
     * Get the elements at the next level of the callstack hierarchy from this
     * element
     *
     * FIXME: Can this method be completely replace by {@link ITree#getChildren()}?
     *
     * @return The list of children elements in the hierarchy
     */
    Collection<ICallStackElement> getChildrenElements();

    /**
     * Get the corresponding group descriptor
     *
     * FIXME: Remove this method?
     *
     * @return The group descriptor of this element
     */
    IWeightedTreeGroupDescriptor getGroup();

    /**
     * Get the next group descriptor
     *
     * FIXME: Remove this method?
     *
     * @return The next group descriptor, or <code>null</code> if this is a leaf
     *         element
     */
    @Nullable IWeightedTreeGroupDescriptor getNextGroup();

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
     * FIXME: Can this method be completely replace by {@link ITree#getParent()}?
     *
     * @return The parent element
     */
    @Nullable ICallStackElement getParentElement();


    /**
     * Get whether this element is a leaf element in the callstack hierarchy.
     * Leaf elements are expected to contain the proper analysis data.
     *
     * @return Whether this element is a leaf, ie contains analysis data or not
     */
    boolean isLeaf();
}
