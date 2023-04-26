/**********************************************************************
 * Copyright (c) 2020 Draeger, Auriga
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.kernel.ui.views.io.perprocess;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.os.linux.core.model.OsStrings;
import org.eclipse.tracecompass.incubator.internal.kernel.core.io.IoAccessDataProvider;
import org.eclipse.tracecompass.incubator.internal.kernel.core.io.IoAccessDataProviderFactory;
import org.eclipse.tracecompass.incubator.internal.kernel.core.io.IoPerProcessDataProviderFactory;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.AbstractMultiView;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.timegraph.BaseDataProviderTimeGraphMultiViewer;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.xychart.ChartMultiViewer;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.ICoreElementResolver;
import org.eclipse.tracecompass.tmf.core.signal.TmfDataModelSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfGenericTreeEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.ITimeDataProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphColorScheme;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * The Multiview.
 *
 * @author Ivan Grinenko
 *
 */
@SuppressWarnings("restriction")
public class IoByProcessView extends AbstractMultiView {
    /**
     * The view's ID.
     */
    public static final String VIEW_ID = "org.eclipse.tracecompass.kernel.ui.view.iobyprocess"; //$NON-NLS-1$

    private @Nullable Integer fSelectedTid = null;
    private Set<Integer> fCheckedTids = new HashSet<>();

    private @Nullable BaseDataProviderTimeGraphMultiViewer fTgViewer = null;

    private @Nullable TmfViewer fSignalSource;

    /**
     * Constructor.
     */
    public IoByProcessView() {
        super(VIEW_ID);
    }

    /**
     * Handler for the model selected signal. It updates the time graph part of
     * the view with the selected thread ID
     *
     * @param signal
     *            The model selected signal
     */
    @TmfSignalHandler
    public void modelSelectedSignal(TmfDataModelSelectedSignal signal) {
        // Only answer to the signal from this view's tree viewer
        if (signal.getSource() != fSignalSource) {
            return;
        }
        Multimap<String, Object> metadata = signal.getMetadata();
        Collection<Object> collection = metadata.get(OsStrings.tid());
        // FIXME Make sure the signal comes from the corresponding viewer
        if (!collection.isEmpty()) {
            // Update the view
            Object tidObj = collection.iterator().next();
            BaseDataProviderTimeGraphMultiViewer tgViewer = fTgViewer;
            Integer selectedTid = fSelectedTid;
            if (tidObj instanceof Integer && !tidObj.equals(selectedTid) && tgViewer != null) {
                fSelectedTid = (Integer) tidObj;
                tgViewer.triggerRebuild();
            }
        }
    }

    public class IoAccessTimeGraphViewer extends BaseDataProviderTimeGraphMultiViewer {

        public IoAccessTimeGraphViewer(Composite composite) {
            super(composite, new BaseDataProviderTimeGraphPresentationProvider(), getViewSite(), IoAccessDataProviderFactory.DESCRIPTOR.getId());
            TmfSignalManager.register(this);
        }

        @Override
        public void dispose() {
            TmfSignalManager.deregister(this);
            super.dispose();
        }

        @Override
        protected @NonNull Map<String, Object> getFetchTreeParameters() {
            Integer tid = fSelectedTid;
            Set<Integer> tids = new HashSet<>(fCheckedTids);
            if (tid != null) {
                tids.add(tid);
            }
            ITimeDataProvider timeProvider = IoByProcessView.this.getTimeProvider();
            if (tids.isEmpty() || timeProvider == null) {
                return Collections.emptyMap();
            }
            return ImmutableMap.of(IoAccessDataProvider.SELECTED_TID_PARAM, tids,
                    DataProviderParameterUtils.REQUESTED_TIME_KEY, ImmutableList.of(timeProvider.getTime0(), timeProvider.getTime1()));
        }

        @Override
        @TmfSignalHandler
        public void windowRangeUpdated(@Nullable TmfWindowRangeUpdatedSignal signal) {
            // FIXME There's a lot more requests than should be, I think
            if (fSelectedTid != null) {
                triggerRebuild();
            }
        }
    }

    @Override
    protected void partControlCreated(Composite mainComposite, SashForm sashForm) {
        // Add an XY lane:
        ChartMultiViewer chartViewer = addChartViewer(IoPerProcessDataProviderFactory.DESCRIPTOR.getId(), false);
        TmfViewer leftChildViewer = chartViewer.getLeftChildViewer();
        if (leftChildViewer instanceof AbstractSelectTreeViewer2) {
            ((AbstractSelectTreeViewer2) leftChildViewer).addTreeListener(entries -> {
                Set<Object> tids = new HashSet<>();
                entries.stream().filter(e -> e instanceof TmfGenericTreeEntry<?>)
                        .map(e -> ((TmfGenericTreeEntry<?>) e).getModel())
                        .filter(m -> m instanceof ICoreElementResolver)
                        .map(m -> ((ICoreElementResolver) m).getMetadata().get(OsStrings.tid()))
                        .forEach(t -> tids.addAll(t));
                fCheckedTids.clear();
                for (Object tid : tids) {
                    if (tid instanceof Integer) {
                        fCheckedTids.add((Integer) tid);
                    }
                }

            });
        }
        fSignalSource = leftChildViewer;

        // Add a time graph lane
        Composite composite = new Composite(sashForm, SWT.NONE);
        composite.setLayout(new FillLayout());
        composite.setBackground(getColorScheme().getColor(TimeGraphColorScheme.BACKGROUND));
        BaseDataProviderTimeGraphMultiViewer tgViewer = new IoAccessTimeGraphViewer(composite);

        tgViewer.init();
        addLane(tgViewer);
        fTgViewer = tgViewer;
    }

}
