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

package org.eclipse.tracecompass.incubator.internal.otf2.ui.views;

import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;

/**
 * Abstract OTF2 view, used for common settings and actions
 *
 * @author Yoann Heitz
 */

public abstract class AbstractOtf2View extends BaseDataProviderTimeGraphView {

    /** The ID prefix of OTF2 views */
    public static final String ID_PREFIX = "org.eclipse.tracecompass.incubator.otf2.ui.views."; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param idSuffix
     *            the view ID suffix
     * @param pres
     *            the presentation provider
     * @param providerId
     *            the data provider ID
     */
    public AbstractOtf2View(String idSuffix, TimeGraphPresentationProvider pres, String providerId) {
        super(ID_PREFIX + idSuffix, pres, providerId);
    }
}