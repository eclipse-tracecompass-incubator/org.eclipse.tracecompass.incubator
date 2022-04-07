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

package org.eclipse.tracecompass.incubator.internal.ros2.ui.messageflow;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messageflow.Ros2MessageFlowAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2FlowTargetInfo;
import org.eclipse.tracecompass.incubator.internal.ros2.core.signals.Ros2FlowItemSelectedSignal;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisParamProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Parameter provider for a message flow analysis.
 *
 * @author Christophe Bedard
 */
public class Ros2MessageFlowParameterProvider extends TmfAbstractAnalysisParamProvider {

    private static final @NonNull String NAME = "ROS 2 Message Flow parameter provider"; //$NON-NLS-1$

    private Ros2FlowTargetInfo fInfo = null;

    /**
     * Constructor
     */
    public Ros2MessageFlowParameterProvider() {
        super();
        TmfSignalManager.register(this);
    }

    @Override
    public void dispose() {
        super.dispose();
        TmfSignalManager.deregister(this);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Object getParameter(String name) {
        if (name.equals(Ros2MessageFlowAnalysis.TARGET_INFO_KEY)) {
            return fInfo;
        }
        return null;
    }

    @Override
    public boolean appliesToTrace(ITmfTrace trace) {
        return true;
    }

    /**
     * Handle {@link Ros2FlowItemSelectedSignal}
     *
     * @param signal
     *            the signal
     */
    @TmfSignalHandler
    public void handleFollowedMessage(Ros2FlowItemSelectedSignal signal) {
        setTargetInfo(signal.getInfo());
    }

    private void setTargetInfo(Ros2FlowTargetInfo info) {
        fInfo = info;
        notifyParameterChanged(Ros2MessageFlowAnalysis.TARGET_INFO_KEY);
    }
}
