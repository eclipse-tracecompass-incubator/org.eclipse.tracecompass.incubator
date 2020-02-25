/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.xaf.ui.views;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.table.AbstractSegmentStoreTableView;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.table.AbstractSegmentStoreTableViewer;

/**
 * @author Raphaël Beamonte
 */
public class StateMachineInstancesConstraintsView extends AbstractSegmentStoreTableView {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /** The view's ID */
    public static final String ID = "org.eclipse.tracecompass.xaf.core.ui.views.stateMachineInstancesConstraints"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    @Override
    protected AbstractSegmentStoreTableViewer createSegmentStoreViewer(TableViewer tableViewer) {
        return new StateMachineInstancesConstraintsTableViewer(tableViewer);
    }
}
