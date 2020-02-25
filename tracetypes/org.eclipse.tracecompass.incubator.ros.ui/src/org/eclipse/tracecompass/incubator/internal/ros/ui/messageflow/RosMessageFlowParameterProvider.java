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

package org.eclipse.tracecompass.incubator.internal.ros.ui.messageflow;

import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow.RosMessageFlowAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow.TargetMessageInfo;
import org.eclipse.tracecompass.incubator.internal.ros.core.signals.RosMessageSelectedSignal;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisParamProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Parameter provider for the ROS Message Flow analysis. Contains the target
 * message info.
 *
 * @author Christophe Bedard
 */
public class RosMessageFlowParameterProvider extends TmfAbstractAnalysisParamProvider {

    private static final String NAME = "ROS Message Flow parameter provider"; //$NON-NLS-1$

    private TargetMessageInfo fInfo = null;

    /**
     * Constructor
     */
    public RosMessageFlowParameterProvider() {
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
        if (name.equals(RosMessageFlowAnalysis.TARGET_MESSAGE_INFO_KEY)) {
            return fInfo;
        }
        return null;
    }

    @Override
    public boolean appliesToTrace(ITmfTrace trace) {
        return true;
    }

    /**
     * Handle {@link RosMessageSelectedSignal}
     *
     * @param signal
     *            the signal
     */
    @TmfSignalHandler
    public void handleFollowedMessage(RosMessageSelectedSignal signal) {
        setTargetMessageInfo(signal.getInfo());
    }

    private void setTargetMessageInfo(TargetMessageInfo info) {
        fInfo = info;
        notifyParameterChanged(RosMessageFlowAnalysis.TARGET_MESSAGE_INFO_KEY);
    }
}
