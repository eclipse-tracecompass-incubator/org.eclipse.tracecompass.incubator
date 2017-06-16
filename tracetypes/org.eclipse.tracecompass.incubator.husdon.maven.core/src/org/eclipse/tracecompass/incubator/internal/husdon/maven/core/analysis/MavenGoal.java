/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.husdon.maven.core.analysis;

import java.io.Serializable;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * A maven goal running
 *
 * @author Marc-Andre Laperle
 */
public class MavenGoal implements ISegment {

    private static final long serialVersionUID = 1554494342105208730L;

    /**
     * The subset of information that is available from the syscall entry event.
     */
    public static class InitialInfo implements Serializable {

        private static final long serialVersionUID = 1444232969168657886L;
        private final long fStartTime;
        private final String fName;

        /**
         * @param startTime
         *            Start time of the system call
         * @param name
         *            Name of the system call
         * @param startRank
         *            rank of the event at the start time
         */
        public InitialInfo(
                long startTime,
                String name, long startRank) {
            fStartTime = startTime;
            fName = name;
        }
    }

    private final InitialInfo fInfo;
    private final long fEndTime;

    /**
     * @param info
     *            Initial information of the system call
     * @param endTime
     *            End time of the system call
     * @param endRank
     *            rank of the event at the end
     */
    public MavenGoal(
            InitialInfo info,
            long endTime, long endRank) {
        fInfo = info;
        fEndTime = endTime;

    }

    @Override
    public long getStart() {
        return fInfo.fStartTime;
    }

    @Override
    public long getEnd() {
        return fEndTime;
    }

    /**
     * Get the name of the system call
     *
     * @return Name
     */
    public String getName() {
        return fInfo.fName;
    }

    @Override
    public int compareTo(@NonNull ISegment o) {
        int ret = ISegment.super.compareTo(o);
        if (ret != 0) {
            return ret;
        }
        return toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        return "Start Time = " + getStart() + //$NON-NLS-1$
                "; End Time = " + getEnd() + //$NON-NLS-1$
                "; Duration = " + getLength() + //$NON-NLS-1$
                "; Name = " + getName(); //$NON-NLS-1$
    }
}
