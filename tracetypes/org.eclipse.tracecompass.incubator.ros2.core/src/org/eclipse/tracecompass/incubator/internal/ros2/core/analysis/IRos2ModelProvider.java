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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface for a model provider, which is an analysis that takes information
 * from other model(s), existing state system(s), and/or trace event(s), and
 * generates a model, i.e., a database using an abstract representation of the
 * trace data.
 *
 * @author Christophe Bedard
 * @param <M>
 *            the ROS 2 model it provides
 */
public interface IRos2ModelProvider<M extends IRos2Model> {

    /**
     * @return the model, or {@code null} if it hasn't been generated yet
     */
    @Nullable
    M getModel();
}
