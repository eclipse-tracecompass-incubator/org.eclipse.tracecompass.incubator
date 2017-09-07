/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.xml.callstack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICallStackSymbol;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraph;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.ICallGraphProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.IFlameChartProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph.CallGraphAnalysis;
import org.eclipse.tracecompass.incubator.internal.callstack.core.xml.callstack.CallstackXmlModuleHelper.ISubModuleHelper;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.TmfXmlStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.TmfXmlUtils;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfAnalysisModuleWithStateSystems;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 *
 * @author Geneviève Bastien
 */
public class CallstackXmlAnalysis extends TmfAbstractAnalysisModule implements IFlameChartProvider, ITmfAnalysisModuleWithStateSystems, ICallGraphProvider {

    private final Path fSourceFile;
    private final ISubModuleHelper fHelper;
    private @Nullable IAnalysisModule fModule = null;
    private @Nullable Collection<CallStackSeries> fCallStacks = null;
    private final CallGraphAnalysis fCallGraph;

    /**
     * Constructor
     *
     * @param sourceFile
     *            The source file containing this callstack analysis
     * @param helper
     *            The helper for the dependent module
     */
    public CallstackXmlAnalysis(Path sourceFile, ISubModuleHelper helper) {
        super();
        fSourceFile = sourceFile;
        fHelper = helper;
        fCallGraph = new CallGraphAnalysis(this);
    }

    @Override
    public Collection<CallStackSeries> getCallStackSeries() {
        Collection<CallStackSeries> callstacks = fCallStacks;
        if (callstacks == null) {
            IAnalysisModule module = getAnalysisModule();
            if (!(module instanceof ITmfAnalysisModuleWithStateSystems)) {
                return Collections.EMPTY_SET;
            }
            Iterator<@NonNull ITmfStateSystem> stateSystems = ((ITmfAnalysisModuleWithStateSystems) module).getStateSystems().iterator();
            if (!stateSystems.hasNext()) {
                return Collections.EMPTY_SET;
            }
            ITmfStateSystem ss = stateSystems.next();
            Path xmlFile = fSourceFile;
            final String pathString = xmlFile.toString();
            Element doc = TmfXmlUtils.getElementInFile(pathString, CallstackXmlStrings.CALLSTACK, getId());
            if (doc == null) {
                fCallStacks = Collections.EMPTY_SET;
                return Collections.EMPTY_SET;
            }

            /* parser for defined Fields */
            NodeList callstackNodes = doc.getElementsByTagName(CallstackXmlStrings.CALLSTACK_GROUP);
            NodeList childNodes = doc.getChildNodes();
            for (int i = 0; i<childNodes.getLength(); i++) {
                Node item = childNodes.item(i);
                System.out.println(item.getNodeName());
            }
            callstacks = new ArrayList<>();
            for (int i = 0; i < callstackNodes.getLength(); i++) {
                Element element = (Element) callstackNodes.item(i);
                List<String[]> patterns = new ArrayList<>();
                for (Element child : TmfXmlUtils.getChildElements(element, CallstackXmlStrings.CALLSTACK_LEVEL)) {
                    String attribute = child.getAttribute(CallstackXmlStrings.CALLSTACK_PATH);
                    patterns.add(attribute.split("/")); //$NON-NLS-1$
                }

                // Build the thread resolver
                List<Element> childElements = TmfXmlUtils.getChildElements(element, CallstackXmlStrings.CALLSTACK_THREAD);
                IThreadIdResolver resolver = null;
                if (childElements.size() > 0) {
                    Element threadElement = childElements.get(0);
                    String attribute = threadElement.getAttribute(CallstackXmlStrings.CALLSTACK_THREADCPU);
                    if (!attribute.isEmpty()) {
                        resolver = new CallStackSeries.CpuResolver(attribute.split("/")); //$NON-NLS-1$
                    } else {
                        attribute = threadElement.getAttribute(CallstackXmlStrings.CALLSTACK_THREADLEVEL);
                        if (!attribute.isEmpty()) {
                            resolver = new CallStackSeries.AttributeNameThreadResolver(Integer.valueOf(attribute));
                        }
                    }
                }

                callstacks.add(new CallStackSeries(ss, patterns, 0, element.getAttribute(TmfXmlStrings.NAME), getHostId(), resolver));
            }
            fCallStacks = callstacks;
        }
        return callstacks;
    }

    @Override
    public String getHostId() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return ""; //$NON-NLS-1$
        }
        return trace.getHostId();
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        IAnalysisModule analysisModule = getAnalysisModule();
        if (analysisModule == null) {
            return false;
        }
        boolean ret = analysisModule.waitForCompletion(monitor);
        if (!ret) {
            return ret;
        }
        fCallGraph.schedule();
        return true;
    }

    @Override
    protected void canceling() {
        IAnalysisModule analysisModule = getAnalysisModule();
        if (analysisModule != null) {
            analysisModule.cancel();
        }
        fCallGraph.cancel();
    }

    @Override
    public void dispose() {
        /*
         * The sub-analyses are not registered to the trace directly, so we need
         * to tell them when the trace is disposed.
         */
        super.dispose();
        IAnalysisModule analysisModule = getAnalysisModule();
        if (analysisModule != null) {
            analysisModule.dispose();
        }
        fCallGraph.dispose();
    }

    @Override
    public boolean setTrace(@NonNull ITmfTrace trace) throws TmfAnalysisException {
        if (!super.setTrace(trace)) {
            return false;
        }
        return fCallGraph.setTrace(trace);
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        fCallGraph.setName(name);
    }

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new NullPointerException("Trace should not be null at this point"); //$NON-NLS-1$
        }
        IAnalysisModule module = getAnalysisModule();

        if (module == null) {
            return Collections.emptyList();
        }
        return Collections.singleton(module);

    }

    private @Nullable IAnalysisModule getAnalysisModule() {
        IAnalysisModule module = fModule;
        if (module == null) {
            ITmfTrace trace = getTrace();
            if (trace == null) {
                return null;
            }
            module = fHelper.getAnalysis(trace);
            if (module != null) {
                fModule = module;
            }
        }
        return module;
    }

    @Override
    public @Nullable ITmfStateSystem getStateSystem(String id) {
        IAnalysisModule analysisModule = getAnalysisModule();
        if (analysisModule instanceof ITmfAnalysisModuleWithStateSystems) {
            return ((ITmfAnalysisModuleWithStateSystems) analysisModule).getStateSystem(id);
        }
        return null;
    }

    @Override
    public Iterable<ITmfStateSystem> getStateSystems() {
        IAnalysisModule analysisModule = getAnalysisModule();
        if (analysisModule instanceof ITmfAnalysisModuleWithStateSystems) {
            return ((ITmfAnalysisModuleWithStateSystems) analysisModule).getStateSystems();
        }
        return Collections.emptyList();
    }

    @Override
    public boolean waitForInitialization() {
        IAnalysisModule analysisModule = getAnalysisModule();
        if (analysisModule instanceof ITmfAnalysisModuleWithStateSystems) {
            return ((ITmfAnalysisModuleWithStateSystems) analysisModule).waitForInitialization();
        }
        return false;
    }

    @Override
    public Collection<ICallStackGroupDescriptor> getGroupDescriptors() {
        return fCallGraph.getGroupDescriptors();
    }

    @Override
    public CallGraph getCallGraph(ITmfTimestamp start, ITmfTimestamp end) {
        return fCallGraph.getCallGraph(start, end);
    }

    @Override
    public CallGraph getCallGraph() {
        return fCallGraph.getCallGraph();
    }

    @Override
    public AggregatedCallSite createCallSite(ICallStackSymbol symbol) {
        return fCallGraph.createCallSite(symbol);
    }

}
