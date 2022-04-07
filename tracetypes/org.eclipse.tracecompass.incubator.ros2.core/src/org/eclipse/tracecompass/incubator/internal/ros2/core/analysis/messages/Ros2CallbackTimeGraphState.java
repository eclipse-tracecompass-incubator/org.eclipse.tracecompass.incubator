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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackInstance;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;

import com.google.common.collect.Multimap;

/**
 * Time graph state for ROS 2 callback instance. Wraps a
 * {@link Ros2CallbackInstance}.
 *
 * @author Christophe Bedard
 */
public class Ros2CallbackTimeGraphState extends TimeGraphState {

    /** Callback instance */
    public static final String KEY_DATA = "data"; //$NON-NLS-1$

    private final Ros2CallbackInstance fCallbackInstance;

    /**
     * Constructor
     *
     * @param callbackInstance
     *            the callback instance
     */
    public Ros2CallbackTimeGraphState(Ros2CallbackInstance callbackInstance) {
        super(callbackInstance.getStartTime(), callbackInstance.getEndTime() - callbackInstance.getStartTime() + 1, 0, StringUtils.EMPTY, null);
        fCallbackInstance = callbackInstance;
    }

    @Override
    public synchronized Multimap<@NonNull String, @NonNull Object> getMetadata() {
        Multimap<@NonNull String, @NonNull Object> metadata = super.getMetadata();
        metadata.put(KEY_DATA, Objects.requireNonNull(fCallbackInstance));
        return metadata;
    }

    @Override
    public @NonNull String toString() {
        return String.format("Ros2CallbackTimeGraphState: callbackInstance=[%s] + %s", fCallbackInstance.toString(), super.toString()); //$NON-NLS-1$
    }
}
