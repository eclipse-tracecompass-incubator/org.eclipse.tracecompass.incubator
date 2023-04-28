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

package org.eclipse.tracecompass.incubator.internal.scripting.ui.views.xychart;

import java.util.Comparator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfGenericTreeEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;

import com.google.common.collect.ImmutableList;

/**
 * Tree Viewer for the {@link ScriptedXYView}
 *
 * @author Benjamin Saint-Cyr
 */
public class ScriptedXYTreeViewer extends AbstractSelectTreeViewer2 {

    private final String fProviderId;

    private class ScriptedLabelProvider extends TreeLabelProvider {
        @Override
        public @Nullable Image getColumnImage(@Nullable Object element, int columnIndex) {
            if (columnIndex == 1 && element instanceof TmfGenericTreeEntry && isChecked(element)) {
                TmfTreeDataModel model = ((TmfGenericTreeEntry<TmfTreeDataModel>) element).getModel();
                if (model.getParentId() < 0) {
                    // do not show the legend for the trace entries.
                    return null;
                }
                return getLegendImage(model.getId());
            }
            return null;
        }
    }

    /**
     * Constructor
     *
     * @param parent
     *            Parent composite
     * @param name
     *            Secondary ID name
     */
    public ScriptedXYTreeViewer(Composite parent, String name) {
        super(parent, 1, name);
        setLabelProvider(new ScriptedLabelProvider());
        fProviderId = name;
    }

    @Override
    protected ITmfTreeColumnDataProvider getColumnDataProvider() {
        return () -> ImmutableList.of(
                createColumn(Messages.ScriptedXYTreeViewer_Name, Comparator.comparing(TmfGenericTreeEntry::getName)),
                new TmfTreeColumnData(Messages.ScriptedXYTreeViewer_Legend));
    }

    @Override
    protected ITmfTreeDataProvider<@NonNull ITmfTreeDataModel> getProvider(ITmfTrace trace) {
        if (fProviderId == null) {
            return null;
        }
        return DataProviderManager
                .getInstance().getOrCreateDataProvider(trace, fProviderId, ITmfTreeDataProvider.class);
    }

}
