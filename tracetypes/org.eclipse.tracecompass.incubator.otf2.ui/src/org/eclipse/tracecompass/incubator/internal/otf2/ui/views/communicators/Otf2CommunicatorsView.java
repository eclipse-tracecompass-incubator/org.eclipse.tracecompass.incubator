/**********************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.ui.views.communicators;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.communicators.Otf2CommunicatorsDataProvider;
import org.eclipse.tracecompass.incubator.internal.otf2.ui.views.AbstractOtf2View;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;

/**
 * View for OTF2 communicators
 *
 * @author Yoann Heitz
 */

public class Otf2CommunicatorsView extends AbstractOtf2View {

    /** View ID suffix */
    public static final String ID_SUFFIX = "communicators"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public Otf2CommunicatorsView() {
        super(ID_SUFFIX, new BaseDataProviderTimeGraphPresentationProvider(), Otf2CommunicatorsDataProvider.getFullDataProviderId());
        setFilterColumns(new String[] { StringUtils.EMPTY });
        setFilterLabelProvider(new Otf2CommunicatorsViewFilterLabelProvider());
    }

    private static class Otf2CommunicatorsViewFilterLabelProvider extends TreeLabelProvider {
    }

    /**
     * @return the full view ID for this view
     */
    public static String getFullViewId() {
        return AbstractOtf2View.ID_PREFIX + ID_SUFFIX;
    }
}