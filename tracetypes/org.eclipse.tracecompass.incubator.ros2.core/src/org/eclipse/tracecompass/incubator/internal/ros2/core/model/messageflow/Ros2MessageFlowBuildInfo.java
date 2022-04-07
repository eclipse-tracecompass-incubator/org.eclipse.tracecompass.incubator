/**********************************************************************
 * Copyright (c) 2023 Apex.AI, Inc.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messagelinks.Ros2MessageCausalLinksModel;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;

/**
 * Bundle of information needed to build the message flow.
 *
 * @author Christophe Bedard
 */
public class Ros2MessageFlowBuildInfo {

    private final @NonNull ITmfStateSystem fObjectsSs;
    private final @NonNull ITmfStateSystem fMessagesSs;
    private final @NonNull Ros2MessageCausalLinksModel fMessageLinksModel;

    /**
     * Constructor
     *
     * @param objectsSs
     *            the objects state system
     * @param messagesSs
     *            the messages state system
     * @param messageLinksModel
     *            the message links model
     */
    public Ros2MessageFlowBuildInfo(@NonNull ITmfStateSystem objectsSs, @NonNull ITmfStateSystem messagesSs, @NonNull Ros2MessageCausalLinksModel messageLinksModel) {
        fObjectsSs = objectsSs;
        fMessagesSs = messagesSs;
        fMessageLinksModel = messageLinksModel;
    }

    /**
     * @return the objects state system, which provides information on various
     *         pub/sub/timer/node objects
     */
    public @NonNull ITmfStateSystem getObjectsSs() {
        return fObjectsSs;
    }

    /**
     * @return the messages state system, which provides information on various
     *         pub/sub callback/timer callback instances, as well as message
     *         transport and publication instances
     */
    public @NonNull ITmfStateSystem getMessagesSs() {
        return fMessagesSs;
    }

    /**
     * @return the message links model, which provides information on causal
     *         links
     */
    public @NonNull Ros2MessageCausalLinksModel getMessageLinksModel() {
        return fMessageLinksModel;
    }
}
