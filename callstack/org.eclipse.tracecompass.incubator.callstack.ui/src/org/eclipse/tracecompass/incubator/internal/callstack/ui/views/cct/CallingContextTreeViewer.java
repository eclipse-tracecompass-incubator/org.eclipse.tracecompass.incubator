/*******************************************************************************
 * Copyright (c) 2015, 2017 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.callstack.ui.views.cct;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.SubSecondTimeWithUnitFormat;
import org.eclipse.tracecompass.common.core.StreamUtils;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraph;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.ICallGraphProvider;
import org.eclipse.tracecompass.incubator.callstack.core.sampled.callgraph.AggregatedStackTraces;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph.AggregatedCalledFunction;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractTmfTreeViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData.ITmfColumnPercentageProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;

/**
 * An abstract tree viewer implementation for displaying segment store
 * statistics
 *
 * @author Geneviève Bastien
 */
public class CallingContextTreeViewer extends AbstractTmfTreeViewer {

    private static final Format TIME_FORMATTER = new SubSecondTimeWithUnitFormat();
    private static final Format DECIMAL_FORMATTER = new DecimalFormat("###,###.##"); //$NON-NLS-1$
    // Order CCT children by decreasing length
    private static final Comparator<CCTCallSiteEntry> COMPARATOR = (o1, o2) -> Long.compare(o2.getCallSite().getLength(), o1.getCallSite().getLength());

    private MenuManager fTablePopupMenuManager;
    private String fAnalysisId;

    private Collection<ISymbolProvider> fSymbolProviders = Collections.emptyList();

    private static final String[] COLUMN_NAMES = new String[] {
            checkNotNull(Messages.CallingContextTreeViewer_CallSite),
            checkNotNull(Messages.CallingContextTreeViewer_NbCalls),
            checkNotNull(Messages.CallingContextTreeViewer_Duration),
            checkNotNull(Messages.CallingContextTreeViewer_SelfTime),
            checkNotNull(Messages.CallingContextTreeViewer_CpuTime)
    };

    /**
     * Constructor
     *
     * @param parent
     *            the parent composite
     * @param analysisId
     *            The ID of the analysis to use to fill this CCT
     */
    public CallingContextTreeViewer(@Nullable Composite parent, String analysisId) {
        super(parent, false);
        fAnalysisId = analysisId;
        setLabelProvider(new CallingContextTreeLabelProvider());
        fTablePopupMenuManager = new MenuManager();
        fTablePopupMenuManager.setRemoveAllWhenShown(true);
        fTablePopupMenuManager.addMenuListener(manager -> {
            TreeViewer viewer = getTreeViewer();
            ISelection selection = viewer.getSelection();
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection sel = (IStructuredSelection) selection;
                if (manager != null) {
                    appendToTablePopupMenu(manager, sel);
                }
            }
        });
        Menu tablePopup = fTablePopupMenuManager.createContextMenu(getTreeViewer().getTree());
        Tree tree = getTreeViewer().getTree();
        tree.setMenu(tablePopup);
    }

    /** Provides label for the Segment Store tree viewer cells */
    protected static class CallingContextTreeLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(@Nullable Object element, int columnIndex) {
            String value = ""; //$NON-NLS-1$
            if (element instanceof HiddenTreeViewerEntry) {
                if (columnIndex == 0) {
                    value = ((HiddenTreeViewerEntry) element).getName();
                }
            } else if (element instanceof CCTElementEntry) {
                CCTElementEntry entry = (CCTElementEntry) element;
                if (columnIndex == 0) {
                    return String.valueOf(entry.getName());
                }
                value = StringUtils.EMPTY;
            } else if (element instanceof CCTCallSiteEntry) {
                CCTCallSiteEntry entry = (CCTCallSiteEntry) element;
                if (columnIndex == 0) {
                    return String.valueOf(entry.getName());
                }
                AggregatedCallSite callSite = entry.getCallSite();
                if (callSite instanceof AggregatedCalledFunction) {
                    value = getStringForColumn((AggregatedCalledFunction) callSite, columnIndex);
                } else if (callSite instanceof AggregatedStackTraces) {
                    value = getStringForColumn((AggregatedStackTraces) callSite, columnIndex);
                }
            }
            return checkNotNull(value);
        }

        private static String getStringForColumn(AggregatedStackTraces callsite, int columnIndex) {
            if (columnIndex == 1) {
                return String.format("%s", DECIMAL_FORMATTER.format(callsite.getLength())); //$NON-NLS-1$
            }
            return StringUtils.EMPTY;
        }

        private static String getStringForColumn(AggregatedCalledFunction callsite, int columnIndex) {
            if (columnIndex == 1) {
                return String.format("%s", DECIMAL_FORMATTER.format(callsite.getNbCalls())); //$NON-NLS-1$
            } else if (columnIndex == 2) {
                return toFormattedString(callsite.getDuration());
            } else if (columnIndex == 3) {
                return toFormattedString(callsite.getSelfTime());
            } else if (columnIndex == 4) {
                long cpuTime = callsite.getCpuTime();
                if (cpuTime != IHostModel.TIME_UNKNOWN) {
                    return toFormattedString(cpuTime);
                }
            }
            return StringUtils.EMPTY;
        }
    }

    private static class CCTPercentageProvider implements ITmfColumnPercentageProvider {

        @Override
        public double getPercentage(@Nullable Object data) {
            double value = 0;
            if (data instanceof CCTCallSiteEntry) {
                CCTCallSiteEntry entry = (CCTCallSiteEntry) data;

                AggregatedCallSite callSite = entry.getCallSite();

                // Find the total length from the parent
                ITmfTreeViewerEntry parentEntry = entry;
                while (parentEntry != null && !(parentEntry instanceof CCTElementEntry)) {
                    parentEntry = parentEntry.getParent();
                }
                if (parentEntry != null) {
                    value = (double) callSite.getLength() / ((CCTElementEntry) parentEntry).getTotalLength();
                }
            }
            return value;
        }
    }

    @Override
    protected ITmfTreeColumnDataProvider getColumnDataProvider() {
        return new ITmfTreeColumnDataProvider() {

            @Override
            public List<@Nullable TmfTreeColumnData> getColumnData() {
                /* All columns are sortable */
                List<@Nullable TmfTreeColumnData> columns = new ArrayList<>();
                TmfTreeColumnData column = new TmfTreeColumnData(COLUMN_NAMES[0]);
                column.setAlignment(SWT.LEFT);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                        if ((!(e1 instanceof CCTCallSiteEntry)) || (!(e2 instanceof CCTCallSiteEntry))) {
                            return 0;
                        }

                        CCTCallSiteEntry n1 = (CCTCallSiteEntry) e1;
                        CCTCallSiteEntry n2 = (CCTCallSiteEntry) e2;

                        return n1.getName().compareTo(n2.getName());
                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(COLUMN_NAMES[1]);
                column.setAlignment(SWT.RIGHT);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                        if ((!(e1 instanceof CCTCallSiteEntry)) || (!(e2 instanceof CCTCallSiteEntry))) {
                            return 0;
                        }

                        CCTCallSiteEntry n1 = (CCTCallSiteEntry) e1;
                        CCTCallSiteEntry n2 = (CCTCallSiteEntry) e2;

                        AggregatedCallSite callsite1 = n1.getCallSite();
                        AggregatedCallSite callsite2 = n2.getCallSite();

                        if ((callsite1 instanceof AggregatedCalledFunction) && (callsite2 instanceof AggregatedCalledFunction)) {
                            return Long.compare(((AggregatedCalledFunction) callsite1).getNbCalls(), ((AggregatedCalledFunction) callsite2).getNbCalls());
                        }
                        if ((callsite1 instanceof AggregatedStackTraces) && (callsite2 instanceof AggregatedStackTraces)) {
                            return Long.compare(((AggregatedStackTraces) callsite1).getLength(), ((AggregatedStackTraces) callsite2).getLength());
                        }
                        if ((callsite1 instanceof AggregatedCalledFunction) && (callsite2 instanceof AggregatedStackTraces)) {
                            return 1;
                        }
                        if ((callsite1 instanceof AggregatedStackTraces) && (callsite2 instanceof AggregatedCalledFunction)) {
                            return -1;
                        }
                        return 0;
                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(COLUMN_NAMES[2]);
                column.setPercentageProvider(new CCTPercentageProvider());
                column.setAlignment(SWT.RIGHT);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                        if ((!(e1 instanceof CCTCallSiteEntry)) || (!(e2 instanceof CCTCallSiteEntry))) {
                            return 0;
                        }

                        CCTCallSiteEntry n1 = (CCTCallSiteEntry) e1;
                        CCTCallSiteEntry n2 = (CCTCallSiteEntry) e2;

                        AggregatedCallSite callsite1 = n1.getCallSite();
                        AggregatedCallSite callsite2 = n2.getCallSite();

                        if ((callsite1 instanceof AggregatedCalledFunction) && (callsite2 instanceof AggregatedCalledFunction)) {
                            return Long.compare(((AggregatedCalledFunction) callsite1).getDuration(), ((AggregatedCalledFunction) callsite2).getDuration());
                        }
                        if ((callsite1 instanceof AggregatedCalledFunction) && (callsite2 instanceof AggregatedStackTraces)) {
                            return 1;
                        }
                        if ((callsite1 instanceof AggregatedStackTraces) && (callsite2 instanceof AggregatedCalledFunction)) {
                            return -1;
                        }
                        return 0;
                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(COLUMN_NAMES[3]);
                column.setAlignment(SWT.RIGHT);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                        if ((!(e1 instanceof CCTCallSiteEntry)) || (!(e2 instanceof CCTCallSiteEntry))) {
                            return 0;
                        }

                        CCTCallSiteEntry n1 = (CCTCallSiteEntry) e1;
                        CCTCallSiteEntry n2 = (CCTCallSiteEntry) e2;

                        AggregatedCallSite callsite1 = n1.getCallSite();
                        AggregatedCallSite callsite2 = n2.getCallSite();

                        if ((callsite1 instanceof AggregatedCalledFunction) && (callsite2 instanceof AggregatedCalledFunction)) {
                            return Long.compare(((AggregatedCalledFunction) callsite1).getSelfTime(), ((AggregatedCalledFunction) callsite2).getSelfTime());
                        }
                        if ((callsite1 instanceof AggregatedCalledFunction) && (callsite2 instanceof AggregatedStackTraces)) {
                            return 1;
                        }
                        if ((callsite1 instanceof AggregatedStackTraces) && (callsite2 instanceof AggregatedCalledFunction)) {
                            return -1;
                        }
                        return 0;
                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(COLUMN_NAMES[4]);
                column.setAlignment(SWT.RIGHT);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                        if ((!(e1 instanceof CCTCallSiteEntry)) || (!(e2 instanceof CCTCallSiteEntry))) {
                            return 0;
                        }

                        CCTCallSiteEntry n1 = (CCTCallSiteEntry) e1;
                        CCTCallSiteEntry n2 = (CCTCallSiteEntry) e2;

                        AggregatedCallSite callsite1 = n1.getCallSite();
                        AggregatedCallSite callsite2 = n2.getCallSite();

                        if ((callsite1 instanceof AggregatedCalledFunction) && (callsite2 instanceof AggregatedCalledFunction)) {
                            return Long.compare(((AggregatedCalledFunction) callsite1).getCpuTime(), ((AggregatedCalledFunction) callsite2).getCpuTime());
                        }
                        if ((callsite1 instanceof AggregatedCalledFunction) && (callsite2 instanceof AggregatedStackTraces)) {
                            return 1;
                        }
                        if ((callsite1 instanceof AggregatedStackTraces) && (callsite2 instanceof AggregatedCalledFunction)) {
                            return -1;
                        }
                        return 0;
                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(""); //$NON-NLS-1$
                columns.add(column);
                return columns;
            }

        };
    }

    private Set<ICallGraphProvider> getCallGraphs() {
        ITmfTrace trace = getTrace();
        if (trace != null) {
            Iterable<ICallGraphProvider> callgraphModules = TmfTraceUtils.getAnalysisModulesOfClass(trace, ICallGraphProvider.class);

            return StreamUtils.getStream(callgraphModules)
            .filter(m -> {
                if (m instanceof IAnalysisModule) {
                    return ((IAnalysisModule) m).getId().equals(fAnalysisId);
                }
                return true;
            })
            .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @Override
    public void initializeDataSource() {
        ITmfTrace trace = getTrace();
        if (trace != null) {
            Set<ICallGraphProvider> modules = getCallGraphs();

            fSymbolProviders  = SymbolProviderManager.getInstance().getSymbolProviders(trace);
            modules.forEach(m -> {
                if (m instanceof IAnalysisModule) {
                    ((IAnalysisModule) m).schedule();
                }
            });
        }
    }

    /**
     * Method to add commands to the context sensitive menu.
     *
     * @param manager
     *            the menu manager
     * @param sel
     *            the current selection
     */
    protected void appendToTablePopupMenu(IMenuManager manager, IStructuredSelection sel) {

    }

    /**
     * Formats a double value string
     *
     * @param value
     *            a value to format
     * @return formatted value
     */
    protected static String toFormattedString(double value) {
        return String.format("%s", TIME_FORMATTER.format(value)); //$NON-NLS-1$
    }

    /**
     * Class for defining an entry in the statistics tree.
     */
    protected class CCTElementEntry extends TmfTreeViewerEntry {

        private final ICallStackElement fElement;
        private final CallGraph fCallGraph;
        private @Nullable List<ITmfTreeViewerEntry> fChildren = null;

        /**
         * Constructor
         *
         * @param element
         *            The callstack element of this hierarchy
         * @param callgraph
         *            The call graph object
         */
        public CCTElementEntry(ICallStackElement element, CallGraph callgraph) {
            super(element.getName());
            fElement = element;
            fCallGraph = callgraph;
        }

        /**
         * Gets the statistics object
         *
         * @return statistics object
         */
        public ICallStackElement getElement() {
            return fElement;
        }

        @Override
        public boolean hasChildren() {
            return true;
        }

        @Override
        public List<ITmfTreeViewerEntry> getChildren() {
            List<ITmfTreeViewerEntry> children = fChildren;
            if (children == null) {
                if (fElement.isLeaf()) {
                    children = getChildrenCallSites();
                } else {
                    children = getChildrenElements();
                }

                fChildren = children;
            }
            return children;
        }

        /**
         * Get the total length for the callsites children of this element. This
         * is used for percentages
         *
         * @return The total length of the children callsites
         */
        public long getTotalLength() {
            List<ITmfTreeViewerEntry> childrenCallSites = getChildrenCallSites();
            long length = 0L;
            for (ITmfTreeViewerEntry callsiteEntry : childrenCallSites) {
                length += ((CCTCallSiteEntry) callsiteEntry).getCallSite().getLength();
            }
            return length;
        }

        private List<ITmfTreeViewerEntry> getChildrenElements() {
            List<ITmfTreeViewerEntry> list = new ArrayList<>();
            Collection<ICallStackElement> children = fElement.getChildren();
            for (ICallStackElement elem : children) {
                list.add(new CCTElementEntry(elem, fCallGraph));
            }
            return list;
        }

        private List<ITmfTreeViewerEntry> getChildrenCallSites() {
            List<ITmfTreeViewerEntry> list = new ArrayList<>();
            Collection<AggregatedCallSite> cct = fCallGraph.getCallingContextTree(fElement);
            for (AggregatedCallSite callsite : cct) {
                list.add(new CCTCallSiteEntry(callsite, fCallGraph, this));
            }
            return list;
        }

    }

    /**
     * Class for defining an entry in the statistics tree.
     */
    protected class CCTCallSiteEntry extends TmfTreeViewerEntry {

        private final AggregatedCallSite fCallSite;
        private final CallGraph fCallGraph;
        private @Nullable List<ITmfTreeViewerEntry> fChildren = null;

        /**
         * Constructor
         *
         * @param callsite
         *            The callsite corresponding to this entry
         * @param callGraph
         *            The call graph provider object
         * @param parent
         *            The parent element
         */
        public CCTCallSiteEntry(AggregatedCallSite callsite, CallGraph callGraph, TmfTreeViewerEntry parent) {
            super(callsite.getSymbol().resolve(fSymbolProviders));
            fCallSite = callsite;
            fCallGraph = callGraph;
            this.setParent(parent);
        }

        /**
         * Gets the statistics object
         *
         * @return statistics object
         */
        public AggregatedCallSite getCallSite() {
            return fCallSite;
        }

        @Override
        public boolean hasChildren() {
            return !fCallSite.getCallees().isEmpty();
        }

        @Override
        public List<ITmfTreeViewerEntry> getChildren() {
            List<ITmfTreeViewerEntry> children = fChildren;
            if (children == null) {
                List<CCTCallSiteEntry> cctChildren = new ArrayList<>();
               for (AggregatedCallSite callsite : fCallSite.getCallees()) {
                    CCTCallSiteEntry entry = new CCTCallSiteEntry(callsite, fCallGraph, this);
                    int index = Collections.binarySearch(cctChildren, entry, COMPARATOR);
                    cctChildren.add((index < 0 ? -index - 1 : index), entry);
                }
                children = new ArrayList<>(cctChildren);
                fChildren = children;
            }
            return children;
        }

    }

    @Override
    protected @Nullable ITmfTreeViewerEntry updateElements(long start, long end, boolean isSelection) {

        Set<ICallGraphProvider> modules = getCallGraphs();

        if (modules.isEmpty()) {
            return null;
        }
        modules.forEach(m -> {
            if (m instanceof IAnalysisModule) {
                ((IAnalysisModule) m).waitForCompletion();
            }
        });

        TmfTreeViewerEntry root = new TmfTreeViewerEntry(""); //$NON-NLS-1$
        List<ITmfTreeViewerEntry> entryList = root.getChildren();

        for (ICallGraphProvider module : modules) {
            if (isSelection) {
                setStats(start, end, entryList, module, true, new NullProgressMonitor());
            }
            // Start, start to ensure the full callgraph will be returned
            setStats(start, start, entryList, module, false, new NullProgressMonitor());
        }
        return root;
    }

    /**
     * TODO: Implement this if necessary
     *
     * @param start
     * @param end
     * @param isSelection
     * @param monitor
     */
    private void setStats(long start, long end, List<ITmfTreeViewerEntry> entryList, ICallGraphProvider module, boolean isSelection, IProgressMonitor monitor) {

        CallGraph callGraph = null;
        if (start != end) {
            callGraph = module.getCallGraph(TmfTimestamp.fromNanos(start), TmfTimestamp.fromNanos(end));
        } else {
            callGraph = module.getCallGraph();
        }
        Collection<ICallStackElement> elements = callGraph.getElements();

        for (ICallStackElement element : elements) {
            CCTElementEntry entry = new CCTElementEntry(element, callGraph);
            entryList.add(entry);
        }
    }

    @Override
    @TmfSignalHandler
    public void windowRangeUpdated(@Nullable TmfWindowRangeUpdatedSignal signal) {
        // Do nothing. We do not want to update the view and lose the selection
        // if the window range is updated with current selection outside of this
        // new range.
    }

    /**
     * Get the type label
     *
     * @return the label
     * @since 1.2
     */
    protected String getTypeLabel() {
        return checkNotNull("type");
    }

    /**
     * Get the total column label
     *
     * @return the totals column label
     * @since 1.2
     */
    protected String getTotalLabel() {
        return checkNotNull("label");
    }

    /**
     * Get the selection column label
     *
     * @return The selection column label
     * @since 1.2
     */
    protected String getSelectionLabel() {
        return checkNotNull("selection");
    }

    /**
     * Class to define a level in the tree that doesn't have any values.
     */
    protected class HiddenTreeViewerEntry extends TmfTreeViewerEntry {
        /**
         * Constructor
         *
         * @param name
         *            the name of the level
         */
        public HiddenTreeViewerEntry(String name) {
            super(name);
        }
    }

}
