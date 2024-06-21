/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class GCTraceLayout {

    private final String fAlloc = "Allocation"; //$NON-NLS-1$
    private final String fGcId = "GC Id"; //$NON-NLS-1$
    private final String fCause = "Cause"; //$NON-NLS-1$
    private final String fCauseInterval = "Cause Interval"; //$NON-NLS-1$
    private final String fPause = "Pause"; //$NON-NLS-1$
    private final String fReclamation = "Reclamation"; //$NON-NLS-1$
    private final String fCpuTime = "Cpu time"; //$NON-NLS-1$
    private final String fDuration = "Duration"; //$NON-NLS-1$
    private final String fLevel = "Level"; //$NON-NLS-1$
    private final String fPromotion = "Promotion"; //$NON-NLS-1$
    private final String fEndTime = "End time"; //$NON-NLS-1$

    public GCTraceLayout() {
        // Do Nothing
    }

    /**
     * @return the alloc
     */
    public String getAlloc() {
        return fAlloc;
    }

    /**
     * @return the gcId
     */
    public String getGcId() {
        return fGcId;
    }

    /**
     * @return the cause
     */
    public String getCause() {
        return fCause;
    }

    /**
     * @return the pause
     */
    public String getPause() {
        return fPause;
    }

    /**
     * @return the causeInterval
     */
    public String getCauseInterval() {
        return fCauseInterval;
    }

    /**
     * @return the reclamation
     */
    public String getReclamation() {
        return fReclamation;
    }

    /**
     * @return the cpuTime
     */
    public String getCpuTime() {
        return fCpuTime;
    }

    /**
     * @return the duration
     */
    public String getDuration() {
        return fDuration;
    }

    /**
     * @return the level
     */
    public String getLevel() {
        return fLevel;
    }

    /**
     * @return the promotion
     */
    public String getPromotion() {
        return fPromotion;
    }

    /**
     * @return the endTime
     */
    public String getEndTime() {
        return fEndTime;
    }

    /**
     * Get Memory Name
     *
     * @param suffix
     *            the suffix, like "cache"
     * @return an encoded string with
     */
    public String getMemName(String suffix) {
        return "mem" + suffix; //$NON-NLS-1$
    }
}
