/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

import com.google.common.collect.Multimap;

/**
 * @author Raphaël Beamonte
 *
 */
public class StateMachineVariableHelpers {

    /**
     * @param e A sched_switch event
     * @return The name and pid of the process that was scheduled
     */
    public static String getSchedSwitchNextProcessInformation(ITmfEvent e) {
        // Get the analysis event layout
        IKernelAnalysisEventLayout layout = ((IKernelTrace) e.getTrace()).getKernelEventLayout();

        // Get event content
        ITmfEventField content = e.getContent();

        // Get the name of the task that preempted ours
        String taskname = content.getField(layout.fieldNextComm()).getFormattedValue();
        // Get the TID of the task that preempted ours
        int tasktid = Integer.parseInt(content.getField(layout.fieldNextTid()).getFormattedValue());

        return String.format("%s %d", taskname, tasktid); //$NON-NLS-1$
        //return taskname;
    }

    /**
     * To compute the sample size to use for a population
     *
     * @param N The size of the population
     * @param z The z-score according to the confidence (1.96 for 95%, 2.276 for 99%)
     * @param p The known probability (use .5 for unknown probability)
     * @param e The error margin (usually around 3% or 5%)
     * @return The sample size
     */
    public static double sampleSize(double N, double z, double p, double e) {
        double ss = Math.pow(z, 2) * p * (1 - p) / Math.pow(e, 2);
        double ssV = ss / (1 + ss / N);
        return Math.ceil(ssV);
    }

    /**
     * @param mmap The multimap for which we want a keymap
     * @return the KeyMap for the given multimap, which is a map mapping
     * the key of the multimap to the number of times that key appears in
     * the multimap.
     */
    static public <K, V> Map<K, Integer> keyMap(final Multimap<K, V> mmap) {
        Map<K, Integer> keymap = new HashMap<>();

        for (Entry<K, Collection<V>> entry : mmap.asMap().entrySet()) {
            keymap.put(entry.getKey(), entry.getValue().size());
        }

        return keymap;
    }

}
