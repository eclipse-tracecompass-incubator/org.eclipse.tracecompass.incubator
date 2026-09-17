/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.ui.view;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

/**
 * Placeholder view for the TraceX execution analysis.
 */
public class TraceXView extends TmfView {

    /** View extension identifier. */
    public static final String ID = "org.eclipse.tracecompass.incubator.tracex.ui.view"; //$NON-NLS-1$

    /** Constructor. */
    public TraceXView() {
        super("TraceX Execution"); //$NON-NLS-1$
    }

    @Override
    public void createPartControl(@Nullable Composite parent) {
        super.createPartControl(parent);
        if (parent != null) {
            Label label = new Label(parent, SWT.WRAP);
            label.setText("TraceX execution analysis is under development."); //$NON-NLS-1$
        }
    }

    @Override
    public void setFocus() {
        // The placeholder view has no interactive control yet.
    }
}
