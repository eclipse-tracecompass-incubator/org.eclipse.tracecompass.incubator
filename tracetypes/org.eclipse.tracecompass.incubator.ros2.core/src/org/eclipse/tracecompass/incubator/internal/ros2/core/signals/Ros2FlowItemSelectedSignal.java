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

package org.eclipse.tracecompass.incubator.internal.ros2.core.signals;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2FlowTargetInfo;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;

/**
 * ROS 2 flow item selection signal.
 *
 * @author Christophe Bedard
 */
public class Ros2FlowItemSelectedSignal extends TmfSignal {

    private final @NonNull Ros2FlowTargetInfo fInfo;

    /**
     * Constructor
     *
     * @param source
     *            the source
     * @param info
     *            the target element info
     */
    public Ros2FlowItemSelectedSignal(Object source, @NonNull Ros2FlowTargetInfo info) {
        super(source, 0);
        fInfo = info;
    }

    /**
     * @return the info
     */
    public @NonNull Ros2FlowTargetInfo getInfo() {
        return fInfo;
    }
}
