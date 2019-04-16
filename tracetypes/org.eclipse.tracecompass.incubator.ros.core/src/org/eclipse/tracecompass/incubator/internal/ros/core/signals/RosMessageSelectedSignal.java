/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.signals;

import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow.TargetMessageInfo;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;

/**
 * ROS message selection signal
 *
 * @author Christophe Bedard
 */
public class RosMessageSelectedSignal extends TmfSignal {

    private final TargetMessageInfo fInfo;

    /**
     * Constructor
     *
     * @param source
     *            the source
     * @param info
     *            the target message info
     */
    public RosMessageSelectedSignal(Object source, TargetMessageInfo info) {
        super(source, 0);
        fInfo = info;
    }

    /**
     * @return the target message info
     */
    public TargetMessageInfo getInfo() {
        return fInfo;
    }
}
