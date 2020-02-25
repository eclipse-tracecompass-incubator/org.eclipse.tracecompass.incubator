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

package org.eclipse.tracecompass.incubator.analysis.core.tests.weighted.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.eclipse.tracecompass.incubator.internal.analysis.core.weighted.tree.DifferentialPalette;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;

/**
 * Test the {@link DifferentialPalette} class
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
@RunWith(Parameterized.class)
public class DifferentialPaletteTest {

    private static final String NO_DIFF_STYLE = "equal";
    private static final String LESS_STYLES = "less"; //$NON-NLS-1$
    private static final String MORE_STYLES = "more"; //$NON-NLS-1$
    private static final int MAX_VALUE = 5;

    /**
     * @return The arrays of parameters
     */
    @Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
                { "Default Palette", DifferentialPalette.getInstance(), Collections.singletonList(0), ImmutableList.of(1, 0.04),
                        ImmutableList.of(new Pair<>(0.00001, 1), new Pair<>(0.009, 1), new Pair<>(0.01, 2), new Pair<>(0.015, 2), new Pair<>(0.03, 4), new Pair<>(0.030000000001, 4), new Pair<>(0.02999999999, 3)) },
                { "Palette With Threshold", DifferentialPalette.create(10, 100), ImmutableList.of(0, 0.01, 0.0999999, 0.05, 0.1), ImmutableList.of(1, 10),
                        ImmutableList.of(new Pair<>(0.10000000001, 1), new Pair<>(0.9, 4), new Pair<>(0.33, 2), new Pair<>(0.53, 2), new Pair<>(0.55, 3), new Pair<>(0.75, 3), new Pair<>(0.78, 4), new Pair<>(0.98, 4)) },
                { "Palette With Small Threshold", DifferentialPalette.create(0, 1), Collections.singletonList(0), ImmutableList.of(0.01, 0.1, 1),
                        ImmutableList.of(new Pair<>(0.0000000001, 1), new Pair<>(0.00999999999, 4), new Pair<>(0.0025, 2), new Pair<>(0.0049999999, 2), new Pair<>(0.00500001, 3), new Pair<>(0.0075, 4), new Pair<>(0.0078, 4)) },

        });
    }

    private final String fTestName;
    private final DifferentialPalette fPalette;
    private final List<Number> fNoDiffValues;
    private final List<Number> fMaxDiffValues;
    private final List<Pair<Number, Integer>> fTestValues;

    /**
     * Constructor
     *
     * @param testName
     *            The name of the current test case
     * @param palette
     *            The palette to test
     * @param noDiffValues
     *            The values for which there should be no difference shown
     * @param maxDiffValues
     *            The values for which there should be maximum difference heat
     *            shown
     * @param testMap
     *            A map of value and their expected heat
     */
    public DifferentialPaletteTest(String testName, DifferentialPalette palette, List<Number> noDiffValues, List<Number> maxDiffValues, List<Pair<Number, Integer>> testMap) {
        fTestName = testName;
        fPalette = palette;
        fNoDiffValues = noDiffValues;
        fMaxDiffValues = maxDiffValues;
        fTestValues = testMap;
    }

    /**
     * Test the differential palette with the default constructor values
     */
    @Test
    public void testDefaultDiffPalette() {
        Map<String, OutputElementStyle> styles = fPalette.getStyles();

        WeightedTree<String> original = new WeightedTree<>("test", 100);

        // Test a difference of 0
        for (Number diff : fNoDiffValues) {
            DifferentialWeightedTree<String> diffTree = new DifferentialWeightedTree<>(original, original.getObject(), original.getWeight(), diff.doubleValue());
            OutputElementStyle style = fPalette.getStyleFor(diffTree);
            assertEquals(fTestName + ": No diff style " + diff, NO_DIFF_STYLE, style.getParentKey());
            assertTrue(fTestName + ": Style present for " + diff, styles.containsKey(style.getParentKey()));
        }

        // Test the maximum difference above the threshold, negative and positive
        for (Number diff : fMaxDiffValues) {
            // Test the positive value
            DifferentialWeightedTree<String> diffTree = new DifferentialWeightedTree<>(original, original.getObject(), original.getWeight(), Math.abs(diff.doubleValue()));
            OutputElementStyle style = fPalette.getStyleFor(diffTree);
            assertEquals(fTestName + ": Max diff positive " + diff, MORE_STYLES + MAX_VALUE, style.getParentKey());
            assertTrue(fTestName + ": Style present " + diff, styles.containsKey(style.getParentKey()));

            // Test the negative value
            diffTree = new DifferentialWeightedTree<>(original, original.getObject(), original.getWeight(), -Math.abs(diff.doubleValue()));
            style = fPalette.getStyleFor(diffTree);
            assertEquals(fTestName + ": Max diff negative " + diff, LESS_STYLES + MAX_VALUE, style.getParentKey());
            assertTrue(fTestName + ": Style present " + diff, styles.containsKey(style.getParentKey()));
        }

        // Test the various values in between the thresholds
        for (Pair<Number, Integer> diffEntry : fTestValues) {
            Number diff = diffEntry.getFirst();
            Integer expectedHeat = diffEntry.getSecond();

            // Test he positive value
            DifferentialWeightedTree<String> diffTree = new DifferentialWeightedTree<>(original, original.getObject(), original.getWeight(), Math.abs(diff.doubleValue()));
            OutputElementStyle style = fPalette.getStyleFor(diffTree);
            assertEquals(fTestName + ": positive diff " + diff, MORE_STYLES + expectedHeat, style.getParentKey());
            assertTrue(fTestName + ": Style present " + diff, styles.containsKey(style.getParentKey()));

            // Test the negative value
            diffTree = new DifferentialWeightedTree<>(original, original.getObject(), original.getWeight(), -Math.abs(diff.doubleValue()));
            style = fPalette.getStyleFor(diffTree);
            assertEquals(fTestName + ": negative diff " + diff, LESS_STYLES + expectedHeat, style.getParentKey());
            assertTrue(fTestName + ": Style present " + diff, styles.containsKey(style.getParentKey()));
        }
    }

}
