package org.eclipse.tracecompass.incubator.opentracing.core.analysis.callstack;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICallStackSymbol;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ProcessStatusInterval;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraph;
import org.eclipse.tracecompass.incubator.callstack.core.flamechart.CallStack;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.ICalledFunction;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.IFlameChartProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries;
import org.eclipse.tracecompass.incubator.callstack.core.symbol.CallStackSymbolFactory;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.InstrumentedCallStackElement;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph.AbstractCalledFunction;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph.AggregatedCalledFunction;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph.CallGraphAnalysis;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;

/**
 * Call Graph Analysis for concurrent traces
 * @author Fateme Faraji Daneshgar
 *
 */
public class SpanCallGraphAnalysis extends CallGraphAnalysis {
    private boolean fHasKernelStatuses = false;
    public static final String Span = "span:";
    public static final String parentSpan = "parentSpan:";
    public static final String func = "opName:";



    /**
     * @param csProvider
     */
    public SpanCallGraphAnalysis(IFlameChartProvider csProvider) {
        super(csProvider);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected boolean iterateOverCallstackSerie(CallStackSeries callstackSerie, IHostModel model, CallGraph callgraph, long start, long end, IProgressMonitor monitor) {
        // The root elements are the same as the one from the callstack series
        Collection<ICallStackElement> rootElements = callstackSerie.getRootElements();
        for (ICallStackElement element : rootElements) {
            if (monitor.isCanceled()) {
                return false;
            }
            iterateOverElement(element, model, callgraph, start, end, monitor);
        }
        return true;
    }

    private void iterateOverElement(ICallStackElement element, IHostModel model, CallGraph callgraph, long start, long end, IProgressMonitor monitor) {
        // Iterator over the children of the element until we reach the leaves
        if (element.isLeaf()) {
            iterateOverLeafElement(element, model, callgraph, start, end, monitor);
            return;
        }
        for (ICallStackElement child : element.getChildrenElements()) {
            iterateOverElement(child, model, callgraph, start, end, monitor);
        }
    }

    private void iterateOverLeafElement(ICallStackElement element, IHostModel model, CallGraph callgraph, long start, long end, IProgressMonitor monitor) {
        if (!(element instanceof InstrumentedCallStackElement)) {
            throw new IllegalStateException("Call Graph Analysis: The element does not have the right type"); //$NON-NLS-1$
        }
        InstrumentedCallStackElement insElement = (InstrumentedCallStackElement) element;

        CallStack callStack = insElement.getCallStack();

        // If there is no children for this callstack, just return
        if (callStack.getMaxDepth() == 0) {
            return;
        }
        fHasKernelStatuses |= callStack.hasKernelStatuses();
        // Start with the first function
        AbstractCalledFunction nextFunction = (AbstractCalledFunction) callStack.getNextFunction(callStack.getStartTime(), 1, null, model, start, end);
        while (nextFunction != null) {
            AggregatedCalledFunction aggregatedChild = createSpanCallSite(CallStackSymbolFactory.createSymbol(getFuncName(nextFunction.getSymbol()), element, nextFunction.getStart()));
            iterateOverCallstack(element, callStack, nextFunction, 2, aggregatedChild, model, start, end, monitor);
            aggregatedChild.addFunctionCall(nextFunction);
            Iterable<ProcessStatusInterval> kernelStatuses = callStack.getKernelStatuses(nextFunction, Collections.emptyList());
            for (ProcessStatusInterval status : kernelStatuses) {
                aggregatedChild.addKernelStatus(status);
            }
            callgraph.addAggregatedCallSite(element, aggregatedChild);
            nextFunction = (AbstractCalledFunction) callStack.getNextFunction(nextFunction.getEnd(), 1, null, model, start, end);
        }
    }

    private void iterateOverCallstack(ICallStackElement element, CallStack callstack, ICalledFunction function, int nextLevel, AggregatedCalledFunction aggregatedCall, IHostModel model, long start, long end, IProgressMonitor monitor) {
        if (nextLevel > callstack.getMaxDepth()) {
            return;
        }
        AbstractCalledFunction nextFunction = (AbstractCalledFunction) callstack.getNextFunction(function.getStart(), nextLevel, function, model, Math.max(function.getStart(), start), Math.min(function.getEnd(), end));
        int level = nextLevel;
        String funcSpanId = getSpanId(function.getSymbol());
        while (nextFunction !=null) {
            String nextFuncParentId = getParentId(nextFunction.getSymbol());
            if (!funcSpanId.equals(nextFuncParentId)) {
                nextFunction = (AbstractCalledFunction) callstack.getNextFunction(nextFunction.getEnd()+1, level, function, model, Math.max(function.getStart(), start), Math.min(function.getEnd(), end));
                continue;
                }

            if (nextFunction !=null) {
                ((AbstractCalledFunction) function).addChild(nextFunction);
                AggregatedCalledFunction aggregatedChild = createSpanCallSite(CallStackSymbolFactory.createSymbol(getFuncName(nextFunction.getSymbol()), element, nextFunction.getStart()));
                iterateOverCallstack(element, callstack, nextFunction, level+1, aggregatedChild, model, start, end, monitor);
                aggregatedCall.addChild( nextFunction, aggregatedChild);

                nextFunction = (AbstractCalledFunction) callstack.getNextFunction(nextFunction.getEnd()+1, level, function, model, Math.max(function.getStart(), start), Math.min(function.getEnd(), end));

            }
    }

    }
    private static String getParentId(@NonNull Object object) {
        String symbol = String.valueOf(object);
        int indx = symbol.indexOf(parentSpan);
        int lastIndx = symbol.indexOf(",o");
        if (indx ==-1) {
            return null;
        }
        return symbol.substring(indx+parentSpan.length(),lastIndx);
    }

    private static String getSpanId(@NonNull Object object) {
        String symbol = String.valueOf(object);
        int indx = symbol.indexOf(Span);
        int lastIndx = symbol.indexOf(",");
        if (indx ==-1) {
            return null;
        }
        return symbol.substring(indx+Span.length(),lastIndx);

    }

    private static String getFuncName(@NonNull Object object) {
        String symbol = String.valueOf(object);
        int indx = symbol.indexOf(func);
        int lastIndx = symbol.indexOf("]");
        if (indx ==-1) {
            return null;
        }
        return symbol.substring(indx+func.length(),lastIndx);

    }

    @Override
    public List<String> getExtraDataSets() {
        if (fHasKernelStatuses) {
            return Collections.singletonList(String.valueOf(org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.Messages.FlameChartDataProvider_KernelStatusTitle));
        }
        //return ICallGraphProvider.super.getExtraDataSets();
        return Collections.emptyList();
    }


    public AggregatedCalledFunction createSpanCallSite(Object symbol) {
        return new AggregatedCalledFunction((ICallStackSymbol) symbol);
    }

    @Override
    public void dispose() {
        super.dispose();
        TmfSignalManager.deregister(this);

    }


}