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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messagelinks;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.AbstractRos2StateProvider;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messagelinks.Ros2MessageCausalLinkType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messagelinks.Ros2MessageCausalLinksModel;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for the ROS 2 message causal links analysis.
 *
 * @author Christophe Bedard
 */
public class Ros2MessageCausalLinksStateProvider extends AbstractRos2StateProvider {

    private static final int VERSION_NUMBER = 0;

    private final ITmfStateSystem fObjectsSs;
    private final Ros2MessageCausalLinksModel fModel;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     * @param objectsSs
     *            the objects state system
     * @param model
     *            the message links model
     */
    public Ros2MessageCausalLinksStateProvider(ITmfTrace trace, ITmfStateSystem objectsSs, Ros2MessageCausalLinksModel model) {
        super(trace, Ros2MessageCausalLinksAnalysis.getFullAnalysisId());
        fObjectsSs = objectsSs;
        fModel = model;
    }

    @Override
    public int getVersion() {
        return VERSION_NUMBER;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new Ros2MessageCausalLinksStateProvider(getTrace(), fObjectsSs, fModel);
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        if (!considerEvent(event)) {
            return;
        }

        /**
         * No need to actually fill in the state system or use the event
         * timestamps.
         */

        // Periodic async
        if (isEvent(event, LAYOUT.eventMessageLinkPeriodicAsync())) {
            long[] subs = (long[]) getField(event, LAYOUT.fieldSubs());
            long[] pubs = (long[]) getField(event, LAYOUT.fieldPubs());

            fModel.addLink(toHandles(event, subs), toHandles(event, pubs), Ros2MessageCausalLinkType.PERIODIC_ASYNC);
        }
        // Partial sync
        else if (isEvent(event, LAYOUT.eventMessageLinkPartialSync())) {
            long[] subs = (long[]) getField(event, LAYOUT.fieldSubs());
            long[] pubs = (long[]) getField(event, LAYOUT.fieldPubs());

            fModel.addLink(toHandles(event, subs), toHandles(event, pubs), Ros2MessageCausalLinkType.PARTIAL_SYNC);
        }
    }

    private static Collection<@NonNull Ros2ObjectHandle> toHandles(@NonNull ITmfEvent event, long[] handles) {
        return LongStream.of(handles).boxed().map(h -> handleFrom(event, Objects.requireNonNull(h))).collect(Collectors.toUnmodifiableList());
    }
}
