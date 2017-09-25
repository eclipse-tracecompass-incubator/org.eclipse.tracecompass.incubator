/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.builder;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;

/**
 * @author Raphaël Beamonte
 *
 * @param <T>
 */
public class SuffixTree<T extends Comparable<? super T>> {
    private final int oo = Integer.MAX_VALUE / 2;
    private List<Node> nodes;
    private final List<T> origContent;
    private List<@Nullable T> content;
    private int root;
    private int position = -1;
    private int currentNode;
    private int needSuffixLink;
    private int remainder;

    private int active_node;
    private int active_length;
    private int active_edge;

    private class Node {
        int start;
        int end = oo;
        int link;
        public TreeMap<@Nullable T, Integer> next = new TreeMap<>(new Comparator<@Nullable T>() {
            @Override
            public int compare(@Nullable T t0, @Nullable T t1) {
                if (t0 == null) {
                    if (t1 == null) {
                        return 0;
                    }
                    return -1;
                } else if (t1 == null) {
                    return 1;
                }
                return t0.compareTo(t1);
            }
        });

        public Node(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int edgeLength() {
            return Math.min(end, position + 1) - start;
        }

        public List<T> edgeLabel() {
            if (edgeLength() < 1) {
                return new ArrayList<>();
            }

            List<@Nullable T> nullableSublist = content.subList(start, Math.min(Math.min(position + 1, end), content.size()));
            if (nullableSublist.size() > 0 && nullableSublist.get(nullableSublist.size() - 1) == null) {
                nullableSublist.remove(nullableSublist.size() - 1);
            }
            return new ArrayList<>(nullableSublist);
        }
    }

    public SuffixTree(List<T> orig) {
        origContent = orig;
        content = new ArrayList<>(orig);
        /*
         * if (content.size() > 10) { content = content.subList(0, 10); }
         */
        if (content.get(content.size() - 1) != null) {
            content.add(null);
        }

        nodes = new ArrayList<>(2 * content.size() + 2);
        for (int i = 0; i < 2 * content.size() + 2; i++) {
            nodes.add(null);
        }
        root = active_node = newNode(-1, -1);

        for (@Nullable
        T object : content) {
            addObject(object);
        }
    }

    private List<T> getLongestCommonPrefix(List<T> l0, List<T> l1, int max) {
        int min = Math.min(Math.min(l0.size(), l1.size()), max);
        for (int i = 0; i < min; i++) {
            if (!NonNullUtils.checkNotNull(l0.get(i)).equals(l1.get(i))) {
                return l0.subList(0, i);
            }
        }
        return l0.subList(0, min);
    }

    public List<T> getLongestNonOverlappingRepeatedSubstringWithoutRepeat() {
        List<T> longest = getLongestNonOverlappingRepeatedSubstring();

        int index = 0;
        int searchIndex;
        while ((searchIndex = longest.subList(index + 1, longest.size()).indexOf(longest.get(0))) != -1) {
            index += searchIndex + 1;

            /*
             * If there is a rest at the end of the list, compare it directly
             * now... If it doesn't match, then there probably won't be any
             * repetition
             */
            int rest = longest.size() % index;
            if (rest > 0
                    && !longest.subList(0, rest).equals(longest.subList(longest.size() - rest, longest.size()))) {
                continue;
            }

            // If nothing says it doesn't work, then consider it works
            boolean works = true;

            // Store the first substring, it will be use for comparison
            List<T> first = longest.subList(0, index);

            // Store the maximum value to try
            int max = longest.size() - index + 1;

            // Loop to test the repetitions
            for (int j = index; j < max; j += index) {
                // Get the second substring for comparison
                List<T> second = longest.subList(j, j + index);

                // If it's not equal, we can stop testing: there is no
                // comparison here!
                if (!first.equals(second)) {
                    works = false;
                    break;
                }
            }

            // If we arrive there with a working situation, we can return the
            // comparison substring
            if (works) {
                return first;
            }

        }

        return longest;
    }

    private List<T> getLongestNonOverlappingRepeatedSubstring() {
        List<T> longest = null;
        int distance = -1;
        int neighbors_to_check = 1;
        int at_least_size = 1;

        Iterator<List<T>> suffixArrayIterator = getSuffixArrayIterator();

        LinkedList<List<T>> listOfNeighbors = new LinkedList<>();
        listOfNeighbors.add(suffixArrayIterator.next());

        while (suffixArrayIterator.hasNext()) {
            List<T> l0 = suffixArrayIterator.next();

            // System.out.println("DEBUG: L0 = " + l0.toString());

            for (int neighbor = neighbors_to_check; neighbor > 0; neighbor--) {
                List<T> l1 = listOfNeighbors.get(neighbor - 1);

                // System.out.println("DEBUG:\t\t L1 = " + l1.toString());
                // System.out.println("\t - L0 = " + l0.toString() + "\n\t L1 =
                // " + l1.toString());

                distance = Math.abs(l0.size() - l1.size());
                boolean useAsBest = false;
                if (longest != null
                        && distance == at_least_size - 1
                        && l0.size() >= at_least_size - 1
                        && l1.size() >= at_least_size - 1
                        && l0.subList(0, at_least_size - 1).equals(l1.subList(0, at_least_size - 1))
                        && content.indexOf(l0.get(0)) < content.indexOf(longest.get(0))) {
                    // System.out.println("DEBUG: BLAH");
                    useAsBest = true;
                }
                if (!useAsBest && distance < at_least_size) {
                    if (l0.size() >= at_least_size
                            && l1.size() >= at_least_size
                            && l0.subList(0, at_least_size).equals(l1.subList(0, at_least_size))) {
                        // System.out.println("DEBUG: OUTPUT 1");
                        neighbors_to_check = Math.max(neighbors_to_check, neighbor + 1);
                    } else {
                        // System.out.println("DEBUG: OUTPUT 2");
                        neighbors_to_check = neighbor;
                    }
                    continue;
                }

                if (!useAsBest
                        && (l0.size() < at_least_size
                                || l1.size() < at_least_size
                                || !l0.subList(0, at_least_size).equals(l1.subList(0, at_least_size)))) {
                    // System.out.println("DEBUG: OUTPUT 3");
                    neighbors_to_check = neighbor;
                    continue;
                }

                longest = getLongestCommonPrefix(l0, l1, distance);
                // System.out.println("DEBUG: NEW LONGEST: " + longest + " /
                // DISTANCE = " + distance);
                at_least_size = longest.size() + 1;
                if (longest.size() == distance) {
                    // System.out.println("DEBUG: OUTPUT 4");
                    neighbors_to_check = Math.max(neighbors_to_check, neighbor + 1);
                } else {
                    // System.out.println("DEBUG: OUTPUT 5");
                    neighbors_to_check = neighbor;
                }
            }

            listOfNeighbors.addFirst(l0);
            while (listOfNeighbors.size() > 4 * neighbors_to_check) {
                listOfNeighbors.removeLast();
            }
        }

        return (longest != null) ? longest : origContent;
    }

    /*
     * private List<T> getLongestNonOverlappingRepeatedSubstringNoIterator() {
     * List<T> longest = null; int distance = -1; int neighbors_to_check = 1;
     * int at_least_size = 1;
     *
     * List<List<T>> suffixArray = new ArrayList<>(getSuffixArray()); for (int i
     * = 1; i < suffixArray.size(); i++) { List<T> l0 = suffixArray.get(i);
     *
     * //System.out.println("DEBUG: L0 = " + l0.toString());
     *
     * for (int neighbor = neighbors_to_check; neighbor > 0; neighbor--) {
     * List<T> l1 = suffixArray.get(i - neighbor);
     *
     * //System.out.println("DEBUG:\t\t L1 = " + l1.toString());
     * //System.out.println("\t - L0 = " + l0.toString() + "\n\t   L1 = " +
     * l1.toString());
     *
     * distance = Math.abs(l0.size() - l1.size()); boolean useAsBest = false; if
     * (longest != null && distance == at_least_size - 1 && l0.size() >=
     * at_least_size - 1 && l1.size() >= at_least_size - 1 && l0.subList(0,
     * at_least_size - 1).equals(l1.subList(0, at_least_size - 1)) &&
     * content.indexOf(l0.get(0)) < content.indexOf(longest.get(0))) {
     * //System.out.println("DEBUG: BLAH"); useAsBest = true; } if (!useAsBest
     * && distance < at_least_size) { if (l0.size() >= at_least_size &&
     * l1.size() >= at_least_size && l0.subList(0,
     * at_least_size).equals(l1.subList(0, at_least_size))) {
     * //System.out.println("DEBUG: OUTPUT 1"); neighbors_to_check =
     * Math.max(neighbors_to_check, neighbor + 1); } else {
     * //System.out.println("DEBUG: OUTPUT 2"); neighbors_to_check = neighbor; }
     * continue; }
     *
     * if (!useAsBest && (l0.size() < at_least_size || l1.size() < at_least_size
     * || !l0.subList(0, at_least_size).equals(l1.subList(0, at_least_size)))) {
     * //System.out.println("DEBUG: OUTPUT 3"); neighbors_to_check = neighbor;
     * continue; }
     *
     * longest = getLongestCommonPrefix(l0, l1, distance);
     * //System.out.println("DEBUG: NEW LONGEST: " + longest + " / DISTANCE = "
     * + distance); at_least_size = longest.size() + 1; if (longest.size() ==
     * distance) { //System.out.println("DEBUG: OUTPUT 4"); neighbors_to_check =
     * Math.max(neighbors_to_check, neighbor + 1); } else {
     * //System.out.println("DEBUG: OUTPUT 5"); neighbors_to_check = neighbor; }
     * } }
     *
     * return (longest != null) ? longest : origContent; }
     */

    /*
     * private Set<List<T>> getSuffixArray() { return getSuffixArray(new
     * ArrayList<>(), root); }
     */

    /*
     * private class PairTest { public int node; public int depth;
     *
     * public PairTest(int node, int depth) { this.node = node; this.depth =
     * depth; } }
     */

    // private Set<List<T>> getSuffixArray(List<T> prefix, int node) {
    /*
     * private Set<List<T>> getSuffixArray() { Set<List<T>> suffixArray = new
     * TreeSet<>(new Comparator<List<T>>() {
     *
     * @Override public int compare(List<T> l0, List<T> l1) { List<T> shortest;
     * if (l0.size() > l1.size()) { shortest = l1; } else { shortest = l0; }
     *
     * int cmp = 0; for (int i = 0; cmp == 0 && i < shortest.size(); i++) { cmp
     * = l0.get(i).compareTo(l1.get(i)); }
     *
     * if (cmp == 0 && l0.size() != l1.size()) { if (l0 == shortest) { cmp = -1;
     * } else { cmp = 1; } }
     *
     * return cmp; } }); /*for (int cnode : nodes.get(node).next.values()) {
     * List<T> newPrefix = new ArrayList<>(prefix);
     * newPrefix.addAll(edgeList(cnode)); if (nodes.get(cnode).next.size() == 0)
     * { suffixArray.add(newPrefix); } else {
     * suffixArray.addAll(getSuffixArray(newPrefix, cnode)); } }*
     *
     * LinkedList<List<T>> prefixes = new LinkedList<>(); LinkedList<PairTest>
     * nodesToTreat = new LinkedList<>(); nodesToTreat.add(new PairTest(root,
     * 0)); while (!nodesToTreat.isEmpty()) { PairTest nodePair =
     * nodesToTreat.removeFirst();
     *
     * while (nodePair.depth < prefixes.size()) { prefixes.removeLast(); }
     *
     * if (nodes.get(nodePair.node).next.size() == 0) { List<T> newPrefix = new
     * ArrayList<>(); for (List<T> prefixToAdd : prefixes) {
     * newPrefix.addAll(prefixToAdd); }
     * newPrefix.addAll(nodes.get(nodePair.node).edgeLabel()); if
     * (!newPrefix.isEmpty()) { suffixArray.add(newPrefix); } } else {
     * prefixes.add(nodes.get(nodePair.node).edgeLabel()); for (int cnode :
     * nodes.get(nodePair.node).next.values()) { nodesToTreat.addFirst(new
     * PairTest(cnode, nodePair.depth + 1)); } } }
     *
     * //suffixArray.removeIf(l -> l.isEmpty()); return suffixArray; }
     */

    public Iterator<List<T>> getSuffixArrayIterator() {
        return new SuffixArrayIterator();
    }

    private class SuffixArrayIterator implements Iterator<List<T>> {

        private class NodeDepthPair {
            public final int node;
            public final int depth;

            public NodeDepthPair(int node, int depth) {
                this.node = node;
                this.depth = depth;
            }
        }

        private final Comparator<List<T>> listComparator = new Comparator<List<T>>() {
            @Override
            public int compare(List<T> l0, List<T> l1) {
                if (l0 == null) {
                    if (l1 == null) {
                        return 0;
                    }
                    return -1;
                } else if (l1 == null) {
                    return 1;
                }

                List<T> shortest;
                if (l0.size() > l1.size()) {
                    shortest = l1;
                } else {
                    shortest = l0;
                }

                int cmp = 0;
                for (int i = 0; cmp == 0 && i < shortest.size(); i++) {
                    cmp = NonNullUtils.checkNotNull(l0.get(i)).compareTo(l1.get(i));
                }

                if (cmp == 0 && l0.size() != l1.size()) {
                    if (l0 == shortest) {
                        cmp = -1;
                    } else {
                        cmp = 1;
                    }
                }

                return cmp;
            }
        };
        private final Comparator<Integer> nodeComparator = new Comparator<Integer>() {
            @Override
            public int compare(Integer i0, Integer i1) {
                if (i0 == null) {
                    if (i1 == null) {
                        return 0;
                    }
                    return -1;
                } else if (i1 == null) {
                    return 1;
                }

                return listComparator.compare(nodes.get(i0).edgeLabel(), nodes.get(i1).edgeLabel());
            }
        };

        LinkedList<NodeDepthPair> nodesToTreat = new LinkedList<>();
        LinkedList<T> currentPrefix = new LinkedList<>();
        LinkedList<Integer> prefixSize = new LinkedList<>();

        public SuffixArrayIterator() {
            nodesToTreat.add(new NodeDepthPair(root, 0));
        }

        @Override
        public boolean hasNext() {
            return !nodesToTreat.isEmpty();
        }

        @Override
        public List<T> next() {
            while (!nodesToTreat.isEmpty()) {
                NodeDepthPair nodeDepthPair = nodesToTreat.removeFirst();

                int removePrefix = 0;
                while (nodeDepthPair.depth < prefixSize.size()) {
                    removePrefix += prefixSize.removeLast();
                }
                while (removePrefix > 0) {
                    currentPrefix.removeLast();
                    removePrefix--;
                }

                if (nodes.get(nodeDepthPair.node).next.size() == 0) {
                    List<T> newPrefix = new ArrayList<>(currentPrefix);
                    newPrefix.addAll(nodes.get(nodeDepthPair.node).edgeLabel());
                    if (!newPrefix.isEmpty()) {
                        return newPrefix;
                    }
                } else {
                    List<T> newPrefix = nodes.get(nodeDepthPair.node).edgeLabel();
                    prefixSize.add(newPrefix.size());
                    currentPrefix.addAll(newPrefix);

                    Set<Integer> sortedChildNodes = new TreeSet<>(nodeComparator.reversed());
                    sortedChildNodes.addAll(nodes.get(nodeDepthPair.node).next.values());
                    sortedChildNodes.forEach(cnode -> nodesToTreat.addFirst(new NodeDepthPair(cnode, nodeDepthPair.depth + 1)));
                }
            }

            return null;
        }

    }

    private void addSuffixLink(int node) {
        if (needSuffixLink > 0) {
            nodes.get(needSuffixLink).link = node;
        }
        needSuffixLink = node;
    }

    private @Nullable T active_edge() {
        return content.get(active_edge);
    }

    private boolean walkDown(int next) {
        if (active_length >= nodes.get(next).edgeLength()) {
            active_edge += nodes.get(next).edgeLength();
            active_length -= nodes.get(next).edgeLength();
            active_node = next;
            return true;
        }
        return false;
    }

    private int newNode(int start, int end) {
        nodes.set(++currentNode, new Node(start, end));
        return currentNode;
    }

    public void addObject(@Nullable T object) {
        content.set(++position, object);
        needSuffixLink = -1;
        remainder++;
        while (remainder > 0) {
            if (active_length == 0) {
                active_edge = position;
            }
            // System.out.println("DEBUG: ACTIVE EDGE ? " + active_edge());
            if (!nodes.get(active_node).next.containsKey(active_edge())) {
                // System.out.println("DEBUG: DOES NOT CONTAIN");
                int leaf = newNode(position, oo);
                nodes.get(active_node).next.put(active_edge(), leaf);
                addSuffixLink(active_node); // rule 2
            } else {
                // System.out.println("DEBUG: CONTAINS");
                int next = NonNullUtils.checkNotNull(nodes.get(active_node).next.get(active_edge()));
                if (walkDown(next)) {
                    // System.out.println("DEBUG: WALKDOWN (OBS 2)");
                    continue; // observation 2
                }
                @Nullable
                T obs1 = content.get(nodes.get(next).start + active_length);
                if ((obs1 == null && object == null) || (obs1 != null && obs1.equals(object))) { // observation
                                                                                                 // 1
                    // System.out.println("DEBUG: OBS 1");
                    active_length++;
                    addSuffixLink(active_node); // observation 3
                    break;
                }
                // System.out.println("DEBUG: SPLIT");
                int split = newNode(nodes.get(next).start, nodes.get(next).start + active_length);
                nodes.get(active_node).next.put(active_edge(), split);
                int leaf = newNode(position, oo);
                nodes.get(split).next.put(object, leaf);
                nodes.get(next).start += active_length;
                nodes.get(split).next.put(content.get(nodes.get(next).start), next);
                addSuffixLink(split); // rule 2
            }
            remainder--;

            if (active_node == root && active_length > 0) { // rule 1
                // System.out.println("DEBUG: RULE 1");
                active_length--;
                active_edge = position - remainder + 1;
            } else {
                // System.out.println("DEBUG: RULE 3");
                active_node = nodes.get(active_node).link > 0 ? nodes.get(active_node).link : root; // rule
                                                                                                    // 3
            }
        }
    }

    /*
     * public List<T> getLongestRepeatedSubsequence() { Node maxNode = null; for
     * (Node node : nodes) { if (node != null && node.start > -1 && node.end >
     * -1 && node.next.size() > 1) { if (maxNode == null || node.next.size() >
     * maxNode.next.size() || (node.next.size() == maxNode.next.size() &&
     * node.edgeLength() > maxNode.edgeLength())) { maxNode = node; } } }
     *
     * // If we didn't find any node with at least two repeats if (maxNode ==
     * null) { return new ArrayList<>(); }
     *
     * System.out.println("MAXNODE: start=" + maxNode.start + " end=" +
     * maxNode.end + " / POSITION: " + position + " / REPEAT: " +
     * maxNode.next.size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
     * //$NON-NLS-4$
     *
     * // Needs to be @Nullable to start List<@Nullable T>
     * longestRepeatedSubsequence = content.subList(maxNode.start,
     * Math.min(position + 1, maxNode.end)); if
     * (longestRepeatedSubsequence.get(longestRepeatedSubsequence.size() - 1) ==
     * null) {
     * longestRepeatedSubsequence.remove(longestRepeatedSubsequence.size() - 1);
     * }
     *
     * // Remove the @Nullable type return new
     * ArrayList<>(longestRepeatedSubsequence); }
     */

    /*
     * printing the Suffix Tree in a format understandable by graphviz. The
     * output is written into st.dot file. In order to see the suffix tree as a
     * PNG image, run the following command: dot -Tpng -O st.dot
     */

    private PrintStream out = System.out;

    private String edgeString(int node) {
        return content.subList(nodes.get(node).start, Math.min(position + 1, nodes.get(node).end)).toString();
    }

    public void printTree() {
        out.println("digraph {"); //$NON-NLS-1$
        out.println("\trankdir = LR;"); //$NON-NLS-1$
        out.println("\tedge [arrowsize=0.4,fontsize=10]"); //$NON-NLS-1$
        out.println("\tnode1 [label=\"\",style=filled,fillcolor=lightgrey,shape=circle,width=.1,height=.1];"); //$NON-NLS-1$
        out.println("//------leaves------"); //$NON-NLS-1$
        printLeaves(root);
        out.println("//------internal nodes------"); //$NON-NLS-1$
        printInternalNodes(root);
        out.println("//------edges------"); //$NON-NLS-1$
        printEdges(root);
        out.println("//------suffix links------"); //$NON-NLS-1$
        printSLinks(root);
        out.println("}"); //$NON-NLS-1$
    }

    private void printLeaves(int x) {
        if (nodes.get(x).next.size() == 0) {
            out.println("\tnode" + x + " [label=\"\",shape=point]"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            for (int child : nodes.get(x).next.values()) {
                printLeaves(child);
            }
        }
    }

    private void printInternalNodes(int x) {
        if (x != root && nodes.get(x).next.size() > 0) {
            out.println("\tnode" + x + " [label=\"\",style=filled,fillcolor=lightgrey,shape=circle,width=.07,height=.07]"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        for (int child : nodes.get(x).next.values()) {
            printInternalNodes(child);
        }
    }

    private void printEdges(int x) {
        for (int child : nodes.get(x).next.values()) {
            out.println("\tnode" + x + " -> node" + child + " [label=\"" + edgeString(child) + "\",weight=3]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            printEdges(child);
        }
    }

    private void printSLinks(int x) {
        if (nodes.get(x).link > 0) {
            out.println("\tnode" + x + " -> node" + nodes.get(x).link + " [label=\"\",weight=1,style=dotted]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        for (int child : nodes.get(x).next.values()) {
            printSLinks(child);
        }
    }
}

/*
 * public ST() throws Exception { in = new BufferedReader(new
 * InputStreamReader(System.in)); out = new PrintWriter(new
 * FileWriter("st.dot")); String line = in.readLine(); SuffixTreeTest st = new
 * SuffixTreeTest(line.length()); for (int i = 0; i < line.length(); i++)
 * st.addChar(line.charAt(i)); st.printTree(); out.close(); }
 */
