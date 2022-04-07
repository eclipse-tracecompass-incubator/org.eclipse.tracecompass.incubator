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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2TakeInstance;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;

import com.google.common.collect.Multimap;

/**
 * Time graph state for ROS 2 message taking instance. Wraps a
 * {@link Ros2TakeInstance}.
 *
 * @author Christophe Bedard
 */
public class Ros2TakeTimeGraphState extends TimeGraphState {

    /** Take instance */
    public static final String KEY_DATA = "data"; //$NON-NLS-1$

    private final Ros2TakeInstance fTakeInstance;

    /**
     * Constructor
     *
     * @param takeInstance
     *            the take instance
     */
    public Ros2TakeTimeGraphState(Ros2TakeInstance takeInstance) {
        super(takeInstance.getStartTime(), takeInstance.getEndTime() - takeInstance.getStartTime() + 1, 0, StringUtils.EMPTY, null);
        fTakeInstance = takeInstance;
    }

    @Override
    public synchronized Multimap<@NonNull String, @NonNull Object> getMetadata() {
        Multimap<@NonNull String, @NonNull Object> metadata = super.getMetadata();
        metadata.put(KEY_DATA, Objects.requireNonNull(fTakeInstance));
        return metadata;
    }

    @Override
    public @NonNull String toString() {
        return String.format("Ros2TakeTimeGraphState: takeInstance=[%s] + %s", fTakeInstance.toString(), super.toString()); //$NON-NLS-1$
    }
}
