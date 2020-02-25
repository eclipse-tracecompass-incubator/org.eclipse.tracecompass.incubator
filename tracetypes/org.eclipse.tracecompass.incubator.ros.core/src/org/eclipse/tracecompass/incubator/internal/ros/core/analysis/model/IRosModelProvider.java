/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface for a model provider, which is an analysis that takes information
 * from other model(s), existing state system(s), and/or trace event(s), and
 * generates a model
 *
 * @param <M>
 *            the ROS model it provides
 * @author Christophe Bedard
 */
public interface IRosModelProvider<M extends IRosModel> {

    /**
     * @return the model, or {@code null} if it hasn't been generated yet
     */
    @Nullable
    M getModel();
}
