/*******************************************************************************
 * Copyright (c) 2019, 2022 École Polytechnique de Montréal and 0thers
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.ui.views.timegraph;

import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.scripting.core.data.provider.ScriptedTimeGraphDataProvider;
import org.eclipse.tracecompass.incubator.internal.scripting.core.data.provider.ScriptingDataProviderManager;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
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

    private @Nullable ITimeGraphDataProvider<@NonNull TimeGraphEntryModel> fProvider = null;

    /**
     * Constructor
     */
    public ScriptedTimeGraphView() {
        super(ID, new BasePresentationProvider(), ScriptedTimeGraphDataProvider.ID);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        String title = ScriptingDataProviderManager.extractProviderName(NonNullUtils.nullToEmptyString(getProviderId()));
        if (title != null) {
            setPartName(title);
        }
    }

    @Override
    protected String getProviderId() {
        String secondaryId = getViewSite().getSecondaryId();
        if (secondaryId != null) {
            return secondaryId.replace(COLON, DataProviderConstants.ID_SEPARATOR);
        }
        return super.getProviderId();
    }

    @Override
    protected @NonNull Iterable<ITmfTrace> getTracesToBuild(@Nullable ITmfTrace trace) {
        return Collections.singleton(trace);
    }

    /**
     * Request of refresh of the view if the data provider has changed.
     */
    public void refreshIfNeeded() {
        ITmfTrace trace = getTrace();
        ITimeGraphDataProvider<@NonNull TimeGraphEntryModel> dataProvider = DataProviderManager
                .getInstance().getOrCreateDataProvider(trace, getProviderId(), ITimeGraphDataProvider.class);
        if (dataProvider != fProvider) {
            rebuild();
        }
    }

    @Override
    protected void buildEntryList(@NonNull ITmfTrace trace, @NonNull ITmfTrace parentTrace, @NonNull IProgressMonitor monitor) {
        ITimeGraphDataProvider<@NonNull TimeGraphEntryModel> dataProvider = DataProviderManager
                .getInstance().getOrCreateDataProvider(trace, getProviderId(), ITimeGraphDataProvider.class);
        fProvider = dataProvider;
        super.buildEntryList(trace, parentTrace, monitor);
    }
}
