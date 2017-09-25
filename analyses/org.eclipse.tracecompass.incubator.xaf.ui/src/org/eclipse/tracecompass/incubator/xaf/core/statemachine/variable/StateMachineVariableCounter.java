/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineReport;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance.InstanceStepInformation;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.StateMachineBackendAnalysis;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.Operator;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.StateMachineConstraint;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.ValueType;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineNoInvalidCaseException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineNoValidCaseException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineStateSystemNotFoundException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineStateSystemTimeRangeException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineTIDNotFoundException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineUnexpectedEventException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineVariableException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariableAnalysis.DefaultInterruptionEventRunner;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariableAnalysis.InterruptionEventGetter;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariableAnalysis.InterruptionEventRunner;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariableAnalysis.ResponsibilityMapGetter;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionEvent;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionEventList;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionIntervalSetDiff;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionReason;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.ResponsibilityMap;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

/**
 * Class that represents a state machine variable of counter class
 * @author Raphaël Beamonte
 */
public abstract class StateMachineVariableCounter extends StateMachineVariable {

    /**
     * @param name The name of the variable
     * @param value The initial value of the variable
     */
    @SuppressWarnings("rawtypes")
    public StateMachineVariableCounter(String name, Comparable value) {
        super(name, value);
    }

    /**
     * @return The attribute of this variable in the backend
     */
    public abstract String getCounterAttribute();

    @Override
    public Comparable getValue(ITmfEvent event, StateMachineInstance stateMachineInstance) throws StateMachineVariableException {
        if (stateMachineInstance.getStateMachineInstanceGroup().getStateMachineBackendAnalysisModules().isEmpty()) {
            throw new StateMachineVariableException("State system unavailable"); //$NON-NLS-1$
        }

        ITmfTimestamp intervalStart = (ITmfTimestamp) getValue();
        ITmfTimestamp intervalEnd = event.getTimestamp();

        int tid = stateMachineInstance.getVTid();
        Integer counter = 0;
        for (StateMachineBackendAnalysis kernelAnalysisModule : stateMachineInstance.getStateMachineInstanceGroup().getStateMachineBackendAnalysisModules()) {
            Integer lcounter = kernelAnalysisModule.getCounterIntvl(tid, intervalStart.getValue(), intervalEnd.getValue(), getCounterAttribute());
            if (lcounter != null) {
                counter += lcounter;
            }
        }

        return counter;
    }

    @Override
    public Integer compareTo(Comparable toCompare, ITmfEvent event, StateMachineInstance stateMachineInstance) throws StateMachineVariableException {
        Integer counter = (Integer)getValue(event, stateMachineInstance);
        Integer valueComparaison = Integer.parseInt((String)toCompare);
        Integer comparison = counter.compareTo(valueComparaison);
        return comparison;
    }

    @Override
    public Double getNumericalValue(Comparable format, ITmfEvent event, StateMachineInstance stateMachineInstance) {
        try {
            return ((Integer)getValue(event, stateMachineInstance)).doubleValue();
        } catch (StateMachineVariableException e) {
            return null;
        }
    }

    @Override
    public String getFormattedValue(Comparable format, ITmfEvent event, StateMachineInstance stateMachineInstance) {
        Double delta;
        try {
            delta = ((Integer)getValue(event, stateMachineInstance)).doubleValue();
        } catch (StateMachineVariableException e) {
            delta = null;
        }

        return getFormattedValue(format, delta);
    }

    @Override
    public String getFormattedValue(Comparable format, Double value) {
        if (value == null) {
            return "unavailable"; //$NON-NLS-1$
        }
        return String.valueOf(value);
    }

    private class DefaultCounterInterruptionEventGetter extends InterruptionEventGetter {

        @Override
        public InterruptionEventList execute(TimestampInterval ti, ITmfStateInterval si, ITmfEvent e) throws StateMachineStateSystemNotFoundException, StateMachineTIDNotFoundException, StateMachineStateSystemTimeRangeException, StateMachineUnexpectedEventException {
            InterruptionEventList taskList = new InterruptionEventList();
            if (ti.getStartTime().getValue() > si.getStartTime()) {
                return taskList;
            }

            // Get the start and end of the state, given the start and end of the interval
            long startTime = Math.max(si.getStartTime(), ti.getStartTime().getValue());
            long endTime = startTime + 1;

            InterruptionReason ir = getInterruptionReasonFromEvent(e);

            // Add an InterruptionEvent to the taskList
            taskList.events.add(new InterruptionEvent(e,
                    TmfTimestamp.create(startTime, ITmfTimestamp.NANOSECOND_SCALE),
                    TmfTimestamp.create(endTime, ITmfTimestamp.NANOSECOND_SCALE),
                    ir));

            return taskList;
        }

    }

    /**
     * @return The interruption event getter for that variable
     */
    public InterruptionEventGetter getInterruptionEventGetter() {
        return new DefaultCounterInterruptionEventGetter();
    }

    /**
     * @param e The event from which to extract the interruption reason
     * @return The interruption reason computed from the event
     * @throws StateMachineUnexpectedEventException If the kind of event received is abnormal
     */
    protected abstract InterruptionReason getInterruptionReasonFromEvent(ITmfEvent e) throws StateMachineUnexpectedEventException;

    /**
     * @return The interruption event runner for that variable
     */
    public InterruptionEventRunner getInterruptionEventRunner() {
        return new DefaultInterruptionEventRunner(
                this,
                NonNullUtils.checkNotNull(getCounterAttribute()),
                NonNullUtils.checkNotNull(getInterruptionEventGetter()),
                new ResponsibilityMapGetter<InterruptionReason>() {
                    @Override
                    public ResponsibilityMap<InterruptionReason> execute(InterruptionIntervalSetDiff iisd) {
                        return iisd.getReasonCounterResponsibilityMap();
                    }
                });
    }

    @Override
    public StateMachineVariableAnalysis analyze(StateMachineConstraint constraint, List<InstanceStepInformation> invalidIsiList, List<InstanceStepInformation> validIsiList) {
        // Treat only constants for now
        // TODO: treat comparison between variables
        if (constraint.getValueType() != ValueType.CONSTANT) {
            System.out.println("TODO"); //$NON-NLS-1$
            return null;
        }

        // value of X
        int X = Integer.parseInt(constraint.getValue());

        boolean partial = (
                (constraint.getOperator() == Operator.LT && X < 2)
                || (constraint.getOperator() == Operator.EQ && X == 0));

        StateMachineVariableAnalysis smva = new StateMachineVariableAnalysis(
                this,
                constraint,
                invalidIsiList,
                validIsiList);

        InterruptionEventRunner interruptionEventRunner = getInterruptionEventRunner();


        StateMachineReport.R.println();
        StateMachineReport.R.println_subsubsubsection(String.format("%s analysis: (%s)", //$NON-NLS-1$
                getVariableType(),
                (partial) ? "partial" : "full")); //$NON-NLS-1$ //$NON-NLS-2$

        if (partial) {
            try {
                smva.doPartialAnalysis(interruptionEventRunner);
            } catch (StateMachineNoInvalidCaseException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            if (validIsiList == null || validIsiList.isEmpty()) {
                System.out.print("No valid instance for a full analysis"); //$NON-NLS-1$
                return null;
            }

            try {
                smva.doAnalysis(interruptionEventRunner);
            } catch (StateMachineNoValidCaseException | StateMachineNoInvalidCaseException e) {
                e.printStackTrace();
                return null;
            }
        }
        StateMachineReport.R.inc();

        StateMachineReport.R.printDropped(smva);

        if (!partial) {
            StateMachineReport.R.println(smva.getDifferentValidCases() +
                    " different valid situation" + (smva.getDifferentValidCases() > 1 ? "s" : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    " for " + validIsiList.size() + //$NON-NLS-1$
                    " different valid case" + (validIsiList.size() > 1 ? "s" : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    "." //$NON-NLS-1$
                    );
        }
        StateMachineReport.R.println(smva.getDifferentInvalidCases() +
                " different invalid situation" + (smva.getDifferentInvalidCases() > 1 ? "s" : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                " for " + invalidIsiList.size() + //$NON-NLS-1$
                " different invalid case" + (invalidIsiList.size() > 1 ? "s" : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "." //$NON-NLS-1$
                );
        StateMachineReport.R.println();

        ResponsibilityMap<InterruptionReason> analysisResponsibilityMap = smva.getAnalysisResponsibilityMap();

        if (!partial) {
            StateMachineReport.R.println("General computed difference between invalid and valid instances:"); //$NON-NLS-1$
        }
        String[] header = {"Responsible", "Responsibility"}; //$NON-NLS-1$ //$NON-NLS-2$
        String[][] strs = new String[analysisResponsibilityMap.getCollection().size()][2];
        int idx = 0;
        int progressSize = 20;
        for (Entry<InterruptionReason, Double> entry : analysisResponsibilityMap.getCollection()) {
            double ratioValue = entry.getValue() * 100;
            String symbol = StateMachineReport.progressBar(ratioValue, progressSize);

            strs[idx][0] = entry.getKey().getTaskId();
            strs[idx][1] = String.format("%.2f%% %s", ratioValue, symbol); //$NON-NLS-1$
            idx++;
        }
        StateMachineReport.R.println_table(header, strs, Arrays.asList(new Integer[]{1}));
        StateMachineReport.R.println();

        StateMachineReport.R.dec();

        return smva;
    }

    @Override
    public String getVariableType() {
        return this.getClass()
                .getSimpleName()
                .substring(StateMachineVariableCounter.class.getSimpleName().length());
    }

    @Override
    public String toString() {
        return String.format("(%s)%s", //$NON-NLS-1$
                getVariableType(),
                super.toString());
    }

}
