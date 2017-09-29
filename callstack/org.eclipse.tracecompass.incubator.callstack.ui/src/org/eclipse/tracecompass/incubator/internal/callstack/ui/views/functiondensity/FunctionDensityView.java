/******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.ui.views.functiondensity;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density.AbstractSegmentStoreDensityView;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density.AbstractSegmentStoreDensityViewer;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.table.AbstractSegmentStoreTableViewer;
import org.eclipse.tracecompass.common.core.NonNullUtils;

/**
 * Call stack Density view displaying the call stack segments tree.
 *
 * @author Sonia Farrah
 */
public class FunctionDensityView extends AbstractSegmentStoreDensityView {

    /** The view's ID */
    public static final @NonNull String ID = FunctionDensityView.class.getPackage().getName() + ".functionDensity"; //$NON-NLS-1$

    /**
     * Constructs a new density view.
     */
    public FunctionDensityView() {
        super(ID);
    }

    @Override
    protected AbstractSegmentStoreTableViewer createSegmentStoreTableViewer(Composite parent) {
        return new FunctionTableViewer(new TableViewer(parent, SWT.FULL_SELECTION | SWT.VIRTUAL), getViewSite().getSecondaryId()) {
            @Override
            protected void createProviderColumns() {
                super.createProviderColumns();
                Table t = (Table) getControl();
                t.setColumnOrder(new int[] { 2, 3, 0, 1 });
            }
        };
    }

    @Override
    protected AbstractSegmentStoreDensityViewer createSegmentStoreDensityViewer(Composite parent) {
        return new FunctionDensityViewer(NonNullUtils.checkNotNull(parent), getViewSite().getSecondaryId());
    }
}