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

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineReport;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance.InstanceStepInformation;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.Attributes;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.BackendStateValue;
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
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionEvent;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionEventList;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionReason;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.ResponsibilityMap;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.State;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

/**
 * Variable representing a deadline
 *
 * @author Raphaël Beamonte
 */
public class StateMachineVariableDeadline extends StateMachineVariable {

    /*
     * static { StateMachineVariable.VARIABLE_TYPES.put("deadline",
     * StateMachineVariableDeadline.class); //$NON-NLS-1$
     *
     * System.out.println("DEADLINE Map contains keys:"); for (String k :
     * StateMachineVariable.VARIABLE_TYPES.keySet()) { System.out.println(k); }
     * }
     */

    /**
     * @param name
     *            the name
     * @param value
     *            the value
     */
    @SuppressWarnings("rawtypes")
    public StateMachineVariableDeadline(String name, Comparable value) {
        super(name, value);
    }

    @Override
    public Comparable getInitValue(ITmfEvent event) {
        return event.getTimestamp();
    }

    @Override
    public Comparable getValue(ITmfEvent event, StateMachineInstance stateMachineInstance) {
        return event.getTimestamp().getDelta((ITmfTimestamp) getValue());
    }

    @Override
    public Integer compareTo(Comparable toCompare, ITmfEvent event, StateMachineInstance stateMachineInstance) throws StateMachineVariableException {
        ITmfTimestamp delta = (ITmfTimestamp) getValue(event, stateMachineInstance);
        ITmfTimestamp t = StateMachineUtils.strToTimestamp((String) toCompare);

        Integer comparison = delta.compareTo(t);
        return comparison;
    }

    @Override
    public Double getNumericalValue(Comparable format, ITmfEvent event, StateMachineInstance stateMachineInstance) {
        return ((Long) ((ITmfTimestamp) getValue(event, stateMachineInstance)).getValue()).doubleValue();
    }

    @Override
    public String getFormattedValue(Comparable format, ITmfEvent event, StateMachineInstance stateMachineInstance) {
        ITmfTimestamp delta = (ITmfTimestamp) getValue(event, stateMachineInstance);
        ITmfTimestamp t = StateMachineUtils.strToTimestamp((String) format);

        // delta = delta.normalize(0, t.getScale());
        Double value = delta.getValue() * Math.pow(10, delta.getScale() - t.getScale());

        return getFormattedValue(format, value);
    }

    @Override
    public String getFormattedValue(Comparable format, Double value) {
        String symbol = ""; //$NON-NLS-1$
        ITmfTimestamp t = StateMachineUtils.strToTimestamp((String) format);

        switch (t.getScale()) {
        case ITmfTimestamp.SECOND_SCALE:
            symbol = "s"; //$NON-NLS-1$
            break;
        case ITmfTimestamp.MILLISECOND_SCALE:
            symbol = "ms"; //$NON-NLS-1$
            break;
        case ITmfTimestamp.MICROSECOND_SCALE:
            symbol = "us"; //$NON-NLS-1$
            break;
        case ITmfTimestamp.NANOSECOND_SCALE:
            symbol = "ns"; //$NON-NLS-1$
            break;
        default:
            break;

        }

        return String.format("%.4f%s", //$NON-NLS-1$
                value,
                symbol);
    }

    @Override
    public String toString() {
        StringBuilder path = new StringBuilder();
        path.append("(Deadline)"); //$NON-NLS-1$
        path.append(super.toString());
        return path.toString();
    }

    /**
     * @return a copy of the current variable
     */
    @Override
    public StateMachineVariableDeadline getCopy() {
        return new StateMachineVariableDeadline(getName(), getValue());
    }

    @Override
    public StateMachineVariableAnalysis analyze(StateMachineConstraint constraint, List<InstanceStepInformation> invalidIsiList, List<InstanceStepInformation> validIsiList) {
        // Treat only constants for now
        // TODO: treat comparison between variables
        if (constraint.getValueType() != ValueType.CONSTANT) {
            System.out.println("TODO"); //$NON-NLS-1$
            return null;
        }
        if (validIsiList == null || validIsiList.isEmpty()) {
            System.out.print("No valid instance"); //$NON-NLS-1$
            return null;
        }

        StateMachineVariableAnalysis smva = new StateMachineVariableAnalysis(
                this,
                constraint,
                invalidIsiList,
                validIsiList);

        StateMachineReport.R.println();
        smva.runInstancesDurationAnalysis();

        smva.runCpuFrequencyScalingAnalysis();

        StateMachineReport.R.println_subsubsubsection("State analysis:"); //$NON-NLS-1$
        StateMachineReport.R.inc();

        try {
            DefaultInterruptionEventRunner dier = new DefaultInterruptionEventRunner(this, NonNullUtils.checkNotNull(Attributes.STATE), new StateInterruptionEventGetter());
            smva.doAnalysis(dier);
        } catch (StateMachineNoValidCaseException | StateMachineNoInvalidCaseException e) {
            e.printStackTrace();
            return null;
        }

        StateMachineReport.R.printDropped(smva);

        StateMachineReport.R.println(smva.getDifferentValidCases() +
                " different valid situation" + (smva.getDifferentValidCases() > 1 ? "s" : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                " for " + validIsiList.size() + //$NON-NLS-1$
                " different valid case" + (validIsiList.size() > 1 ? "s" : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "." //$NON-NLS-1$
        );
        StateMachineReport.R.println(smva.getDifferentInvalidCases() +
                " different invalid situation" + (smva.getDifferentInvalidCases() > 1 ? "s" : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                " for " + invalidIsiList.size() + //$NON-NLS-1$
                " different invalid case" + (invalidIsiList.size() > 1 ? "s" : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "." //$NON-NLS-1$
        );
        StateMachineReport.R.println();

        ResponsibilityMap<InterruptionReason> analysisResponsibilityMap = smva.getAnalysisResponsibilityMap();

        StateMachineReport.R.println("General computed difference between invalid and valid instances:"); //$NON-NLS-1$
        String[] header = { "State", "Responsibility for added time" }; //$NON-NLS-1$ //$NON-NLS-2$
        String[][] strs = new String[analysisResponsibilityMap.getCollection().size()][2];
        int idx = 0;
        int progressSize = 20;
        for (Entry<InterruptionReason, Double> entry : analysisResponsibilityMap.getCollection()) {
            double ratioValue = entry.getValue() * 100;
            String symbol = StateMachineReport.progressBar(ratioValue, progressSize);

            strs[idx][0] = entry.getKey().getID();
            strs[idx][1] = String.format("%.2f%% %s", ratioValue, symbol); //$NON-NLS-1$
            idx++;
        }
        StateMachineReport.R.println_table(header, strs, Arrays.asList(new Integer[] { 1 }));
        StateMachineReport.R.println(String.format(
                "Minimum responsibility for a case to be considered: %.2f%%", //$NON-NLS-1$
                analysisResponsibilityMap.getMinProba() * 100));
        StateMachineReport.R.println();

        StateMachineReport.R.println_subsubsubsubsection("Analysis of the states to keep for consideration:"); //$NON-NLS-1$
        for (Entry<InterruptionReason, Double> entry : analysisResponsibilityMap.getTopStdDevCollection()) {
            StateMachineReport.R.println(String.format(
                    "%s (responsible for %.2f%% of the time added in invalid cases)", //$NON-NLS-1$
                    entry.getKey().getID(),
                    (entry.getValue() * 100)));
            StateMachineReport.R.inc();

            if (entry.getKey() instanceof State) {
                BackendStateValue val = NonNullUtils.checkNotNull(((State) entry.getKey()).getValue());
                switch (val) {
                case UNKNOWN:
                    StateMachineReport.R.println("Now we can't do a thorough analysis, can we?"); //$NON-NLS-1$
                    break;
                case SYSCALL:
                case BLOCKED:
                    smva.runPriorityInheritance();
                    smva.runCriticalPath();
                    break;
                case HRTIMER:
                    StateMachineReport.R.println("Further analysis for HRTIMER ?"); //$NON-NLS-1$
                    break;
                case IRQ:
                    StateMachineReport.R.println("Further analysis for IRQ ?"); //$NON-NLS-1$
                    break;
                case PREEMPTED:
                    StateMachineReport.R.println("Further analysis for PREEMPTED ?"); //$NON-NLS-1$
                    break;
                case RUNNING:
                    smva.runStateMachineStateAnalysis();
                    break;
                case SOFTIRQ_BLOCK:
                case SOFTIRQ_BLOCK_IOPOLL:
                case SOFTIRQ_HI:
                case SOFTIRQ_HRTIMER:
                case SOFTIRQ_NET_RX:
                case SOFTIRQ_NET_TX:
                case SOFTIRQ_RCU:
                case SOFTIRQ_SCHED:
                case SOFTIRQ_TASKLET:
                case SOFTIRQ_TIMER:
                    StateMachineReport.R.println("Further analysis for SOFTIRQ ?"); //$NON-NLS-1$
                    break;
                case WAKING:
                    smva.runCriticalPath();
                    break;
                default:
                    StateMachineReport.R.println("We don't know why we're here"); //$NON-NLS-1$
                    break;
                }
            }

            StateMachineReport.R.dec();
        }

        StateMachineReport.R.dec();

        return smva;
    }

    private static class StateInterruptionEventGetter extends InterruptionEventGetter {
        private String lastSyscall = null;

        @Override
        public InterruptionEventList execute(TimestampInterval ti, ITmfStateInterval si, ITmfEvent e)
                throws StateMachineStateSystemNotFoundException, StateMachineTIDNotFoundException, StateMachineStateSystemTimeRangeException, StateMachineUnexpectedEventException {
            InterruptionEventList taskList = new InterruptionEventList();

            // Get the start and end of the state, given the start and end of
            // the interval
            long startTime = Math.max(si.getStartTime(), ti.getStartTime().getValue());
            long endTime = Math.min(si.getEndTime(), ti.getEndTime().getValue());

            Long stateValue = si.getStateValue().unboxLong();
            BackendStateValue s = BackendStateValue.getValue(stateValue);
            if (s == BackendStateValue.UNKNOWN) {
                // Could be considered later, but we'll consider we can't use
                // this for the analysis for now
                throw new StateMachineStateSystemTimeRangeException();
            }

            String data = null;
            if (s == BackendStateValue.SYSCALL) {
                IKernelAnalysisEventLayout layout = ((IKernelTrace) e.getTrace()).getKernelEventLayout();
                if (!(e.getName().startsWith(layout.eventSyscallEntryPrefix())
                        || e.getName().equals(layout.eventSchedSwitch())
                        || e.getName().equals(layout.eventSoftIrqExit()) || e.getName().equals("irq_softirq_exit") //$NON-NLS-1$
                        || e.getName().equals(layout.eventHRTimerExpireExit()) || e.getName().equals("timer_hrtimer_expire_exit") //$NON-NLS-1$
                        || e.getName().equals(layout.eventIrqHandlerExit()))) {
                    throw new StateMachineUnexpectedEventException(
                            String.format("The event should be a '%s', a '%s', a '%s' or a '%s', but is a '%s' instead", //$NON-NLS-1$
                                    layout.eventSyscallEntryPrefix(),
                                    layout.eventSchedSwitch(),
                                    layout.eventIrqHandlerExit(),
                                    layout.eventSoftIrqExit(),
                                    e.getName()));
                }

                if (e.getType().getName().startsWith(layout.eventSyscallEntryPrefix())) {
                    lastSyscall = e.getType().getName().substring(layout.eventSyscallEntryPrefix().length());
                }
                data = lastSyscall;
            } else if (s == BackendStateValue.BLOCKED) {
                data = lastSyscall;
            } else if (s == BackendStateValue.PREEMPTED) {
                IKernelAnalysisEventLayout layout = ((IKernelTrace) e.getTrace()).getKernelEventLayout();
                if (!e.getName().equals(layout.eventSchedSwitch())) {
                    throw new StateMachineUnexpectedEventException(
                            String.format("The event should be a '%s', but is a '%s' instead", //$NON-NLS-1$
                                    layout.eventSchedSwitch(),
                                    e.getName()));
                }

                ITmfEventField content = e.getContent();
                data = String.format("%s (%s)", //$NON-NLS-1$
                        NonNullUtils.checkNotNull(content.getField(layout.fieldNextComm())).getFormattedValue(),
                        NonNullUtils.checkNotNull(content.getField(layout.fieldNextTid())).getFormattedValue());
            } else if (s == BackendStateValue.IRQ) {
                IKernelAnalysisEventLayout layout = ((IKernelTrace) e.getTrace()).getKernelEventLayout();
                if (!e.getName().equals(layout.eventIrqHandlerEntry())) {
                    throw new StateMachineUnexpectedEventException(
                            String.format("The event should be a '%s', but is a '%s' instead", //$NON-NLS-1$
                                    layout.eventIrqHandlerEntry(),
                                    e.getName()));
                }
                ITmfEventField content = e.getContent();
                data = String.format("(%s)", //$NON-NLS-1$
                        content.getField(layout.fieldName()).getFormattedValue());
            }
            State state = new State(stateValue, data);

            // Compute the actual duration (to print, so not useful in the end)
            // double xx = ((double)endTime - (double)startTime) / 1e6;
            /*
             * System.out.print("STATE " + state.toString());
             * System.out.print(" (" + si.getStateValue().unboxLong() + ")");
             * System.out.print(" FOR " + xx + "ms");
             * System.out.printf(" (at time %d / %s)\n", startTime,
             * e.getTimestamp().toString());
             */
            // System.out.println(e);

            taskList.events.add(new InterruptionEvent(e,
                    TmfTimestamp.create(startTime, ITmfTimestamp.NANOSECOND_SCALE),
                    TmfTimestamp.create(endTime, ITmfTimestamp.NANOSECOND_SCALE),
                    state));

            return taskList;
        }
    }

}
