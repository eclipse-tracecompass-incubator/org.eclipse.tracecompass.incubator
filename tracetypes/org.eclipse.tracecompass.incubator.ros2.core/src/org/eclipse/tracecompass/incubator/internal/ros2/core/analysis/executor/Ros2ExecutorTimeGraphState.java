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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.executor;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.executor.Ros2ExecutorStateInstance;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;

import com.google.common.collect.Multimap;

/**
 * Time graph state for ROS 2 executor state. Wraps a
 * {@link Ros2ExecutorStateInstance}.
 *
 * @author Christophe Bedard
 */
public class Ros2ExecutorTimeGraphState extends TimeGraphState {

    /** Executor state */
    public static final String KEY_STATE = "state"; //$NON-NLS-1$
    /** Corresponding handle, if any */
    public static final String KEY_HANDLE = "handle"; //$NON-NLS-1$

    private final Ros2ExecutorStateInstance fStateInstance;

    /**
     * Constructor
     *
     * @param time
     *            the time
     * @param duration
     *            the duration
     * @param stateInstance
     *            the state instance
     */
    public Ros2ExecutorTimeGraphState(long time, long duration, Ros2ExecutorStateInstance stateInstance) {
        super(time, duration, 0, StringUtils.EMPTY, null);
        fStateInstance = stateInstance;
    }

    @Override
    public synchronized Multimap<@NonNull String, @NonNull Object> getMetadata() {
        Multimap<@NonNull String, @NonNull Object> metadata = super.getMetadata();
        metadata.put(KEY_STATE, fStateInstance.getState());
        metadata.put(KEY_HANDLE, fStateInstance.getHandle());
        return metadata;
    }
}
