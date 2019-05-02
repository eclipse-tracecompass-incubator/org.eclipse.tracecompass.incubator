/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.ui.views.timegraph;

import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.scripting.core.data.provider.ScriptedTimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;

/**
 * A data provider view to display the results of a scripted analysis. It uses
 * the secondary ID as the data provider ID to display
 *
 * @author Geneviève Bastien
 */
public class ScriptedTimeGraphView extends BaseDataProviderTimeGraphView {

    /**
     * Because colons are not allowed in secondary IDs, but can be present in
     * data provider IDs, they can be replaced upstream by this string and it
     * will be replaced again when getting the data provider ID.
     */
    public static final String COLON = "[COLON]"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /** View ID */
    public static final String ID = "org.eclipse.tracecompass.incubator.scripting.ui.view.timegraph"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public ScriptedTimeGraphView() {
        super(ID, new BasePresentationProvider(), ScriptedTimeGraphDataProvider.ID);
    }

    @Override
    protected String getProviderId() {
        String secondaryId = getViewSite().getSecondaryId();
        if (secondaryId != null) {
            return secondaryId.replace(COLON, ":"); //$NON-NLS-1$
        }
        return super.getProviderId();
    }

    @Override
    protected @NonNull Iterable<ITmfTrace> getTracesToBuild(@Nullable ITmfTrace trace) {
        return Collections.singleton(trace);
    }

}
