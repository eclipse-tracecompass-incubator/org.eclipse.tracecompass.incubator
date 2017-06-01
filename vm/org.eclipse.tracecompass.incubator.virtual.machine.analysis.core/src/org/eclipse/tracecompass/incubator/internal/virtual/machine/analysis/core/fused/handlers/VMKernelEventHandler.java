/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.handlers;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Base class for the virtual machine event handlers
 *
 * @author Cédric Biancheri
 */
public abstract class VMKernelEventHandler {

    private final IKernelAnalysisEventLayout fLayout;
    private FusedVirtualMachineStateProvider fStateProvider;

    /**
     * Constructor
     *
     * @param layout
     *            The event layout that corresponds to the trace being analysed
     *            by this handler
     * @param sp
     *            The state provider
     */
    public VMKernelEventHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
        fLayout = layout;
        fStateProvider = sp;
    }

    /**
     * Get the Fused state provider
     *
     * @return The state provider
     */
    public FusedVirtualMachineStateProvider getStateProvider() {
        return fStateProvider;
    }

    /**
     * Get the analysis layout
     *
     * @return the analysis layout
     */
    protected IKernelAnalysisEventLayout getLayout() {
        return fLayout;
    }

    /**
     * Handle a specific kernel event.
     *
     * @param ss
     *            the state system to write to
     * @param event
     *            the event
     */
    public abstract void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event);

}
