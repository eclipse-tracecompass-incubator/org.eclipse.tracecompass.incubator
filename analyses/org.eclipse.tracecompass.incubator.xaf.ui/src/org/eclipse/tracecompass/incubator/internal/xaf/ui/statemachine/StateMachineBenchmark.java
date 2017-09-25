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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map.Entry;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;

/**
 * To print the benchmarks data in a common centralized way for the analysis
 *
 * FIXME: Move elsewhere or remove
 *
 * @author Raphaël Beamonte
 */
public class StateMachineBenchmark {

    private static final HashMultimap<String, StateMachineBenchmark> benchmarks = HashMultimap.create();
    private final int localInc;
    private static int inc = 0;

    /**
     * Print the results of all the benchmarks
     */
    public final static void printBenchmarks() {
        // Sort by appearing time
        ArrayList<Entry<String, Collection<StateMachineBenchmark>>> list = new ArrayList<>(benchmarks.asMap().entrySet());
        list.sort(new Comparator<Entry<String, Collection<StateMachineBenchmark>>>() {
            Comparator<StateMachineBenchmark> smbComp = new Comparator<StateMachineBenchmark>() {
                @Override
                public int compare(StateMachineBenchmark smb1, StateMachineBenchmark smb2) {
                    return smb1.startTime.compareTo(smb2.startTime);
                }
            };

            @Override
            public int compare(Entry<String, Collection<StateMachineBenchmark>> e1, Entry<String, Collection<StateMachineBenchmark>> e2) {
                StateMachineBenchmark min1 = Collections.min(e1.getValue(), smbComp);
                StateMachineBenchmark min2 = Collections.min(e2.getValue(), smbComp);
                return smbComp.compare(min1, min2);
            }
        });
        for (Entry<String, Collection<StateMachineBenchmark>> entry : list) {
            long duration = 0;
            int padding = 0;
            for (StateMachineBenchmark smb : entry.getValue()) {
                if (smb.localInc > padding) {
                    padding = smb.localInc;
                }
                duration += smb.getDuration();
            }
            StateMachineReport.benchmark(String.format("%s = %d ns", //$NON-NLS-1$
                    entry.getKey(), duration));
            StateMachineReport.debug(String.format(Strings.padStart("%s = %f ms", padding, '\t'), //$NON-NLS-1$
                    entry.getKey(), duration / 1e6));
        }
    }

    private Long startTime = null;
    private Long endTime = null;

    /**
     * Create a new benchmark
     * @param name The name of the new benchmark
     */
    public StateMachineBenchmark(String name) {
        benchmarks.put(name, this);
        localInc = inc;
        inc++;
        startTime = System.nanoTime();
    }

    /**
     * Stop the current benchmark
     */
    public void stop() {
        endTime = System.nanoTime();
        inc--;
    }

    /**
     * @return the duration of the current benchmark
     */
    public long getDuration() {
        if (startTime != null) {
            if (endTime != null) {
                return endTime - startTime;
            }
            return System.nanoTime() - startTime;
        }
        return 0;
    }


}
