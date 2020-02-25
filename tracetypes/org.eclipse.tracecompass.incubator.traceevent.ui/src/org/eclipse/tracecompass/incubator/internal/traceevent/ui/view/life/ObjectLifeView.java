/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.ui.view.life;

import org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.objectlife.ObjectLifeAnalysis;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.objectlife.ObjectLifeDataProvider;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;

/**
 * Simple gantt chart to see the lifespans of objects
 *
 * @author Matthew Khouzam
 */
public class ObjectLifeView extends BaseDataProviderTimeGraphView {

    private static final String ID = "org.eclipse.tracecompass.incubator.traceevent.ui.view.life.objectlife.view"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public ObjectLifeView() {
        this(ID, new ObjectLifePresentationProvider(), ObjectLifeAnalysis.ID + ObjectLifeDataProvider.SUFFIX);
    }

    /**
     * Extendable constructor
     *
     * @param id
     *            the view ID
     * @param pres
     *            the presentation provider
     * @param dpID
     *            the dataprovider ID
     */
    public ObjectLifeView(String id, TimeGraphPresentationProvider pres, String dpID) {
        super(id, pres, dpID);
    }

}
