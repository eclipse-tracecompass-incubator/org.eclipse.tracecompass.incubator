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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;

import com.google.common.collect.Multimap;

/**
 * Time graph state for ROS 2 message publication instance. Wraps a
 * {@link Ros2PubInstance}.
 *
 * @author Christophe Bedard
 */
public class Ros2PubTimeGraphState extends TimeGraphState {

    /** Publication instance */
    public static final String KEY_DATA = "data"; //$NON-NLS-1$

    private final Ros2PubInstance fPubInstance;

    /**
     * Constructor
     *
     * @param time
     *            the time
     * @param duration
     *            the duration
     * @param pubInstance
     *            the underlying publication instance data
     */
    public Ros2PubTimeGraphState(long time, long duration, Ros2PubInstance pubInstance) {
        super(time, duration, 0, StringUtils.EMPTY, null);
        fPubInstance = pubInstance;
    }

    @Override
    public synchronized Multimap<@NonNull String, @NonNull Object> getMetadata() {
        Multimap<@NonNull String, @NonNull Object> metadata = super.getMetadata();
        metadata.put(KEY_DATA, Objects.requireNonNull(fPubInstance));
        return metadata;
    }

    @Override
    public @NonNull String toString() {
        return String.format("Ros2PubTimeGraphState: pubInstance=[%s] + %s", fPubInstance.toString(), super.toString()); //$NON-NLS-1$
    }
}
