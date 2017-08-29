/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.tests.callgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.AllGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraph;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraphGroupBy;
import org.eclipse.tracecompass.incubator.callstack.core.tests.flamechart.CallStackTestBase;
import org.eclipse.tracecompass.incubator.callstack.core.tests.stubs.CallStackAnalysisStub;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph.AggregatedCalledFunction;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

/**
 * Test the {@link CallGraphGroupBy} class
 *
 * @author Geneviève Bastien
 */
public class CallGraphGroupByInstrumentedTest extends CallStackTestBase {

    private class CallGraphExpected {
        public long duration;
        public long selfTime;
        public Map<String, CallGraphExpected> children;

        public CallGraphExpected(long dur, long self, Map<String, CallGraphExpected> childMap) {
            duration = dur;
            selfTime = self;
            children = childMap;
        }
    }

    private Map<String, CallGraphExpected> getExpectedAll() {
        return ImmutableMap.of(
                "op1", new CallGraphExpected(28, 8, ImmutableMap.of(
                        "op2", new CallGraphExpected(7, 5, ImmutableMap.of(
                                "op3", new CallGraphExpected(2, 2, Collections.emptyMap()))),
                        "op3", new CallGraphExpected(5, 3, ImmutableMap.of(
                                "op1", new CallGraphExpected(2, 2, Collections.emptyMap()))),
                        "op4", new CallGraphExpected(8, 8, Collections.emptyMap()))),
                "op4", new CallGraphExpected(8, 8, Collections.emptyMap()),
                "op2", new CallGraphExpected(17, 10, ImmutableMap.of(
                        "op3", new CallGraphExpected(1, 1, Collections.emptyMap()),
                        "op2", new CallGraphExpected(6, 6, Collections.emptyMap()))),
                "op5", new CallGraphExpected(19, 7, ImmutableMap.of(
                        "op2", new CallGraphExpected(12, 11, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))))));
    }

    private Map<String, CallGraphExpected> getExpectedProcess1() {
        return ImmutableMap.of(
                "op1", new CallGraphExpected(9, 5, ImmutableMap.of(
                        "op2", new CallGraphExpected(4, 3, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))))),
                "op4", new CallGraphExpected(8, 8, Collections.emptyMap()),
                "op2", new CallGraphExpected(17, 10, ImmutableMap.of(
                        "op3", new CallGraphExpected(1, 1, Collections.emptyMap()),
                        "op2", new CallGraphExpected(6, 6, Collections.emptyMap()))));
    }

    private Map<String, CallGraphExpected> getExpectedProcess5() {
        return ImmutableMap.of(
                "op1", new CallGraphExpected(19, 3, ImmutableMap.of(
                        "op3", new CallGraphExpected(5, 3, ImmutableMap.of(
                                "op1", new CallGraphExpected(2, 2, Collections.emptyMap()))),
                        "op2", new CallGraphExpected(3, 2, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))),
                        "op4", new CallGraphExpected(8, 8, Collections.emptyMap()))),
                "op5", new CallGraphExpected(19, 7, ImmutableMap.of(
                        "op2", new CallGraphExpected(12, 11, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))))));
    }

    private Map<String, CallGraphExpected> getExpectedThread2() {
        return ImmutableMap.of(
                "op1", new CallGraphExpected(9, 5, ImmutableMap.of(
                        "op2", new CallGraphExpected(4, 3, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))))),
                "op4", new CallGraphExpected(8, 8, Collections.emptyMap()));
    }

    private Map<String, CallGraphExpected> getExpectedThread3() {
        return ImmutableMap.of(
                "op2", new CallGraphExpected(17, 10, ImmutableMap.of(
                        "op3", new CallGraphExpected(1, 1, Collections.emptyMap()),
                        "op2", new CallGraphExpected(6, 6, Collections.emptyMap()))));
    }

    private Map<String, CallGraphExpected> getExpectedThread6() {
        return ImmutableMap.of(
                "op1", new CallGraphExpected(19, 3, ImmutableMap.of(
                        "op2", new CallGraphExpected(3, 2, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))),
                        "op3", new CallGraphExpected(5, 3, ImmutableMap.of(
                                "op1", new CallGraphExpected(2, 2, Collections.emptyMap()))),
                        "op4", new CallGraphExpected(8, 8, Collections.emptyMap()))));
    }

    private Map<String, CallGraphExpected> getExpectedThread7() {
        return ImmutableMap.of(
                "op5", new CallGraphExpected(19, 7, ImmutableMap.of(
                        "op2", new CallGraphExpected(12, 11, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))))));
    }

    /**
     * Test the group by all level for a call graph
     */
    @Test
    public void testGroupByAllInstrumented() {
        CallStackAnalysisStub cga = getModule();
        CallGraph baseCallGraph = cga.getCallGraph();

        CallGraph callGraph = CallGraphGroupBy.groupCallGraphBy(AllGroupDescriptor.getInstance(), baseCallGraph);
        Collection<ICallStackElement> elements = callGraph.getElements();
        assertEquals(1, elements.size());

        ICallStackElement element = Iterables.getFirst(elements, null);
        assertNotNull(element);

        Collection<AggregatedCallSite> callingContextTree = callGraph.getCallingContextTree(element);
        compareCcts("", getExpectedAll(), callingContextTree);

    }

    /**
     * Test the group by intermediate level for a call graph
     */
    @Test
    public void testGroupByProcessInstrumented() {
        CallStackAnalysisStub cga = getModule();
        CallGraph baseCallGraph = cga.getCallGraph();

        // The first group descriptor is the process
        Collection<ICallStackGroupDescriptor> groupDescriptors = cga.getGroupDescriptors();
        ICallStackGroupDescriptor processGroup = Iterables.getFirst(groupDescriptors, null);
        assertNotNull(processGroup);

        CallGraph callGraph = CallGraphGroupBy.groupCallGraphBy(processGroup, baseCallGraph);
        Collection<ICallStackElement> elements = callGraph.getElements();
        assertEquals(2, elements.size());

        for (ICallStackElement element : elements) {
            switch (element.getName()) {
            case "1": {
                Collection<ICallStackElement> children = element.getChildren();
                assertEquals(0, children.size());
                Collection<AggregatedCallSite> callingContextTree = callGraph.getCallingContextTree(element);
                compareCcts("", getExpectedProcess1(), callingContextTree);
            }
                break;
            case "5": {
                Collection<ICallStackElement> children = element.getChildren();
                assertEquals(0, children.size());
                Collection<AggregatedCallSite> callingContextTree = callGraph.getCallingContextTree(element);
                compareCcts("", getExpectedProcess5(), callingContextTree);
            }
                break;
            default:
                fail("Unexpected element: " + element);
            }
        }
    }

    /**
     * Test the group by leaf level of the call graph
     */
    @Test
    public void testGroupByThreadInstrumented() {
        CallStackAnalysisStub cga = getModule();
        CallGraph baseCallGraph = cga.getCallGraph();

        // The first group descriptor is the process
        Collection<ICallStackGroupDescriptor> groupDescriptors = cga.getGroupDescriptors();
        ICallStackGroupDescriptor group = Iterables.getFirst(groupDescriptors, null);
        assertNotNull(group);
        while (group.getNextGroup() != null) {
            group = group.getNextGroup();
            assertNotNull(group);
        }

        // Group by thread
        CallGraph callGraph = CallGraphGroupBy.groupCallGraphBy(group, baseCallGraph);
        Collection<ICallStackElement> elements = callGraph.getElements();
        assertEquals(2, elements.size());

        for (ICallStackElement element : elements) {
            switch (element.getName()) {
            case "1": {
                Collection<ICallStackElement> children = element.getChildren();
                assertEquals(2, children.size());
                for (ICallStackElement thread : children) {
                    switch (thread.getName()) {
                    case "2": {
                        Collection<AggregatedCallSite> callingContextTree = callGraph.getCallingContextTree(thread);
                        compareCcts("", getExpectedThread2(), callingContextTree);
                    }
                        break;
                    case "3": {
                        Collection<AggregatedCallSite> callingContextTree = callGraph.getCallingContextTree(thread);
                        compareCcts("", getExpectedThread3(), callingContextTree);
                    }
                        break;
                    default:
                        fail("Unexpected thread element: " + thread);
                    }
                }
            }
                break;
            case "5": {
                Collection<ICallStackElement> children = element.getChildren();
                assertEquals(2, children.size());
                for (ICallStackElement thread : children) {
                    switch (thread.getName()) {
                    case "6": {
                        Collection<AggregatedCallSite> callingContextTree = callGraph.getCallingContextTree(thread);
                        compareCcts("", getExpectedThread6(), callingContextTree);
                    }
                        break;
                    case "7": {
                        Collection<AggregatedCallSite> callingContextTree = callGraph.getCallingContextTree(thread);
                        compareCcts("", getExpectedThread7(), callingContextTree);
                    }
                        break;
                    default:
                        fail("Unexpected thread element: " + thread);
                    }
                }
            }
                break;
            default:
                fail("Unexpected element: " + element);
            }
        }

    }

    /**
     * Test changing the grouping for an analysis
     */
    @Test
    public void testMultiGroupBys() {
        // First, group by process
        testGroupByProcessInstrumented();

        // Then, regroup by thread
        testGroupByThreadInstrumented();

        // Then, group by all
        testGroupByAllInstrumented();

        // Group by process again
        testGroupByProcessInstrumented();

        // Group by all
        testGroupByAllInstrumented();

        // Finally by thread
        testGroupByThreadInstrumented();

    }

    private void compareCcts(String prefix, Map<String, CallGraphExpected> expected, Collection<AggregatedCallSite> callingContextTree) {
        assertEquals(expected.size(), callingContextTree.size());
        for (AggregatedCallSite callsite : callingContextTree) {
            assertTrue(callsite instanceof AggregatedCalledFunction);
            AggregatedCalledFunction function = (AggregatedCalledFunction) callsite;
            CallGraphExpected cgExpected = expected.get(callsite.getSymbol().resolve(Collections.emptySet()));
            assertNotNull(cgExpected);
            assertEquals("Callsite " + callsite.getSymbol(), cgExpected.duration, function.getDuration());
            assertEquals("Callsite " + callsite.getSymbol(), cgExpected.selfTime, function.getSelfTime());
            compareCcts(prefix + callsite.getSymbol() + ", ", cgExpected.children, function.getCallees());
        }
    }

}
