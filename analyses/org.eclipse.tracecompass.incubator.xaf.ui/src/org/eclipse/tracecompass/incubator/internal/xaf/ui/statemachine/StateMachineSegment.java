/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;

import com.google.common.base.Joiner;

/**
 * @author Raphaël Beamonte
 */
public class StateMachineSegment implements ISegment {

    private static final long serialVersionUID = 3173799969329502490L;

    private final long fTid;
    private final long fStartTime;
    private final long fEndTime;
    private final List<String> fInvalidConstraints = new ArrayList<>();

    /**
     * @param tid
     *            The instance tid
     * @param startTime
     *            Start time of the instance
     * @param endTime
     *            End time of the instance
     */
    public StateMachineSegment(
            long tid,
            long startTime,
            long endTime) {
        fTid = tid;
        fStartTime = startTime;
        fEndTime = endTime;
    }

    /**
     * @param tid
     *            The instance tid
     * @param startTime
     *            Start time of the instance
     * @param endTime
     *            End time of the instance
     * @param invalidConstraint
     *            invalid constraint for the instance
     */
    public StateMachineSegment(
            long tid,
            long startTime,
            long endTime,
            String invalidConstraint) {
        fTid = tid;
        fStartTime = startTime;
        fEndTime = endTime;
        addInvalidConstraint(invalidConstraint);
    }

    /**
     * @param tid
     *            The instance tid
     * @param startTime
     *            Start time of the instance
     * @param endTime
     *            End time of the instance
     * @param invalidConstraints
     *            invalid constraints for the instance
     */
    public StateMachineSegment(
            long tid,
            long startTime,
            long endTime,
            List<String> invalidConstraints) {
        fTid = tid;
        fStartTime = startTime;
        fEndTime = endTime;
        addInvalidConstraints(invalidConstraints);
    }

    @Override
    public long getStart() {
        return fStartTime;
    }

    @Override
    public long getEnd() {
        return fEndTime;
    }

    /**
     * Verify if a given segment matches the current one (except for the constraints)
     * @param segment The segment for which to verify
     * @return Whether or not the given segment matches the current one
     */
    public boolean matches(ISegment segment) {
        if (!(segment instanceof StateMachineSegment)
                || segment.getStart() != getStart()
                || segment.getEnd() != getEnd()) {
            return false;
        }

        StateMachineSegment sms = (StateMachineSegment)segment;
        if (sms.getTid() != getTid()
                || sms.getStatus() != getStatus()) {
            return false;
        }

        return true;
    }

    /**
     * Add an invalid constraint to the local list
     * @param invalidConstraint The constraint to add
     */
    public void addInvalidConstraint(String invalidConstraint) {
        fInvalidConstraints.add(invalidConstraint);
    }

    /**
     * Add a list of invalid constraints to the local list
     * @param invalidConstraints The list of constraints to add
     */
    public void addInvalidConstraints(List<String> invalidConstraints) {
        fInvalidConstraints.addAll(invalidConstraints);
    }

    /**
     * Get the list of constraints of that segment as a comma separated list
     * @return A string representing the list of constraints
     */
    public String getInvalidConstraints() {
        return Joiner.on(", ").join(fInvalidConstraints); //$NON-NLS-1$
    }

    /**
     * Get the list of constraints of that segment
     * @return The list of constraints
     */
    public List<String> getInvalidConstraintsList() {
        return fInvalidConstraints;
    }

    /**
     * Get the status of that segment (VALID or INVALID) depending on the
     * content of the list of constraints.
     * @return VALID if there is no invalid constraint on that segment, INVALID else
     */
    public String getStatus() {
        return fInvalidConstraints.isEmpty() ? "VALID" : "INVALID"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Get the TID of the instance on which that segment applies
     * @return The TID
     */
    public long getTid() {
        return fTid;
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
                "; Tid = " + getTid() + //$NON-NLS-1$
                "; Status = " + getStatus() + //$NON-NLS-1$
                ((fInvalidConstraints.isEmpty()) ? "" : "; Invalid Constraints = " + getInvalidConstraints()); //$NON-NLS-1$ //$NON-NLS-2$
    }



    /**
     * Aspect for the representation of the TID of the state machine instance
     *
     * @author Raphaël Beamonte
     */
    public static class TidAspect implements ISegmentAspect {
        /**
         * Instance of that aspect
         */
        public static final @NonNull ISegmentAspect INSTANCE = new TidAspect();

        private TidAspect() { }

        @Override
        public String getHelpText() {
            return "Instance TID"; //$NON-NLS-1$
        }
        @Override
        public String getName() {
            return "Instance TID"; //$NON-NLS-1$
        }
        @Override
        public @Nullable Comparator<?> getComparator() {
            return null;
        }
        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof StateMachineSegment) {
                return Long.toString(((StateMachineSegment) segment).getTid());
            }
            return EMPTY_STRING;
        }
    }

    /**
     * Aspect for the representation of the Status of the state machine instance
     *
     * @author Raphaël Beamonte
     */
    public static class StatusAspect implements ISegmentAspect {
        /**
         * Instance of that aspect
         */
        public static final @NonNull ISegmentAspect INSTANCE = new StatusAspect();

        private StatusAspect() { }

        @Override
        public String getHelpText() {
            return "Instance status"; //$NON-NLS-1$
        }
        @Override
        public String getName() {
            return "Instance status"; //$NON-NLS-1$
        }
        @Override
        public @Nullable Comparator<?> getComparator() {
            return null;
        }
        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof StateMachineSegment) {
                return ((StateMachineSegment) segment).getStatus();
            }
            return EMPTY_STRING;
        }
    }

    /**
     * Aspect for the representation of the invalid constraints of the state machine instance
     *
     * @author Raphaël Beamonte
     */
    public static class InvalidConstraintsAspect implements ISegmentAspect {
        /**
         * Instance of that aspect
         */
        public static final @NonNull ISegmentAspect INSTANCE = new InvalidConstraintsAspect();

        private InvalidConstraintsAspect() { }

        @Override
        public String getHelpText() {
            return "Invalid constraints"; //$NON-NLS-1$
        }
        @Override
        public String getName() {
            return "Invalid constraints"; //$NON-NLS-1$
        }
        @Override
        public @Nullable Comparator<?> getComparator() {
            return null;
        }
        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof StateMachineSegment) {
                return ((StateMachineSegment) segment).getInvalidConstraints();
            }
            return EMPTY_STRING;
        }
    }
}
