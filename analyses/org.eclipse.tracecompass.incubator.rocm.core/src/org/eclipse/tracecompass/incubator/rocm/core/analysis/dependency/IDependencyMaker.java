/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.rocm.core.analysis.dependency;

import java.util.Map;

import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * This interface is used to separate the event handlers which do not store
 * states and dependency making which requires to store some information to
 * make the dependencies.
 *
 * @author Arnaud Fiorini
 */
public interface IDependencyMaker {

    /**
     * @param event
     *            The event to process
     * @param ssb
     *            The state system builder to use to add the dependency
     */
    void processEvent(ITmfEvent event, ITmfStateSystemBuilder ssb);

    /**
     * @return A Map which stores the event correlations
     */
    Map<Long, ITmfEvent> getApiEventCorrelationMap();
}
