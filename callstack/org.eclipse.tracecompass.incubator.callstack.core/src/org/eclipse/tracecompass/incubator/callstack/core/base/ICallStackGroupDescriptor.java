/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.base;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeGroupDescriptor;

/**
 * This interface describes a group in the callstack. A group can either be a
 * source group under which other groups are, or a leaf group, under which is
 * the actual stack.
 *
 * Example: Let's take a trace that registers function entry and exit for
 * threads and where events also provide information on some other stackable
 * application component:
 *
 * A possible group hierarchy would be the following:
 *
 * <pre>
 *  Per PID:
 *    [pid]
 *        [tid]
 *            data
 * </pre>
 *
 * or
 *
 * <pre>
 *  With additional component information:
 *    [pid]
 *       [application component]
 *          [tid]
 *              data
 * </pre>
 *
 * In the first case, there would be 2 groups, and in the second 3 groups. It is
 * the analysis's responsibility to implement how to retrieve which group a
 * trace event belongs to and add the data to the proper leaf group. These
 * groups are indication for the various analyses on how to divide the data and
 * some analyses may do some aggregation based on those groups.
 *
 * To each group will correspond a number of {@link ICallStackElement} that
 * represent single elements of this group. In the example above, to the [pid]
 * group will correspond each individual process being analyses, eg. process 45,
 * 10001, etc.
 *
 * If the function names happen to be addresses in an executable and the PID is
 * the key to map those symbols to actual function names, then the first group
 * "[pid]" would be the symbol key group, used to resolve the symbols.
 *
 * @author Geneviève Bastien
 */
@Deprecated(since="0.10.0", forRemoval=true)
public interface ICallStackGroupDescriptor extends IWeightedTreeGroupDescriptor {

    @Override
    @Nullable ICallStackGroupDescriptor getNextGroup();

    /**
     * Get whether the value of this group should be used as the key for the
     * symbol provider. For instance, for some callstack, the group
     * corresponding to the process ID would be the symbol key group.
     *
     * @return <code>true</code> if the values of this group are used as the
     *         symbol mapping key.
     */
    boolean isSymbolKeyGroup();

}
