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

package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.rocm.core.Activator;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.dependency.HipApiHipActivityDependencyMaker;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old.AbstractGpuEventHandler;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old.ApiEventHandler;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old.HipActivityEventHandler;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old.HsaActivityEventHandler;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old.HsaKernelEventHandler;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old.RoctxEventHandler;
import org.eclipse.tracecompass.incubator.rocm.core.analysis.dependency.IDependencyMaker;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceWithPreDefinedEvents;

import com.google.common.collect.ImmutableMap;

/**
 * This state provider creates callstacks and apply different event handlers.
 * There are multiple types of events, each described in their respective
 * handler.
 *
 * Attribute tree:
 *
 * <pre>
 * |- Processes
 * |  |- <GPU>
 * |  |  |- HIP Streams
 * |  |  |  |- <Stream> -> Each stream implemented as a callstack with corresponding
 * |  |  |  |              compute kernel activity.
 * |  |  |- Queues
 * |  |  |  |- <Queue> -> Each queue implemented as a callstack with corresponding
 * |  |  |  |             compute kernel activity.
 * |  |- Memory
 * |  |  |  |- Memory transfers -> Callstack with every memory transfer activity.
 * |  |- System
 * |  |  |- <Thread>
 * |  |  |  |- <API> -> Each API is a callstack showing which API call is executed.
 * </pre>
 *
 * @author Arnaud Fiorini
 */
public class RocmCallStackStateProvider extends AbstractTmfStateProvider {

    private static final String ID = "org.eclipse.tracecompass.incubator.rocm.core.stateprovider.interval"; //$NON-NLS-1$

    private IDependencyMaker fDependencyMaker;
    private Map<String, AbstractGpuEventHandler> fEventNames;

    /**
     * @param trace
     *            trace to follow
     */
    public RocmCallStackStateProvider(@NonNull ITmfTrace trace) {
        super(trace, ID);
        fDependencyMaker = getDependencyMaker((ITmfTraceWithPreDefinedEvents) trace);
        fEventNames = buildEventNames();
    }

    private Map<String, AbstractGpuEventHandler> buildEventNames() {
        ImmutableMap.Builder<String, AbstractGpuEventHandler> builder = ImmutableMap.builder();

        builder.put(RocmStrings.HIP_API, new ApiEventHandler(this));
        builder.put(RocmStrings.HSA_API, new ApiEventHandler(this));
        builder.put(RocmStrings.HIP_ACTIVITY, new HipActivityEventHandler(this));
        builder.put(RocmStrings.ROCTX, new RoctxEventHandler(this));
        builder.put(RocmStrings.HSA_ACTIVITY, new HsaActivityEventHandler(this));
        if (fDependencyMaker == null) { // Disable HSA Activity in case we have
                                        // hip activity events
            builder.put(RocmStrings.KERNEL_EVENT, new HsaKernelEventHandler(this));
        }

        return builder.build();
    }

    private static IDependencyMaker getDependencyMaker(ITmfTraceWithPreDefinedEvents trace) {
        IDependencyMaker dependencyMaker = null;
        for (ITmfEventType eventType : (trace).getContainedEventTypes()) {
            if (eventType.getName().equals(RocmStrings.HIP_ACTIVITY)) {
                dependencyMaker = new HipApiHipActivityDependencyMaker();
                break;
            }
        }
        return dependencyMaker;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new RocmCallStackStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        String eventName = event.getName();
        ITmfStateSystemBuilder ssb = NonNullUtils.checkNotNull(getStateSystemBuilder());
        try {
            AbstractGpuEventHandler handler = fEventNames.get(eventName);
            if (handler != null) {
                handler.handleEvent(ssb, event);
            }
        } catch (AttributeNotFoundException e) {
            Activator.getInstance().logError("Exception while building the RocmCallStack state system", e); //$NON-NLS-1$
        }
        if (fDependencyMaker != null) {
            fDependencyMaker.processEvent(event, ssb);
        }
    }

    /**
     * Accessor for the current dependency maker.
     *
     * The dependency maker is instantiated in the constructor depending the
     * event types present in the rocm trace.
     *
     * @return dependency maker
     */
    public IDependencyMaker getDependencyMaker() {
        return fDependencyMaker;
    }
}
