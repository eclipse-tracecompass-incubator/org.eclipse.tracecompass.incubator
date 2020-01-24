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

package org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.timegraph;

import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphPresentationProvider;

/**
 * Indicates that an implementing class has a TimeGraph presentation provider.
 *
 * @author Ivan Grinenko
 *
 */
public interface IHasTimeGraphPresentationProvider {

    /**
     * @return Presentation provider for TimeGraphs.
     */
    ITimeGraphPresentationProvider getProvider();

}
