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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2NodeObject;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;

import com.google.common.collect.Multimap;

/**
 * Time graph state for ROS 2 node. Wraps a {@link Ros2NodeObject}.
 *
 * @author Christophe Bedard
 */
public class Ros2NodeTimeGraphState extends TimeGraphState {

    /** Node object */
    public static final String KEY_DATA = "data"; //$NON-NLS-1$

    private final Ros2NodeObject fNodeObject;

    /**
     * Constructor
     *
     * @param time
     *            the time
     * @param duration
     *            the duration
     * @param nodeObject
     *            the underlying node object
     */
    public Ros2NodeTimeGraphState(long time, long duration, Ros2NodeObject nodeObject) {
        super(time, duration, 0, StringUtils.EMPTY, null);
        fNodeObject = nodeObject;
    }

    @Override
    public synchronized Multimap<@NonNull String, @NonNull Object> getMetadata() {
        Multimap<@NonNull String, @NonNull Object> metadata = super.getMetadata();
        metadata.put(KEY_DATA, Objects.requireNonNull(fNodeObject));
        return metadata;
    }

    @Override
    public @NonNull String toString() {
        return String.format("Ros2NodeTimeGraphState: nodeObject=[%s] + %s", fNodeObject.toString(), super.toString()); //$NON-NLS-1$
    }
}
