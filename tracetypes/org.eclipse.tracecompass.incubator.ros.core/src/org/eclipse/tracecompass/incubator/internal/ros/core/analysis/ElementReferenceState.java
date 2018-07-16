/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis;

import java.util.Objects;

import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;

/**
 * TimeGraphState for elements with a reference. Useful for elements in a queue.
 *
 * @author Christophe Bedard
 */
public class ElementReferenceState extends TimeGraphState {

    /** Prefix for hex-formatted numbers */
    public static final String HEX_PREFIX = "0x"; //$NON-NLS-1$

    private final long fRef;

    /**
     * Constructor
     *
     * @param time
     *            the timestamp
     * @param duration
     *            the duration
     * @param ref
     *            the element reference
     */
    public ElementReferenceState(long time, long duration, long ref) {
        super(time, duration, 0, Objects.requireNonNull(HEX_PREFIX + Long.toHexString(ref)));
        fRef = ref;
    }

    /**
     * Get the queue element reference
     *
     * @return the queue element reference
     */
    public long getReference() {
        return fRef;
    }
}
