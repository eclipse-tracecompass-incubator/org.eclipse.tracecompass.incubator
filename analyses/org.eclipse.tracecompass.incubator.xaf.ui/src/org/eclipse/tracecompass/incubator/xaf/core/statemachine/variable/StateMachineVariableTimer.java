/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineBenchmark;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstanceGroup;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineReport;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance.InstanceStepInformation;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.BackendStateValue;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.StateMachineBackendAnalysis;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.Operator;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.StateMachineConstraint;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.ValueType;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineNoInvalidCaseException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineNoValidCaseException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineStateSystemNotFoundException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineStateSystemTimeRangeException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineTIDNotFoundException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineVariableException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariableAnalysis.InterruptionEventRunner;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionEvent;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionEventList;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionReason;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.ResponsibilityMap;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.State;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.StateMachineVariableHelpers;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Class that represents a state machine variable of timer class
 *
 * @author Raphaël Beamonte
 */
public abstract class StateMachineVariableTimer extends StateMachineVariable {

    /**
     * @param name
     *            The name of the variable
     * @param value
     *            The initial value of the variable
     */
    @SuppressWarnings("rawtypes")
    public StateMachineVariableTimer(String name, Comparable value) {
        super(name, value);
    }

    /**
     * @return The attribute of this variable in the backend
     */
    public abstract String getTimerAttribute();

    @Override
    public Comparable getValue(ITmfEvent event, StateMachineInstance stateMachineInstance) throws StateMachineVariableException {
        if (stateMachineInstance.getStateMachineInstanceGroup().getStateMachineBackendAnalysisModules().isEmpty()) {
            throw new StateMachineVariableException("State system unavailable"); //$NON-NLS-1$
        }

        ITmfTimestamp intervalStart = (ITmfTimestamp) getValue();
        ITmfTimestamp intervalEnd = event.getTimestamp();

        int tid = stateMachineInstance.getVTid();
        long usage = 0;
        for (StateMachineBackendAnalysis kernelAnalysisModule : stateMachineInstance.getStateMachineInstanceGroup().getStateMachineBackendAnalysisModules()) {
            Long lusage = kernelAnalysisModule.getTimerIntvl(tid, intervalStart.getValue(), intervalEnd.getValue(), getTimerAttribute());
            if (lusage != null) {
                usage += lusage;
            }
        }

        return usage;
    }

    @Override
    public Integer compareTo(Comparable toCompare, ITmfEvent event, StateMachineInstance stateMachineInstance) throws StateMachineVariableException {
        ITmfTimestamp intervalStart = (ITmfTimestamp) getValue();
        ITmfTimestamp intervalEnd = event.getTimestamp();

        long usage = (long) getValue(event, stateMachineInstance);
        double intvl = intervalEnd.getValue() - intervalStart.getValue();
        Integer comparison = new Double(usage).compareTo(StateMachineUtils.getValueFromPercent((String) toCompare, intvl));

        return comparison;
    }

    @Override
    public Double getNumericalValue(Comparable format, ITmfEvent event, StateMachineInstance stateMachineInstance) {
        try {
            Double delta = ((Long) getValue(event, stateMachineInstance)).doubleValue();

            if (((String) format).endsWith("%")) { //$NON-NLS-1$
                ITmfTimestamp intervalStart = (ITmfTimestamp) getValue();
                ITmfTimestamp intervalEnd = event.getTimestamp();
                double intvl = intervalEnd.getValue() - intervalStart.getValue();
                delta = delta / intvl * 100;
            }

            return delta;
        } catch (StateMachineVariableException e) {
            return null;
        }
    }

    @Override
    public String getFormattedValue(Comparable format, ITmfEvent event, StateMachineInstance stateMachineInstance) {
        Double delta;
        try {
            delta = ((Long) getValue(event, stateMachineInstance)).doubleValue();

            if (((String) format).endsWith("%")) { //$NON-NLS-1$
                ITmfTimestamp intervalStart = (ITmfTimestamp) getValue();
                ITmfTimestamp intervalEnd = event.getTimestamp();
                double intvl = intervalEnd.getValue() - intervalStart.getValue();
                delta = delta / intvl * 100;
            }
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

        String result = null;

        if (((String) format).endsWith("%")) { //$NON-NLS-1$
            result = String.format("%.4f%%", //$NON-NLS-1$
                    value);
        } else {
            result = String.valueOf(value);
        }
        return result;
    }

    /**
     * Interruption event runner for Timers
     *
     * @author Raphaël Beamonte
     */
    public class DefaultTimerInterruptionEventRunner extends InterruptionEventRunner {
        private final StateMachineVariableTimer variable;
        private final String attribute;
        private Map<ITmfTrace, ITmfContext> contextPerTrace = new HashMap<>();

        /**
         * To create a DefaultTimerInterruptionEventRunner
         *
         * @param variable
         *            The variable for which this interruption event runner will
         *            run
         */
        public DefaultTimerInterruptionEventRunner(StateMachineVariableTimer variable) {
            this.variable = variable;
            this.attribute = variable.getTimerAttribute();
        }

        @Override
        public List<InterruptionEventList> execute(InstanceStepInformation isi) throws StateMachineStateSystemNotFoundException, StateMachineTIDNotFoundException, StateMachineStateSystemTimeRangeException {
            // Get the instance group
            StateMachineInstanceGroup group = isi.instance.getStateMachineInstanceGroup();

            /*
             * If we don't have kernel analysis modules for that group, we can
             * just drop that analysis as we won't have the needed information
             * to analyze that constraint. We can thus return.
             */
            if (group.getStateMachineBackendAnalysisModules().isEmpty()) {
                throw new StateMachineStateSystemNotFoundException();
            }

            /*
             * Thread ID of our instance. We currently only manage situations in
             * which our model follows an unique TID
             */
            Integer tid = isi.instance.getVTid();
            if (tid == null) {
                throw new StateMachineTIDNotFoundException();
            }

            /*
             * Get the timestamp interval for this variable application
             */
            TimestampInterval ti = isi.getVariableInterval(variable.getName());

            /*
             * List in which we will store the events corresponding to an
             * increment of the counter on which that variable is based
             */
            List<InterruptionEventList> interruptionEventList = new ArrayList<>();

            /// USING THE NEW STATE ATTRIBUTE
            boolean analysisDone = false;
            // For each analysis module...
            for (StateMachineBackendAnalysis stateMachineBackendAnalysis : group.getStateMachineBackendAnalysisModules()) {
                ITmfStateSystem ss = stateMachineBackendAnalysis.getStateSystem();
                if (ss == null || ss.getCurrentEndTime() < ti.getEndTime().getValue() || ss.getStartTime() > ti.getStartTime().getValue()) {
                    continue;
                }
                analysisDone = true;

                // Kernel trace on which we're currently working
                ITmfTrace trace = stateMachineBackendAnalysis.getKernelTrace();

                // To store the events we'll read
                ITmfEvent e = null;

                // To store the context for this trace
                ITmfContext ctx = contextPerTrace.get(trace);

                if (ctx != null) {
                    // Get the next event in the trace
                    e = trace.getNext(ctx);

                    // If our event is after our intervalStart... we'll need to
                    // seek :(
                    if (e.getTimestamp().compareTo(ti.getStartTime()) > 0) {
                        ctx.dispose();
                        ctx = null;
                    }
                }

                if (ctx == null) {
                    ctx = trace.seekEvent(ti.getStartTime());
                    contextPerTrace.put(trace, ctx);

                    // Get the initial event of the interval
                    e = trace.getNext(ctx);
                }

                // Can't happen
                if (e == null) {
                    System.out.println("e is null and can't be null... what happened?!"); //$NON-NLS-1$
                    return null;
                }

                // Long value of the interval start timestamp
                long seek = ti.getStartTime().getValue();

                /*
                 * Get the whole interval for which the counter had the same
                 * value as the one it has at seeking time 'seek'
                 */
                StateMachineBenchmark benchmarkObject = new StateMachineBenchmark("Analysis SS Get"); //$NON-NLS-1$
                ITmfStateInterval si = stateMachineBackendAnalysis.getStateInterval(tid, seek, attribute);
                benchmarkObject.stop();

                /*
                 * Change the value of 'seek' to reference the start of the next
                 * value interval for that counter
                 */
                seek = si.getEndTime() + 1;

                /*
                 * If the new value of 'seek' is before the end of the interval,
                 * it means we incremented the counter during that interval, we
                 * thus need to get the event information and store it in our
                 * list. We'll do that as long as our 'seek' value stays before
                 * the end of the interval...
                 */

                IKernelAnalysisEventLayout layout = ((IKernelTrace) e.getTrace()).getKernelEventLayout();
                while (seek < ti.getEndTime().getValue()) {
                    // Loop until we get to the event we're searching
                    while (e.getTimestamp().getValue() < seek
                            || !e.getName().equals(layout.eventSchedSwitch())) {
                        e = trace.getNext(ctx);
                    }

                    /*
                     * Get the whole interval for which the counter had the same
                     * value as the one it has at seeking time 'seek'
                     */
                    benchmarkObject = new StateMachineBenchmark("Analysis SS Get"); //$NON-NLS-1$
                    si = stateMachineBackendAnalysis.getStateInterval(tid, seek, attribute);
                    benchmarkObject.stop();

                    /*
                     * The event should never be 'null'. If it is, show an
                     * error. If not, add it to the list.
                     */
                    if (e.getTimestamp().getValue() > seek) {
                        System.out.println("Didn't find the event at " + seek); //$NON-NLS-1$
                    } else {
                        ITmfEventField content = e.getContent();
                        int tasktid = Integer.parseInt(content.getField(layout.fieldNextTid()).getFormattedValue());

                        // Get the reason of the interruption
                        int state = Integer.parseInt(content.getField(layout.fieldPrevState()).getFormattedValue());

                        InterruptionEventList taskList = new InterruptionEventList();

                        benchmarkObject = new StateMachineBenchmark("Analysis SS Get"); //$NON-NLS-1$
                        ITmfStateInterval si2 = stateMachineBackendAnalysis.getStateInterval(tasktid, seek, attribute);
                        benchmarkObject.stop();

                        while (si2.getStartTime() < si.getEndTime()) {
                            State stateObj = new State(
                                    ((state == 0 || state == 1024) ? BackendStateValue.PREEMPTED : BackendStateValue.BLOCKED).getValue(),
                                    StateMachineVariableHelpers.getSchedSwitchNextProcessInformation(e));
                            taskList.events.add(new InterruptionEvent(
                                    e, TmfTimestamp.create(si2.getEndTime(), ITmfTimestamp.NANOSECOND_SCALE),
                                    stateObj));

                            while (e.getTimestamp().getValue() < si2.getEndTime()
                                    || !e.getName().equals(layout.eventSchedSwitch())) {
                                e = trace.getNext(ctx);
                            }

                            tasktid = Integer.parseInt(e.getContent().getField(layout.fieldNextTid()).getFormattedValue());
                            if (tasktid == tid) {
                                break;
                            }

                            benchmarkObject = new StateMachineBenchmark("Analysis SS Get"); //$NON-NLS-1$
                            si2 = stateMachineBackendAnalysis.getStateInterval(tasktid, si2.getEndTime() + 1, attribute);
                            benchmarkObject.stop();
                        }

                        if (tasktid != tid) {
                            benchmarkObject = new StateMachineBenchmark("Analysis SS Get"); //$NON-NLS-1$
                            taskList.events.add(new InterruptionEvent(e,
                                    TmfTimestamp.create(
                                            si.getEndTime() - stateMachineBackendAnalysis.getTimerIntvl(tid, si.getStartTime(), si.getEndTime(), attribute),
                                            ITmfTimestamp.NANOSECOND_SCALE)));
                            benchmarkObject.stop();
                        }

                        interruptionEventList.add(taskList);
                    }

                    /*
                     * Change the value of seek to be able to seek the next
                     * interval in the next loop
                     */
                    seek = si.getEndTime() + 1;
                }
            }

            // If we couldn't do the analysis
            if (!analysisDone) {
                throw new StateMachineStateSystemTimeRangeException();
            }

            return interruptionEventList;
        }
    }

    /**
     * @return The interruption event runner for that variable
     */
    public InterruptionEventRunner getInterruptionEventRunner() {
        return new DefaultTimerInterruptionEventRunner(this);
    }

    @Override
    public StateMachineVariableAnalysis analyze(StateMachineConstraint constraint, List<InstanceStepInformation> invalidIsiList, List<InstanceStepInformation> validIsiList) {
        // Treat only constants for now
        // TODO: treat comparison between variables
        if (constraint.getValueType() != ValueType.CONSTANT) {
            System.out.println("TODO"); //$NON-NLS-1$
            return null;
        }

        boolean partial = (constraint.getOperator() == Operator.EQ && constraint.getValue().matches("^100 *%$")); //$NON-NLS-1$

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
        String[] header = { "Concerning process", "Responsibility" }; //$NON-NLS-1$ //$NON-NLS-2$
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
        StateMachineReport.R.println_table(header, strs, Arrays.asList(new Integer[] { 1 }));
        StateMachineReport.R.println();

        StateMachineReport.R.dec();

        return smva;
    }

    @Override
    public String getVariableType() {
        return this.getClass()
                .getSimpleName()
                .substring(StateMachineVariableTimer.class.getSimpleName().length());
    }

    @Override
    public String toString() {
        return String.format("(%s)%s", //$NON-NLS-1$
                getVariableType(),
                super.toString());
    }

}
