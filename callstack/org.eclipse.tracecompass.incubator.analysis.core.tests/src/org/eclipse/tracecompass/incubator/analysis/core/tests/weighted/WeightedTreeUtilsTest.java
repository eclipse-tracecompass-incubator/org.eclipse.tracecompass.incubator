/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.tests.weighted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTreeUtils;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.junit.Test;

/**
 * Test the operations on the the {@link WeightedTree} class
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public class WeightedTreeUtilsTest {

    private static final Integer ELEMENT1 = 1;
    private static final Integer ELEMENT2 = 2;
    private static final Integer ELEMENT3 = 3;
    private static final Integer ELEMENT4 = 4;
    private static final Integer ELEMENT5 = 5;

    /**
     * Test the {@link WeightedTreeUtils#diffTrees(Collection, Collection)}
     * method with simple trees
     */
    @Test
    public void testDiffTree() {
        // Create a simple tree with 2 elements and a level of children
        List<WeightedTree<Integer>> tree1 = new ArrayList<>();
        List<WeightedTree<Integer>> tree2 = new ArrayList<>();
        /**
         * <pre>
         * Create a first collection of tree
         * * 1  ->  10
         *    | * 2 -> 4
         *    | * 3 -> 3
         *        | * 3 -> 1
         * * 2  ->  10
         *    | * 4 -> 5
         *    | * 5 -> 5
         * </pre>
         */
        WeightedTree<Integer> element1 = new WeightedTree<>(ELEMENT1, 10);
        element1.addChild(new WeightedTree<>(ELEMENT2, 4));
        WeightedTree<Integer> element2 = new WeightedTree<>(ELEMENT3, 3);
        element1.addChild(element2);
        element2.addChild(new WeightedTree<>(ELEMENT3, 1));
        tree1.add(element1);
        element1 = new WeightedTree<>(ELEMENT2, 10);
        element1.addChild(new WeightedTree<>(ELEMENT4, 5));
        element1.addChild(new WeightedTree<>(ELEMENT5, 5));
        tree1.add(element1);

        /**
         * <pre>
         * Create a second collection of tree
         * * 1  ->  10
         *    | * 2 -> 3
         *    | * 3 -> 3
         *        | * 3 -> 1
         * * 2  ->  20
         *    | * 4 -> 10
         *    | * 5 -> 10
         *        | * 3 -> 5
         * </pre>
         */
        element1 = new WeightedTree<>(ELEMENT1, 10);
        element1.addChild(new WeightedTree<>(ELEMENT2, 3));
        element2 = new WeightedTree<>(ELEMENT3, 3);
        element1.addChild(element2);
        element2.addChild(new WeightedTree<>(ELEMENT3, 1));
        tree2.add(element1);
        element1 = new WeightedTree<>(ELEMENT2, 20);
        element1.addChild(new WeightedTree<>(ELEMENT4, 10));
        element2 = new WeightedTree<>(ELEMENT5, 10);
        element1.addChild(element2);
        element2.addChild(new WeightedTree<>(ELEMENT3, 5));
        tree2.add(element1);

        // Differentiate tree1 and tree2
        Collection<DifferentialWeightedTree<Integer>> diffTrees = WeightedTreeUtils.diffTrees(tree1, tree2);
        assertEquals("Size of differential tree", 2, diffTrees.size());
        // Compare the first element
        Collection<DifferentialWeightedTree<Integer>> nextTree = getAndVerifyTree(diffTrees, ELEMENT1, 10, 0);
        assertEquals("Size of differential tree level 2", 2, nextTree.size());
        getAndVerifyTree(nextTree, ELEMENT2, 3, -0.25);
        nextTree = getAndVerifyTree(nextTree, ELEMENT3, 3, 0);
        assertEquals("Size of diferential tree level 3", 1, nextTree.size());
        getAndVerifyTree(nextTree, ELEMENT3, 1, 0);

        // Compare the second element
        nextTree = getAndVerifyTree(diffTrees, ELEMENT2, 20, 1.0);
        assertEquals("Size of differential tree level 2", 2, nextTree.size());
        getAndVerifyTree(nextTree, ELEMENT4, 10, 1.0);
        nextTree = getAndVerifyTree(nextTree, ELEMENT5, 10, 1.0);
        assertEquals("Size of diferential tree level 3", 1, nextTree.size());
        getAndVerifyTree(nextTree, ELEMENT3, 5, Double.NaN);

        // Reverse: Differentiate tree2 and tree1
        diffTrees = WeightedTreeUtils.diffTrees(tree2, tree1);
        assertEquals("Size of differential tree", 2, diffTrees.size());
        // Compare the first element
        nextTree = getAndVerifyTree(diffTrees, ELEMENT1, 10, 0);
        assertEquals("Size of differential tree level 2", 2, nextTree.size());
        getAndVerifyTree(nextTree, ELEMENT2, 4, 0.33333);
        nextTree = getAndVerifyTree(nextTree, ELEMENT3, 3, 0);
        assertEquals("Size of diferential tree level 3", 1, nextTree.size());
        getAndVerifyTree(nextTree, ELEMENT3, 1, 0);

        // Compare the first element
        nextTree = getAndVerifyTree(diffTrees, ELEMENT2, 10, -0.5);
        assertEquals("Size of differential tree level 2", 2, nextTree.size());
        getAndVerifyTree(nextTree, ELEMENT4, 5, -0.5);
        nextTree = getAndVerifyTree(nextTree, ELEMENT5, 5, -0.5);
        assertTrue(nextTree.isEmpty());
    }

    private static Collection<DifferentialWeightedTree<Integer>> getAndVerifyTree(Collection<DifferentialWeightedTree<Integer>> diffTrees, Integer element, int expectedWeight, double expectedDiff) {
        for (DifferentialWeightedTree<Integer> tree : diffTrees) {
            List<DifferentialWeightedTree<Integer>> children = new ArrayList<>();
            if (tree.getObject().equals(element)) {
                // Found the tree, verify its values and return its children
                assertEquals("Base weight of " + element, expectedWeight, tree.getWeight());
                assertEquals("Differential value of " + element, expectedDiff, tree.getDifference(), 0.001);
                for (WeightedTree<Integer> child : tree.getChildren()) {
                    children.add((DifferentialWeightedTree<Integer>) child);
                }
                return children;
            }
        }
        throw new NullPointerException("Tree not found for object " + element);
    }

}
