/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.filedescriptor;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.TimeGraphStateQueryFilter;

import com.google.common.collect.Multimap;

/**
 * Query a time and TID, should be in OS model?
 *
 * @author Matthew Khouzam
 */
public class TidTimeQueryFilter extends TimeGraphStateQueryFilter {
    private final Collection<Integer> fTids;

    /**
     * Constructor
     *
     * @param start
     *            start time
     * @param end
     *            end time
     * @param n
     *            number of times
     * @param tids
     *            collection of tids
     */
    public TidTimeQueryFilter(long start, long end, int n, Collection<Long> items, Multimap<@NonNull Integer, @NonNull String> regexes, Collection<Integer> tids) {
        super(start, end, n, items, regexes);
        fTids = tids;
    }

    /**
     * Constructor
     *
     * @param tids
     *            collection of tids
     */
    public TidTimeQueryFilter(List<Long> times, Collection<Long> items, Multimap<@NonNull Integer, @NonNull String> regexes, Collection<Integer> tids) {
        super(times, items, regexes);
        fTids = tids;
    }

    /**
     * Get the selected Tids
     *
     * @return the tids
     */
    public Collection<Integer> getTids() {
        return fTids;
    }
}