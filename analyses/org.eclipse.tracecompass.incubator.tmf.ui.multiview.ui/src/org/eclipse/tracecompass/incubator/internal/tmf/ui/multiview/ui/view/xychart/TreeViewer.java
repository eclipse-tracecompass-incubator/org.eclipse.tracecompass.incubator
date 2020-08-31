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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfGenericTreeEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;

/**
 * The tree viewer part (left side) of the view.
 *
 * @author Ivan Grinenko
 */
public class TreeViewer extends AbstractSelectTreeViewer2 {

    private static final int COL_INDEX_COLOR_LEGEND = 2;

    /**
     * @param parent
     *            the parent component
     * @param id
     *            the Provider ID
     */
    public TreeViewer(Composite parent, String id) {
        super(parent, TreeViewer.COL_INDEX_COLOR_LEGEND, id);
        setLabelProvider(new TreeXyLabelProvider());
    }

    @Override
    protected ITmfTreeColumnDataProvider getColumnDataProvider() {
        TmfTreeColumnData legendColumn = new TmfTreeColumnData("Legend");
        legendColumn.setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object o1, Object o2) {
                if (isChecked(o1) && !isChecked(o2)) {
                    return -1;
                }
                if (!isChecked(o1) && isChecked(o2)) {
                    return 1;
                }
                if (o1 instanceof ITmfTreeViewerEntry & o2 instanceof ITmfTreeViewerEntry) {
                    ITmfTreeViewerEntry e1 = (ITmfTreeViewerEntry) o1;
                    ITmfTreeViewerEntry e2 = (ITmfTreeViewerEntry) o2;
                    return e1.getName().compareTo(e2.getName());
                }
                return 0;
            }
        });
        return () -> Arrays.asList(createColumn("Name", Comparator.comparing(ITmfTreeViewerEntry::getName)),
                new TmfTreeColumnData("Unit"), legendColumn);
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
                    ITmfTreeDataModel treeModel = model.get();
                    if (treeModel.hasRowModel()) {
                        return getLegendImage(treeModel.getId());
                    }
                }
            }
            return null;
        }

    }

}
