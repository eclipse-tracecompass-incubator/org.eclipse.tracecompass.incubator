/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.criticalpath;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.base.ITmfGraphVisitor;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfVertex;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsWorker;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;

import com.google.common.collect.ImmutableList;

/**
 * A weighted tree set that contains the aggregation of the critical path graph
 * states.
 *
 * The tree set will have 3 elements that all aggregate the full critical path,
 * but differently.
 * <ul>
 * <li>1- Top level are all the processes involved and the second level is the
 * time those processes spend in the various states</li>
 * <li>2- with the _all suffix is the tree showing only the final status of the
 * critical path: running, preempted, network, etc</li>
 * <li>3- With the _proc suffix is the tree where the top level are the running
 * processes and the states. Any of the dependent processes could have been in
 * one of those states</li>
 * </ul>
 *
 * @author Geneviève Bastien
 */
public class CriticalPathWeighted implements IWeightedTreeSet<Object, String, WeightedTree<Object>> {

    /**
     * An empty critical path
     */
    private static final CriticalPathWeighted EMPTY_CRIT_PATH_CG = new CriticalPathWeighted();
    private static final String ALL_SUFFIX = String.valueOf(Messages.CriticalPathWeighted_ByStatus);
    private static final String PROCESS_SUFFIX = String.valueOf(Messages.CriticalPathWeighted_ByProcessName);
    private static final String THREAD_SUFFIX = String.valueOf(Messages.CriticalPathWeighted_ByThread);

    private final List<String> fElements;
    private WeightedTree<Object> fAggregatedTree;
    private WeightedTree<Object> fTree;
    private WeightedTree<Object> fProcessTree;

    private class GraphToCallGraphConverter implements ITmfGraphVisitor {

        private final TmfGraph fGraph;
        private final IGraphWorker fMainWorker;

        public GraphToCallGraphConverter(IGraphWorker mainWorker, TmfGraph graph) {
            fGraph = graph;
            fMainWorker = mainWorker;
        }

        @Override
        public void visitHead(TmfVertex vertex) {
            // Nothing to do

        }

        @Override
        public void visit(TmfVertex vertex) {
            // Nothing to do
        }

        @Override
        public void visit(TmfEdge edge, boolean horizontal) {
            if (edge.getDuration() == 0) {
                return;
            }
            addEdgeToElement(edge);
            addEdgeToAggregatedElement(edge);
            addEdgeToProcessElement(edge);
        }

        private void addEdgeToAggregatedElement(TmfEdge edge) {
            // Get the worker to which to attribute this edge, whether vertical
            // or horizontal
            IGraphWorker worker = fGraph.getParentOf(edge.getVertexTo());
            if (worker == null) {
                return;
            }

            // If it's another worker that is running, add a other process
            // running state
            if (worker != fMainWorker && edge.getType().equals(EdgeType.RUNNING)) {
                WeightedTree<Object> callSite = new WeightedTree<>(String.valueOf(Messages.CriticalPathWeighted_OtherRunningProcess));
                callSite.addToWeight(edge.getDuration());
                fAggregatedTree.addChild(callSite);
                return;
            }

            // Otherwise, add a first level call that corresponds to the worker
            WeightedTree<Object> callSite = new WeightedTree<>(edge.getType());
            callSite.addToWeight(edge.getDuration());
            fAggregatedTree.addChild(callSite);

        }

        private void addEdgeToElement(TmfEdge edge) {
            // Get the worker to which to attribute this edge, whether vertical
            // or horizontal
            IGraphWorker worker = fGraph.getParentOf(edge.getVertexTo());
            if (worker == null) {
                return;
            }

            WeightedTree<Object> workerTree;
            if (worker == fMainWorker) {
                // If it is the main worker, first level will be self
                workerTree = new WeightedTree<>(String.valueOf(Messages.CriticalPathWeighted_SelfWorker));
                workerTree.addToWeight(edge.getDuration());
            } else {
                // If it's another worker, add a worker as 1st level
                workerTree = new WeightedTree<>(String.valueOf(worker));
                workerTree.addToWeight(edge.getDuration());
            }

            // Then, add a second level for the edge type if it is not running
            if (!edge.getType().equals(EdgeType.RUNNING)) {
                WeightedTree<Object> childType = new WeightedTree<>(edge.getType());
                childType.addToWeight(edge.getDuration());
                workerTree.addChild(childType);
            }
            fTree.addChild(workerTree);
        }

        private void addEdgeToProcessElement(TmfEdge edge) {
            // Get the worker to which to attribute this edge, whether vertical
            // or horizontal
            IGraphWorker worker = fGraph.getParentOf(edge.getVertexTo());
            if (worker == null) {
                return;
            }

            WeightedTree<Object> workerTree;
            if (worker == fMainWorker) {
                // If it is the main worker, first level will be self
                workerTree = new WeightedTree<>(String.valueOf(Messages.CriticalPathWeighted_SelfWorker));
                workerTree.addToWeight(edge.getDuration());
            } else {
                // If it's another worker, first level is the name of the process
                workerTree = new WeightedTree<>(((OsWorker) worker).getName());
                workerTree.addToWeight(edge.getDuration());
            }

            // Then add a state for the non-running states
            if (!edge.getType().equals(EdgeType.RUNNING)) {
                WeightedTree<Object> typeTree = new WeightedTree<>(edge.getType());
                typeTree.addToWeight(edge.getDuration());
                workerTree.addChild(typeTree);
            }
            fProcessTree.addChild(workerTree);
        }

    }

    private CriticalPathWeighted() {
        // Private constructor to build the empty graph
        fElements = Collections.emptyList();
        fTree = new WeightedTree<>(StringUtils.EMPTY);
        fAggregatedTree = new WeightedTree<>(StringUtils.EMPTY);
        fProcessTree = new WeightedTree<>(StringUtils.EMPTY);
    }

    /**
     * Create a new critical path weighted tree set from the graph received in
     * parameter. The graph can be <code>null</code> or empty and this method
     * will return an empty treeset. It transforms the graph received in
     * parameter into a weighted tree, merging similar state's durations.
     *
     * @param graph
     *            The graph to transform into an aggregated weighted tree set.
     * @return The critical path weighted treeset
     */
    public static CriticalPathWeighted create(@Nullable TmfGraph graph) {
        if (graph == null) {
            return EMPTY_CRIT_PATH_CG;
        }
        TmfVertex head = graph.getHead();
        if (head == null) {
            return EMPTY_CRIT_PATH_CG;
        }
        return new CriticalPathWeighted(graph);
    }

    /**
     * Constructor. It transforms the graph received in parameter into a
     * weighted tree, merging similar state's durations.
     *
     * @param graph
     *            The graph to flatten as a weighted tree
     */
    private CriticalPathWeighted(TmfGraph graph) {
        TmfVertex head = graph.getHead();
        if (head == null) {
            throw new NullPointerException("Empty graph"); //$NON-NLS-1$
        }

        IGraphWorker worker = graph.getParentOf(head);
        if (worker == null) {
            throw new NullPointerException("head vertex has no parent"); //$NON-NLS-1$
        }
        fElements = ImmutableList.of(String.valueOf(worker) + THREAD_SUFFIX, String.valueOf(worker) + ALL_SUFFIX, String.valueOf(worker) + PROCESS_SUFFIX);
        fTree = new WeightedTree<>(String.valueOf(worker) + THREAD_SUFFIX);
        fAggregatedTree = new WeightedTree<>(String.valueOf(worker) + ALL_SUFFIX);
        fProcessTree = new WeightedTree<>(String.valueOf(worker) + PROCESS_SUFFIX);
        GraphToCallGraphConverter converter = new GraphToCallGraphConverter(worker, graph);
        graph.scanLineTraverse(worker, converter);
    }

    @Override
    public Collection<WeightedTree<Object>> getTreesFor(Object element) {
        if (!(element instanceof String)) {
            return Collections.emptyList();
        }
        String elStr = (String) element;
        if (elStr.endsWith(ALL_SUFFIX)) {
            return fAggregatedTree.getChildren();
        }
        if (elStr.endsWith(PROCESS_SUFFIX)) {
            return fProcessTree.getChildren();
        }
        return fTree.getChildren();
    }

    @Override
    public Collection<String> getElements() {
        return fElements;
    }

}
