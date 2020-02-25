/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.nodes.RosNodesStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Instantiator helper for ROS state providers. Can be used by analyses to
 * instantiate their corresponding state provider, or by state providers to
 * create new instances.
 *
 * @author Christophe Bedard
 */
public interface IRosStateProviderInstantiator {

    /**
     * Get new ROS state provider instance
     *
     * @param stateProvider
     *            the state provider class
     * @param trace
     *            the trace to provide to the state provider
     * @return the new state provider instance
     */
    default @NonNull ITmfStateProvider getNewRosStateProviderInstance(Class<?> stateProvider, ITmfTrace trace) {
        try {
            return Objects.requireNonNull((ITmfStateProvider) stateProvider.getConstructor(ITmfTrace.class).newInstance(trace));
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        Activator.getInstance().logError("Error instantiating state provider; using ROS Nodes state provider instead for: " + stateProvider.toString()); //$NON-NLS-1$
        return new RosNodesStateProvider(trace);
    }
}
