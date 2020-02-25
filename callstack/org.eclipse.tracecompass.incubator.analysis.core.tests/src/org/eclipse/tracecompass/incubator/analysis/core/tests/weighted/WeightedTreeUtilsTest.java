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

package org.eclipse.tracecompass.incubator.analysis.core.tests.weighted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.tests.stubs.weighted.SimpleTree;
import org.eclipse.tracecompass.incubator.analysis.core.tests.stubs.weighted.WeightedTreeProviderStub;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.ITree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTreeSet;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTreeUtils;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeProvider;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the operations on the the {@link WeightedTreeUtils} class
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public class WeightedTreeUtilsTest {

    private static final Integer VALUE1 = 1;
    private static final Integer VALUE2 = 2;
    private static final Integer VALUE3 = 3;
    private static final Integer VALUE4 = 4;
    private static final Integer VALUE5 = 5;

    private @Nullable List<WeightedTree<Integer>> fTree1;
    private @Nullable List<WeightedTree<Integer>> fTree2;

    /**
     * Prepare data for the test
     */
    @Before
    public void setupTrees() {
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
        WeightedTree<Integer> element1 = new WeightedTree<>(VALUE1, 10);
        element1.addChild(new WeightedTree<>(VALUE2, 4));
        WeightedTree<Integer> element2 = new WeightedTree<>(VALUE3, 3);
        element1.addChild(element2);
        element2.addChild(new WeightedTree<>(VALUE3, 1));
        tree1.add(element1);
        element1 = new WeightedTree<>(VALUE2, 10);
        element1.addChild(new WeightedTree<>(VALUE4, 5));
        element1.addChild(new WeightedTree<>(VALUE5, 5));
        tree1.add(element1);
        fTree1 = tree1;

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
        element1 = new WeightedTree<>(VALUE1, 10);
        element1.addChild(new WeightedTree<>(VALUE2, 3));
        element2 = new WeightedTree<>(VALUE3, 3);
        element1.addChild(element2);
        element2.addChild(new WeightedTree<>(VALUE3, 1));
        tree2.add(element1);
        element1 = new WeightedTree<>(VALUE2, 20);
        element1.addChild(new WeightedTree<>(VALUE4, 10));
        element2 = new WeightedTree<>(VALUE5, 10);
        element1.addChild(element2);
        element2.addChild(new WeightedTree<>(VALUE3, 5));
        tree2.add(element1);
        fTree2 = tree2;
    }

    /**
     * Test the {@link WeightedTreeUtils#diffTrees(Collection, Collection)}
     * method with simple trees
     */
    @Test
    public void testDiffTree() {
        List<WeightedTree<Integer>> tree1 = fTree1;
        List<WeightedTree<Integer>> tree2 = fTree2;
        assertNotNull(tree1);
        assertNotNull(tree2);

        // Differentiate tree1 and tree2
        Collection<DifferentialWeightedTree<Integer>> diffTrees = WeightedTreeUtils.diffTrees(tree1, tree2);
        verifyDiffTrees12(diffTrees);

        // Reverse: Differentiate tree2 and tree1
        diffTrees = WeightedTreeUtils.diffTrees(tree2, tree1);
        verifyDiffTrees21(diffTrees);

    }

    /**
     * Test the
     * {@link WeightedTreeUtils#diffTreeSets(IWeightedTreeProvider, IWeightedTreeSet, IWeightedTreeSet)}
     * with treesets having a single element, that are not identical in both
     * trees
     */
    @Test
    public void testDiffTreeSetOneElement() {
        // Prepare the treesets for this test case
        String element1 = "elementForTree1";
        String element2 = "elementForTree2";

        WeightedTreeSet<Integer, String> treeSet1 = new WeightedTreeSet<>();
        WeightedTreeSet<Integer, String> treeSet2 = new WeightedTreeSet<>();
        List<WeightedTree<Integer>> tree1 = fTree1;
        List<WeightedTree<Integer>> tree2 = fTree2;
        assertNotNull(tree1);
        assertNotNull(tree2);

        tree1.forEach(t -> treeSet1.addWeightedTree(element1, t));
        tree2.forEach(t -> treeSet2.addWeightedTree(element2, t));

        // Differentiate treeset1 and treeset2
        WeightedTreeProviderStub<Integer, String> provider = new WeightedTreeProviderStub<>();
        DifferentialWeightedTreeProvider<Integer> diffProvider = WeightedTreeUtils.diffTreeSets(provider, treeSet1, treeSet2);
        assertNotNull(diffProvider);
        IWeightedTreeSet<Integer, Object, DifferentialWeightedTree<Integer>> treeSet = diffProvider.getTreeSet();

        // Make sure there is one element in the treeset and it's the same as element1
        Collection<Object> elements = treeSet.getElements();
        assertEquals("Number of elements in diff tree", 1, elements.size());
        Object element = elements.iterator().next();
        assertEquals("element in diffTree", element1, element);
        Collection<DifferentialWeightedTree<Integer>> diffTrees = treeSet.getTreesFor(element);
        verifyDiffTrees12(diffTrees);
    }

    /**
     * Test the
     * {@link WeightedTreeUtils#diffTreeSets(IWeightedTreeProvider, IWeightedTreeSet, IWeightedTreeSet)}
     * with treesets having the exact same elements
     * trees
     */
    @Test
    public void testDiffTreeSetSameElements() {
        // Prepare the treesets for this test case
        String element1 = "element1";
        String element2 = "element2";

        WeightedTreeSet<Integer, String> treeSet1 = new WeightedTreeSet<>();
        WeightedTreeSet<Integer, String> treeSet2 = new WeightedTreeSet<>();
        List<WeightedTree<Integer>> tree1 = fTree1;
        List<WeightedTree<Integer>> tree2 = fTree2;
        assertNotNull(tree1);
        assertNotNull(tree2);

        // For treeset1, add tree1 for element1 and tree2 for element2
        tree1.forEach(t -> treeSet1.addWeightedTree(element1, t));
        tree2.forEach(t -> treeSet1.addWeightedTree(element2, t));
        // Switch the trees for treeSet2: tree2 for el1, tree1 for el2
        tree1.forEach(t -> treeSet2.addWeightedTree(element2, t));
        tree2.forEach(t -> treeSet2.addWeightedTree(element1, t));

        // Differentiate treeset1 and treeset2
        WeightedTreeProviderStub<Integer, String> provider = new WeightedTreeProviderStub<>();
        DifferentialWeightedTreeProvider<Integer> diffProvider = WeightedTreeUtils.diffTreeSets(provider, treeSet1, treeSet2);
        assertNotNull(diffProvider);
        IWeightedTreeSet<Integer, Object, DifferentialWeightedTree<Integer>> treeSet = diffProvider.getTreeSet();

        // Make sure the 2 elements are present in the treeset and their data is correct
        Collection<Object> elements = treeSet.getElements();
        assertEquals("Number of elements in diff tree", 2, elements.size());
        // Compare trees for element1
        Collection<DifferentialWeightedTree<Integer>> diffTrees = treeSet.getTreesFor(element1);
        assertFalse(diffTrees.isEmpty());
        verifyDiffTrees12(diffTrees);

        // Compare trees for element2
        diffTrees = treeSet.getTreesFor(element2);
        assertFalse(diffTrees.isEmpty());
        verifyDiffTrees21(diffTrees);
    }

    /**
     * Test the
     * {@link WeightedTreeUtils#diffTreeSets(IWeightedTreeProvider, IWeightedTreeSet, IWeightedTreeSet)}
     * with treesets having elements with the same name, in a tree structure
     * trees
     */
    @Test
    public void testDiffTreeSetNamedElementsTree() {
        // Prepare the treesets for this test case
        String element1 = "element1";
        String element2 = "element2";
        String element3 = "element3";

        WeightedTreeSet<Integer, SimpleTree> treeSet1 = new WeightedTreeSet<>();
        WeightedTreeSet<Integer, SimpleTree> treeSet2 = new WeightedTreeSet<>();
        List<WeightedTree<Integer>> tree1 = Objects.requireNonNull(fTree1);
        List<WeightedTree<Integer>> tree2 = Objects.requireNonNull(fTree2);

        // For treeset1, add tree1 for childEl11 and tree2 for childEl12
        // Prepare elements
        SimpleTree parentEl1 = new SimpleTree(element1);
        SimpleTree childEl11 = new SimpleTree(element2);
        SimpleTree childEl12 = new SimpleTree(element3);
        parentEl1.addChild(childEl11);
        parentEl1.addChild(childEl12);
        tree1.forEach(t -> treeSet1.addWeightedTree(childEl11, t));
        tree2.forEach(t -> treeSet1.addWeightedTree(childEl12, t));

        // Switch the trees for treeSet2: tree2 for childEl21, tree1 for childEl22
        SimpleTree parentEl2 = new SimpleTree(element1);
        SimpleTree childEl21 = new SimpleTree(element2);
        SimpleTree childEl22 = new SimpleTree(element3);
        parentEl2.addChild(childEl21);
        parentEl2.addChild(childEl22);
        tree1.forEach(t -> treeSet2.addWeightedTree(childEl22, t));
        tree2.forEach(t -> treeSet2.addWeightedTree(childEl21, t));

        // Differentiate treeset1 and treeset2
        WeightedTreeProviderStub<Integer, String> provider = new WeightedTreeProviderStub<>();
        DifferentialWeightedTreeProvider<Integer> diffProvider = WeightedTreeUtils.diffTreeSets(provider, treeSet1, treeSet2);
        assertNotNull(diffProvider);
        IWeightedTreeSet<Integer, Object, DifferentialWeightedTree<Integer>> treeSet = diffProvider.getTreeSet();

        // Make sure the element hierarchy is the same as treeset 1
        Collection<Object> elements = treeSet.getElements();
        assertEquals("Number of elements in diff tree", 1, elements.size());
        Object diffParentEl = elements.iterator().next();
        assertEquals("Same element as treeset1", parentEl1, diffParentEl);
        Collection<ITree> children = ((SimpleTree) diffParentEl).getChildren();
        assertEquals("Number of children", 2, children.size());

        // Compare trees for element1
        Collection<DifferentialWeightedTree<Integer>> diffTrees = treeSet.getTreesFor(childEl11);
        assertFalse(diffTrees.isEmpty());
        verifyDiffTrees12(diffTrees);

        // Compare trees for element2
        diffTrees = treeSet.getTreesFor(childEl12);
        assertFalse(diffTrees.isEmpty());
        verifyDiffTrees21(diffTrees);
    }

    /**
     * Test the
     * {@link WeightedTreeUtils#diffTreeSets(IWeightedTreeProvider, IWeightedTreeSet, IWeightedTreeSet)}
     * with treesets having 2 elements each, but only one with the same name
     * trees
     */
    @Test
    public void testDiffTreeSetNamedElementsMixed() {
        // Prepare the treesets for this test case
        String element1 = "element1";
        String element2 = "element2";
        String element3 = "element3";
        String element4 = "element4";

        WeightedTreeSet<Integer, SimpleTree> treeSet1 = new WeightedTreeSet<>();
        WeightedTreeSet<Integer, SimpleTree> treeSet2 = new WeightedTreeSet<>();
        List<WeightedTree<Integer>> tree1 = Objects.requireNonNull(fTree1);
        List<WeightedTree<Integer>> tree2 = Objects.requireNonNull(fTree2);

        // For treeset1, add tree1 for element1 and tree2 for element2
        // Prepare elements
        SimpleTree parentEl1 = new SimpleTree(element1);
        SimpleTree childEl11 = new SimpleTree(element2);
        SimpleTree childEl12 = new SimpleTree(element3);
        parentEl1.addChild(childEl11);
        parentEl1.addChild(childEl12);
        tree1.forEach(t -> treeSet1.addWeightedTree(childEl11, t));
        tree2.forEach(t -> treeSet1.addWeightedTree(childEl12, t));

        // Switch the trees for treeSet2: tree2 for el1, tree1 for el2
        // Child 2 of parent element does not match any name from other tree set
        SimpleTree parentEl2 = new SimpleTree(element1);
        SimpleTree childEl21 = new SimpleTree(element2);
        SimpleTree childEl22 = new SimpleTree(element4);
        parentEl2.addChild(childEl21);
        parentEl2.addChild(childEl22);
        tree1.forEach(t -> treeSet2.addWeightedTree(childEl22, t));
        tree2.forEach(t -> treeSet2.addWeightedTree(childEl21, t));

        // Differentiate treeset1 and treeset2
        WeightedTreeProviderStub<Integer, String> provider = new WeightedTreeProviderStub<>();
        DifferentialWeightedTreeProvider<Integer> diffProvider = WeightedTreeUtils.diffTreeSets(provider, treeSet1, treeSet2);
        assertNotNull(diffProvider);
        IWeightedTreeSet<Integer, Object, DifferentialWeightedTree<Integer>> treeSet = diffProvider.getTreeSet();

        // Make sure the element hierarchy is the same as treeset 1
        Collection<Object> elements = treeSet.getElements();
        assertEquals("Number of elements in diff tree", 1, elements.size());
        Object diffParentEl = elements.iterator().next();
        assertEquals("Same element as treeset1", parentEl1, diffParentEl);
        Collection<ITree> children = ((SimpleTree) diffParentEl).getChildren();
        assertEquals("Number of children", 2, children.size());

        // Compare trees for element1
        Collection<DifferentialWeightedTree<Integer>> diffTrees = treeSet.getTreesFor(childEl11);
        assertFalse(diffTrees.isEmpty());
        verifyDiffTrees12(diffTrees);

        // The second element should have no trees
        diffTrees = treeSet.getTreesFor(childEl12);
        assertTrue(diffTrees.isEmpty());
    }

    /**
     * Test the
     * {@link WeightedTreeUtils#diffTreeSets(IWeightedTreeProvider, IWeightedTreeSet, IWeightedTreeSet)}
     * with treesets having multiple elements, but none in common
     * trees
     */
    @Test
    public void testDiffTreeSetUnpairedElements() {
        // Prepare the treesets for this test case
        String element1 = "element1";
        String element2 = "element2";
        String element3 = "element3";
        String element4 = "element4";

        WeightedTreeSet<Integer, String> treeSet1 = new WeightedTreeSet<>();
        WeightedTreeSet<Integer, String> treeSet2 = new WeightedTreeSet<>();
        List<WeightedTree<Integer>> tree1 = fTree1;
        List<WeightedTree<Integer>> tree2 = fTree2;
        assertNotNull(tree1);
        assertNotNull(tree2);

        // For treeset1, add tree1 for element1 and tree2 for element2
        tree1.forEach(t -> treeSet1.addWeightedTree(element1, t));
        tree2.forEach(t -> treeSet1.addWeightedTree(element2, t));
        // Switch the trees for treeSet2: tree2 for el1, tree1 for el2
        tree1.forEach(t -> treeSet2.addWeightedTree(element3, t));
        tree2.forEach(t -> treeSet2.addWeightedTree(element4, t));

        // Differentiate treeset1 and treeset2
        WeightedTreeProviderStub<Integer, String> provider = new WeightedTreeProviderStub<>();
        DifferentialWeightedTreeProvider<Integer> diffProvider = WeightedTreeUtils.diffTreeSets(provider, treeSet1, treeSet2);
        assertNull(diffProvider);
    }

    private static void verifyDiffTrees12(Collection<DifferentialWeightedTree<Integer>> diffTrees) {
        assertEquals("Size of differential tree", 2, diffTrees.size());
        // Compare the first element
        Collection<DifferentialWeightedTree<Integer>> nextTree = getAndVerifyTree(diffTrees, VALUE1, 10, 0);
        assertEquals("Size of differential tree level 2", 2, nextTree.size());
        getAndVerifyTree(nextTree, VALUE2, 3, -0.25);
        nextTree = getAndVerifyTree(nextTree, VALUE3, 3, 0);
        assertEquals("Size of diferential tree level 3", 1, nextTree.size());
        getAndVerifyTree(nextTree, VALUE3, 1, 0);

        // Compare the second element
        nextTree = getAndVerifyTree(diffTrees, VALUE2, 20, 1.0);
        assertEquals("Size of differential tree level 2", 2, nextTree.size());
        getAndVerifyTree(nextTree, VALUE4, 10, 1.0);
        nextTree = getAndVerifyTree(nextTree, VALUE5, 10, 1.0);
        assertEquals("Size of diferential tree level 3", 1, nextTree.size());
        getAndVerifyTree(nextTree, VALUE3, 5, Double.NaN);
    }

    private static void verifyDiffTrees21(Collection<DifferentialWeightedTree<Integer>> diffTrees) {
        assertEquals("Size of differential tree", 2, diffTrees.size());
        // Compare the first element
        Collection<DifferentialWeightedTree<Integer>> nextTree = getAndVerifyTree(diffTrees, VALUE1, 10, 0);
        assertEquals("Size of differential tree level 2", 2, nextTree.size());
        getAndVerifyTree(nextTree, VALUE2, 4, 0.33333);
        nextTree = getAndVerifyTree(nextTree, VALUE3, 3, 0);
        assertEquals("Size of diferential tree level 3", 1, nextTree.size());
        getAndVerifyTree(nextTree, VALUE3, 1, 0);

        // Compare the first element
        nextTree = getAndVerifyTree(diffTrees, VALUE2, 10, -0.5);
        assertEquals("Size of differential tree level 2", 2, nextTree.size());
        getAndVerifyTree(nextTree, VALUE4, 5, -0.5);
        nextTree = getAndVerifyTree(nextTree, VALUE5, 5, -0.5);
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
