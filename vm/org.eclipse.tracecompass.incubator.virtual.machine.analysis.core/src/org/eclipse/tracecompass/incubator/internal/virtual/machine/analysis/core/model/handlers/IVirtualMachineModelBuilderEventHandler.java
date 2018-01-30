/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.handlers;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.IVirtualMachineEventHandler;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualEnvironmentModel;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.analysis.VirtualEnvironmentBuilder;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * The interface that event handler for virtual machine model should implement
 *
 * @author Geneviève Bastien
 */
public interface IVirtualMachineModelBuilderEventHandler extends IVirtualMachineEventHandler {

    /**
     * @param ss
     * @param event
     * @param virtEnv
     * @param eventLayout
     */
    @Override
    default void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event, IVirtualEnvironmentModel virtEnv, IKernelAnalysisEventLayout eventLayout) {
        if (!(virtEnv instanceof VirtualEnvironmentBuilder)) {
            throw new IllegalStateException("The environment model should be a builder"); //$NON-NLS-1$
        }
        handleBuilderEvent(ss, event, (VirtualEnvironmentBuilder) virtEnv, eventLayout);
    }

    /**
     * @param ss
     * @param event
     * @param virtEnv
     * @param eventLayout
     */
    void handleBuilderEvent(ITmfStateSystemBuilder ss, ITmfEvent event, VirtualEnvironmentBuilder virtEnv, IKernelAnalysisEventLayout eventLayout);

}
