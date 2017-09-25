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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.tracecompass.common.core.NonNullUtils;

/**
 * A special interval set that represents the difference between intervals or
 * durations sets
 *
 * @author Raphaël Beamonte
 */
public class InterruptionIntervalSetDiff {
    private Map<InterruptionInterval, Integer> map = new TreeMap<>(new Comparator<InterruptionInterval>() {
        @Override
        public int compare(InterruptionInterval ii1, InterruptionInterval ii2) {
            return ii1.getReason().compareTo(ii2.getReason());
        }
    });

    /**
     * Constructor
     */
    public InterruptionIntervalSetDiff() {
    }

    /**
     * Constructor
     *
     * @param iis
     *            The interval set to use to initialize this interval set diff
     */
    public InterruptionIntervalSetDiff(InterruptionIntervalSet iis) {
        addAll(iis);
    }

    /**
     * Constructor
     *
     * @param ids
     *            The duration set to use to initialize this interval set diff
     */
    public InterruptionIntervalSetDiff(InterruptionDurationSet ids) {
        addAll(ids);
    }

    /**
     * Add a duration to the interval set diff
     *
     * @param id
     *            The duration to add to this interval set diff
     * @param count
     *            The number of times to add this duration in the interval set
     *            diff
     */
    public void add(InterruptionDuration id, int count) {
        add(new InterruptionInterval(id), count);
    }

    /**
     * Add an interval to the interval set diff
     *
     * @param ii
     *            The interval to add to this interval set diff
     * @param count
     *            The number of times to add this interval in the interval set
     *            diff
     */
    public void add(InterruptionInterval ii, int count) {
        Integer mapCount = map.get(ii);
        if (mapCount == null) {
            map.put(ii, count);
        } else {
            for (InterruptionInterval ii2 : map.keySet()) {
                if (ii.getReason().compareTo(ii2.getReason()) == 0) {
                    ii.addTsInterval(ii2.getTsIntervals());
                    if (ii.getMaxDuration() < ii2.getMaxDuration()) {
                        ii.setMaxDuration(ii2.getMaxDuration());
                    }
                    if (ii.getMinDuration() > ii2.getMinDuration()) {
                        ii.setMinDuration(ii2.getMinDuration());
                    }
                    break;
                }
            }
            map.remove(ii);
            map.put(ii, mapCount + count);
        }
    }

    /**
     * Add all the durations of a duration set to the interval set diff
     *
     * @param ids
     *            The duration set to add to this interval set diff
     */
    public void addAll(InterruptionDurationSet ids) {
        for (InterruptionDuration id : ids.getDurations()) {
            add(id, 1);
        }
    }

    /**
     * Add all the intervals of an interval set to the interval set diff
     *
     * @param iis
     *            The interval set to add to this interval set diff
     */
    public void addAll(InterruptionIntervalSet iis) {
        for (InterruptionInterval ii : iis.getIntervals()) {
            add(ii, 1);
        }
    }

    /**
     * @return The sum of the minimum durations of all the intervals in the
     *         interval set diff
     */
    public long getMinTotal() {
        long minTotal = 0;
        for (InterruptionInterval ii : map.keySet()) {
            minTotal += ii.getMinDuration();
        }
        return minTotal;
    }

    /**
     * @return The sum of the maximum durations of all the intervals in the
     *         interval set diff
     */
    public long getMaxTotal() {
        long maxTotal = 0;
        for (InterruptionInterval ii : map.keySet()) {
            maxTotal += ii.getMaxDuration();
        }
        return maxTotal;
    }

    /**
     * @return The responsibility map for the elements of the interval set diff
     *         using the counter of each interval
     */
    public ResponsibilityMap<InterruptionInterval> getCounterResponsibilityMap() {
        ResponsibilityMap<InterruptionInterval> responsibilityMap = new ResponsibilityMap<>();

        double total = 0;
        for (InterruptionInterval ii : map.keySet()) {
            total += ii.getCounter();
        }

        for (InterruptionInterval ii : map.keySet()) {
            Double resp = ii.getCounter() / total;

            responsibilityMap.put(ii, resp);
        }

        return responsibilityMap;
    }

    /**
     * @return The responsibility map for the elements of the interval set diff
     *         using the counter of each reason
     */
    public ResponsibilityMap<InterruptionReason> getReasonCounterResponsibilityMap() {
        ResponsibilityMap<InterruptionReason> responsibilityMap = new ResponsibilityMap<>();

        double total = 0;
        for (InterruptionInterval ii : map.keySet()) {
            total += ii.getTsIntervals().size();
        }

        for (InterruptionInterval ii : map.keySet()) {
            InterruptionReason ir = ii.getReason();
            Double resp = ii.getTsIntervals().size() / total;

            if (responsibilityMap.containsKey(ir)) {
                resp += responsibilityMap.get(ir);
            }

            responsibilityMap.put(ir, resp);
        }

        return responsibilityMap;
    }

    /**
     * @return A map with computed responsibilities for min and max durations of
     *         each interval in the set diff
     */
    public Map<InterruptionInterval, Double[]> getResponsibilityMap() {
        Map<InterruptionInterval, Double[]> responsibilityMap = new HashMap<>();

        long minTotal = 0;
        long maxTotal = 0;
        for (InterruptionInterval ii : map.keySet()) {
            minTotal += ii.getMinDuration();
            maxTotal += ii.getMaxDuration();
        }

        for (InterruptionInterval ii : map.keySet()) {
            Double[] resp = new Double[2];

            if (minTotal == 0) {
                if (maxTotal == 0) {
                    resp[0] = resp[1] = 1.0 / map.size();
                } else {
                    resp[0] = resp[1] = ii.getMaxDuration() / maxTotal;
                }
            } else if (maxTotal == 0) {
                resp[0] = resp[1] = ii.getMinDuration() / minTotal;
            } else {
                resp[0] = ii.getMinDuration() / minTotal;
                resp[1] = ii.getMaxDuration() / maxTotal;
            }

            responsibilityMap.put(ii, resp);
        }

        return responsibilityMap;
    }

    /**
     * @return A responsibility map over the interval using the mean
     *         responsibility between min and max durations of the interval
     */
    public ResponsibilityMap<InterruptionInterval> getIntervalMeanResponsibilityMap() {
        ResponsibilityMap<InterruptionInterval> meanResponsibilityMap = new ResponsibilityMap<>();
        Map<InterruptionInterval, Double[]> responsibilityMap = getResponsibilityMap();

        for (Entry<InterruptionInterval, Double[]> entry : responsibilityMap.entrySet()) {
            Double resp = new Double((entry.getValue()[0] + entry.getValue()[1]) / 2.0);
            meanResponsibilityMap.put(entry.getKey(), resp);
        }

        return meanResponsibilityMap;
    }

    /**
     * @return A responsibility map over the reason using the mean
     *         responsibility between min and max durations of the interval
     */
    public ResponsibilityMap<InterruptionReason> getMeanResponsibilityMap() {
        ResponsibilityMap<InterruptionReason> meanResponsibilityMap = new ResponsibilityMap<>();
        Map<InterruptionInterval, Double[]> responsibilityMap = getResponsibilityMap();

        for (Entry<InterruptionInterval, Double[]> entry : responsibilityMap.entrySet()) {
            Double resp = new Double((entry.getValue()[0] + entry.getValue()[1]) / 2.0);
            meanResponsibilityMap.put(entry.getKey().getReason(), resp);
        }

        return meanResponsibilityMap;
    }

    /**
     * Perform an inter-union operation: inter as we keep only the reasons that
     * are common between the two interval set diff, and union as we merge the
     * intervals that share a reason.
     *
     * @param iisd
     *            The interval set diff with which to the the inter-union
     *            operation
     * @return The computed interval set diff after the inter-union operation
     */
    public InterruptionIntervalSetDiff interUnion(InterruptionIntervalSetDiff iisd) {
        // Create a new instance
        InterruptionIntervalSetDiff newiisd = new InterruptionIntervalSetDiff();

        /*
         * Choose which instance to loop, and which one to search in, depending
         * on their respective sizes. The smallest one will be looped
         */
        InterruptionIntervalSetDiff toLoop, toSearch;
        if (map.size() > iisd.map.size()) {
            toLoop = iisd;
            toSearch = this;
        } else {
            toLoop = this;
            toSearch = iisd;
        }

        /*
         * Create and fill a Map that will allow to get and interval from a
         * reason. Here, it is considered there can be only one interval per
         * reason. It is possible in the future that we could need to extend
         * that.
         */
        Map<InterruptionReason, InterruptionInterval> searchMap = new HashMap<>();
        for (Entry<InterruptionInterval, Integer> entry : toSearch.map.entrySet()) {
            searchMap.put(entry.getKey().getReason(), entry.getKey());
        }

        // For each entry of the instance to loop...
        for (Entry<InterruptionInterval, Integer> entry : toLoop.map.entrySet()) {
            // ... get the interval and its reason
            InterruptionInterval ii = entry.getKey();
            InterruptionReason ir = ii.getReason();

            // ... search if there is an interval with the same reason in the
            // other
            InterruptionInterval iiOther = searchMap.get(ir);
            if (iiOther == null) {
                /*
                 * ... if there isn't, that doesn't satisfy the 'inter' aspect
                 * of that method, thus, just continue to the next interval.
                 */
                continue;
            }

            /*
             * ... get the minimum number of times that interval appears between
             * this instance and the other one.
             */
            Integer count = Math.min(entry.getValue(), NonNullUtils.checkNotNull(toSearch.map.get(iiOther)));

            // Then create a new interval with that same reason
            InterruptionInterval newii = new InterruptionInterval(ir);
            // For which the minimum duration is the minimum of our two
            // intervals
            newii.setMinDuration(Math.min(ii.getMinDuration(), iiOther.getMinDuration()));
            // And the maximum duration is the maximum of our two intervals
            newii.setMaxDuration(Math.max(ii.getMaxDuration(), iiOther.getMaxDuration()));
            // Its counter is also the minimum number of times that interval has
            // been filled in both
            newii.setCounter(Math.min(ii.getCounter(), iiOther.getCounter()));
            // Then put back inside the timestamp intervals of the actual
            // periods for the current instance
            if (toSearch == this) {
                newii.addTsInterval(iiOther.getTsIntervals());
            } else {
                newii.addTsInterval(ii.getTsIntervals());
            }

            // Finally, add that interval to our new instance
            newiisd.add(newii, count);
        }

        return newiisd;
    }

    /**
     * @return A collection of all the intervals in the interval set diff
     */
    public Collection<InterruptionInterval> getIntervals() {
        return map.keySet();
    }

    @Override
    public String toString() {
        String str = "Diff: [\n"; //$NON-NLS-1$
        for (Entry<InterruptionInterval, Integer> entry : map.entrySet()) {
            /*
             * str += String.format(
             * "    [%s, %d, between %.3f and %.3f us],\n", //$NON-NLS-1$
             * entry.getKey().getReason(), entry.getValue(),
             * entry.getKey().getMinDuration() / 1e3,
             * entry.getKey().getMaxDuration() / 1e3 );
             */
            str += "    [" + entry.getKey().getReason() + ", " + entry.getValue() + ", [" + entry.getKey().getMinDuration() + ", " + entry.getKey().getMaxDuration() + "]]\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        }
        str += "]"; //$NON-NLS-1$
        return str;
    }
}