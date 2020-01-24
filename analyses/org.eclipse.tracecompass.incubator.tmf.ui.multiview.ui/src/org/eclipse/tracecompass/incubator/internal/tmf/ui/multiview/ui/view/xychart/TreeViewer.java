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

package org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.xychart;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfGenericTreeEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;

/**
 * The tree viewer part (left side) of the view.
 *
 * @author Ivan Grinenko
 */
public class TreeViewer extends AbstractSelectTreeViewer {

    private static final int COL_INDEX_COLOR_LEGEND = 2;

    /**
     * @param parent
     *            - The parent component.
     * @param id
     *            - The Provider ID.
     */
    public TreeViewer(Composite parent, String id) {
        super(parent, TreeViewer.COL_INDEX_COLOR_LEGEND, id);
        setLabelProvider(new TreeXyLabelProvider());
    }

    @Override
    protected ITmfTreeColumnDataProvider getColumnDataProvider() {
        return () -> Arrays.asList(createColumn("Name", Comparator.comparing(ITmfTreeViewerEntry::getName)),
                new TmfTreeColumnData("Unit"), new TmfTreeColumnData("Legend"));
    }

    @Override
    protected void updateContent(long start, long end, boolean isSelection) {
        super.updateContent(start, end, isSelection);
    }

    private final class TreeXyLabelProvider extends TreeLabelProvider {

        private Optional<ITmfTreeDataModel> tryGetModel(Object element) {
            if (element instanceof TmfGenericTreeEntry<?>) {
                ITmfTreeDataModel model = ((TmfGenericTreeEntry<?>) element).getModel();
                return Optional.ofNullable(model);
            }
            return Optional.empty();
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            // Any other column
            return super.getColumnText(element, columnIndex);
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            if (columnIndex == TreeViewer.COL_INDEX_COLOR_LEGEND && element instanceof ITmfTreeViewerEntry
                    && isChecked(element)) {
                Optional<ITmfTreeDataModel> model = tryGetModel(element);
                if (model.isPresent()) {
                    return getLegendImage(model.get().getName());
                }
            }
            return null;
        }

    }

}
