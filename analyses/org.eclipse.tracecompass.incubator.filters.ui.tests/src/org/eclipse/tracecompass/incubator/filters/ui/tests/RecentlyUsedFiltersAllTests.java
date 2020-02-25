/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.filters.ui.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.tracecompass.incubator.internal.filters.ui.lspFilterTextbox.RecentlyUsedFilters;
import org.junit.jupiter.api.Test;

/**
 * This class tests the {@link RecentlyUsedFilters}
 *
 * @author Jeremy Dube
 *
 */
@SuppressWarnings("restriction")
public class RecentlyUsedFiltersAllTests {
    /**
     * Test when one filter is added
     */
    @Test
    public void createOneFilter() {
        RecentlyUsedFilters recentlyUsedFilters = new RecentlyUsedFilters(5, "RecentlyUsedFiltersTest");
        recentlyUsedFilters.addFilter("filter1");
        // Get filters
        List<String> filtersList = recentlyUsedFilters.getRecentlyFilters();
        assertEquals(1, filtersList.size());
        assertEquals("filter1", filtersList.get(0));
        // Clear filters for future tests
        recentlyUsedFilters.clearFilters();
    }

    /**
     * Test when one filter is added then removed
     */
    @Test
    public void removeOneFilter() {
        RecentlyUsedFilters recentlyUsedFilters = new RecentlyUsedFilters(5, "RecentlyUsedFiltersTest2");
        recentlyUsedFilters.addFilter("filter1");
        // Get filters
        List<String> filtersList = recentlyUsedFilters.getRecentlyFilters();
        assertEquals(1, filtersList.size());
        recentlyUsedFilters.clearFilters();
        filtersList = recentlyUsedFilters.getRecentlyFilters();
        assertEquals(0, filtersList.size());
    }

    /**
     * Creates two filters and check if they have the correct order, i.e.
     * filter2 should be first
     */
    @Test
    public void createTwoFilters() {
        RecentlyUsedFilters recentlyUsedFilters = new RecentlyUsedFilters(5, "RecentlyUsedFiltersTest3");
        recentlyUsedFilters.addFilter("filter1");
        recentlyUsedFilters.addFilter("filter2");
        // Get filters
        List<String> filtersList = recentlyUsedFilters.getRecentlyFilters();
        assertEquals(2, filtersList.size());
        assertEquals("filter2", filtersList.get(0));
        assertEquals("filter1", filtersList.get(1));
        // Clear filters for future tests
        recentlyUsedFilters.clearFilters();
    }
}
