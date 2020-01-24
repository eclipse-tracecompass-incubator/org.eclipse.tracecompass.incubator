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

package org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view;

import org.eclipse.tracecompass.tmf.ui.views.ITmfTimeAligned;

/**
 * A lane to be used by the {@link MultiView}.
 *
 * @author Ivan Grinenko
 *
 */
public interface IMultiViewer extends ITmfTimeAligned {

    /**
     * Naming for threads or displaying.
     *
     * @return name of the viewer
     */
    String getName();

    /**
     * Dispose of the component.
     */
    void dispose();

}
