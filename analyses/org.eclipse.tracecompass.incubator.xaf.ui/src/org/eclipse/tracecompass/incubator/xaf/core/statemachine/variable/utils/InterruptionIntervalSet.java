/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.Direction;

import com.google.common.collect.HashMultimap;

/**
 * Set of interruption interval objects
 *
 * @author Raphaël Beamonte
 */
public class InterruptionIntervalSet implements Cloneable {
    private HashMultimap<InterruptionReason, InterruptionInterval> map = HashMultimap.create();
    private int counter = 0;
    private Direction fDirection = null;

    /**
     * Constructor
     */
    public InterruptionIntervalSet() {
    }

    /**
     * Constructor
     *
     * @param direction
     *            The direction of the interval set
     */
    public InterruptionIntervalSet(Direction direction) {
        this.fDirection = direction;
    }

    /**
     * @return Whether the interval set is empty or not
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * @return The direction of the interval set
     */
    public Direction getDirection() {
        return fDirection;
    }

    /**
     * Add an interval to the interval set
     *
     * @param ii
     *            The interval to add to the interval set
     */
    public void add(InterruptionInterval ii) {
        map.put(ii.reason, ii);
    }

    /**
     * @return The number of duration set put in that interval set
     */
    public int getCounter() {
        return counter;
    }

    /**
     * Add a duration into the closest interval we can find that matches its
     * reason, or create a new interval if we couldn't find any
     *
     * @param id
     *            The duration to add
     */
    public void addToCloser(InterruptionDuration id) {
        List<InterruptionInterval> list = new ArrayList<>(map.get(id.reason));

        if (list.isEmpty()) {
            // We did not find any corresponding key, create a new interval
            map.put(id.reason, new InterruptionInterval(id));
            return;
        }

        double closerDist = Double.MAX_VALUE;
        int closerI = 0;

        for (int i = 0; i < list.size(); i++) {
            InterruptionInterval ii = list.get(i);

            // If we encounter one matching interval, the duration is already
            // considered, we can leave.
            if (ii.isMatching(id)) {
                ii.addDuration(id);
                return;
            }

            // Else, calculate the minimum distance with that interval
            double dist = Math.min(Math.abs(ii.maxDuration - id.fDuration), Math.abs(ii.minDuration - id.fDuration));
            if (dist < closerDist) {
                closerDist = dist;
                closerI = i;
            }
        }

        // If we didn't add the duration, add it to the closest interval
        list.get(closerI).addDuration(id);
    }

    /**
     * Sort a list of durations per duration
     *
     * @param durationList
     *            The list of durations to sort
     * @return The sorted list of durations
     */
    private static List<InterruptionDuration> sortDurationList(List<InterruptionDuration> durationList) {
        List<InterruptionDuration> sortedDurationList = new ArrayList<>(durationList);
        Collections.sort(sortedDurationList, new Comparator<InterruptionDuration>() {
            @Override
            public int compare(InterruptionDuration id1, InterruptionDuration id2) {
                return -Double.valueOf(id1.getDuration()).compareTo(id2.getDuration());
            }
        });
        return sortedDurationList;
    }

    /**
     * Sort a list of intervals per duration, keeping into account the direction
     *
     * @param intervalList
     *            The list of intervals to sort
     * @param direction
     *            The direction to use for the sort
     * @return The sorted list of intervals
     */
    private static List<InterruptionInterval> sortIntervalList(List<InterruptionInterval> intervalList, final Direction direction) {
        List<InterruptionInterval> sortedIntervalList = new ArrayList<>(intervalList);
        Collections.sort(sortedIntervalList, new Comparator<InterruptionInterval>() {
            @Override
            public int compare(InterruptionInterval ii1, InterruptionInterval ii2) {
                int result;
                if (direction == Direction.VARIABLE_OVER_VALUE) {
                    result = -Double.valueOf(ii1.getMinDuration()).compareTo(ii2.getMinDuration());
                } else {
                    result = -Double.valueOf(ii1.getMaxDuration()).compareTo(ii2.getMaxDuration());
                }
                return result;
            }
        });
        return sortedIntervalList;
    }

    private static List<InterruptionDuration> closestDurationList(List<InterruptionDuration> durationList, List<InterruptionInterval> intervalList, final Direction direction) {
        // Organize duration list by decreasing durations
        List<InterruptionDuration> sortedDurationList = sortDurationList(durationList);
        // Organize interval list by decreasing max or min durations depending
        // on direction
        List<InterruptionInterval> sortedIntervalList = sortIntervalList(intervalList, direction);

        // Now we match them in that order
        List<InterruptionDuration> idL = new ArrayList<>();
        for (InterruptionInterval ii : intervalList) {
            idL.add(sortedDurationList.get(sortedIntervalList.indexOf(ii)));
        }

        return idL;
    }

    private static List<InterruptionInterval> closestIntervalList(List<InterruptionDuration> durationList, List<InterruptionInterval> intervalList, final Direction direction) {
        // Organize duration list by decreasing durations
        List<InterruptionDuration> sortedDurationList = sortDurationList(durationList);
        // Organize interval list by decreasing max or min durations depending
        // on direction
        List<InterruptionInterval> sortedIntervalList = sortIntervalList(intervalList, direction);

        // Now we match them in that order
        List<InterruptionInterval> iiL = new ArrayList<>();
        for (InterruptionDuration id : durationList) {
            iiL.add(sortedIntervalList.get(sortedDurationList.indexOf(id)));
        }

        return iiL;
    }

    /*
     * private static DurationIntervalPair
     * closestPermutation(List<InterruptionInterval> intervalList,
     * List<InterruptionDuration> durationList, Boolean direction) { if
     * (durationList.size() == intervalList.size()) { return new
     * DurationIntervalPair(closestDurationList(durationList, intervalList,
     * direction), intervalList); }
     *
     * Iterator<DurationIntervalPair> it;
     *
     * int durationListSize = durationList.size(); int intervalListSize =
     * intervalList.size(); int minSize = Math.min(durationListSize,
     * intervalListSize); if (durationListSize > intervalListSize) { it = new
     * Iterator<DurationIntervalPair>() {
     *
     * @SuppressWarnings("null") private Iterator<List<InterruptionDuration>>
     * uniqueCombinationsIterator = new
     * UniqueCombinationsIterator<>(durationList, intervalListSize);
     *
     * @Override public boolean hasNext() { return
     * uniqueCombinationsIterator.hasNext(); }
     *
     * @Override public DurationIntervalPair next() { return new
     * DurationIntervalPair(closestDurationList(uniqueCombinationsIterator.next(
     * ), intervalList, direction), intervalList); } }; } else { it = new
     * Iterator<DurationIntervalPair>() {
     *
     * @SuppressWarnings("null") private Iterator<List<InterruptionInterval>>
     * uniqueCombinationsIterator = new
     * UniqueCombinationsIterator<>(intervalList, durationListSize);
     *
     * @Override public boolean hasNext() { return
     * uniqueCombinationsIterator.hasNext(); }
     *
     * @Override public DurationIntervalPair next() { return new
     * DurationIntervalPair(durationList, closestIntervalList(durationList,
     * uniqueCombinationsIterator.next(), direction)); } }; }
     *
     * double closestDist = Double.MAX_VALUE; DurationIntervalPair
     * closestDistPair = null;
     *
     * int limit = 1000000; while (it.hasNext() && limit-- > 0) {
     * DurationIntervalPair pair = it.next();
     *
     * double dist = 0; for (int i = 0; i < minSize; i++) { InterruptionInterval
     * intvl = pair.interval.get(i); InterruptionDuration dur =
     * pair.duration.get(i);
     *
     * if (direction == null) { dist += intvl.distance(dur); } else if
     * (direction) { dist += intvl.distanceSup(dur); } else { dist +=
     * intvl.distanceInf(dur); } }
     *
     * List<InterruptionDuration> copy = null; if (closestList.size() <
     * durationList.size()) { copy = new ArrayList<>(durationList);
     * copy.removeAll(closestList); for (InterruptionDuration dur : copy) { dist
     * += dur.getDuration(); } } List<InterruptionInterval> copy = null; if
     * (list.size() < intervalList.size()) { copy = new
     * ArrayList<>(intervalList); copy.removeAll(list); for
     * (InterruptionInterval intvl : copy) { dist += (intvl.getMaxDuration() -
     * intvl.getMinDuration()) / 2.0; } }
     *
     * if (dist < closestDist) { closestDist = dist; closestDistList = new
     * ArrayList<>(closestList); if (copy != null) {
     * closestDistList.addAll(copy); }
     *
     * if (dist == 0) { break; } } }
     *
     * return closestDistPair; }
     */

    private static List<InterruptionDuration> closestPermutationDuration(List<InterruptionInterval> intervalList, List<InterruptionDuration> durationList, Direction direction) {
        if (durationList.size() == intervalList.size()) {
            return closestDurationList(durationList, intervalList, direction);
        }

        int minSize = intervalList.size();
        @SuppressWarnings("null")
        Iterator<List<InterruptionDuration>> uniqueCombinationsIterator = new UniqueCombinationsIteratorSample<>(durationList, minSize);

        double closestDist = Double.MAX_VALUE;
        List<InterruptionDuration> closestDistList = null;

        while (uniqueCombinationsIterator.hasNext()) {
            List<InterruptionDuration> list = uniqueCombinationsIterator.next();
            List<InterruptionDuration> closestList = closestDurationList(list, intervalList, direction);

            double dist = 0;
            for (int i = 0; i < minSize; i++) {
                InterruptionInterval intvl = intervalList.get(i);
                InterruptionDuration dur = closestList.get(i);

                dist += intvl.distance(dur, direction);
            }

            List<InterruptionDuration> copy = null;
            if (closestList.size() < durationList.size()) {
                copy = new ArrayList<>(durationList);
                copy.removeAll(closestList);
                for (InterruptionDuration dur : copy) {
                    dist += dur.getDuration();
                }
            }

            if (dist < closestDist) {
                closestDist = dist;
                closestDistList = new ArrayList<>(closestList);
                if (copy != null) {
                    closestDistList.addAll(copy);
                }

                if (dist == 0) {
                    break;
                }
            }
        }

        return closestDistList;
    }

    private static List<InterruptionInterval> closestPermutationInterval(List<InterruptionInterval> intervalList, List<InterruptionDuration> durationList, Direction direction) {
        if (durationList.size() == intervalList.size()) {
            return closestIntervalList(durationList, intervalList, direction);
        }

        int minSize = durationList.size();
        @SuppressWarnings("null")
        Iterator<List<InterruptionInterval>> uniqueCombinationsIterator = new UniqueCombinationsIteratorSample<>(intervalList, minSize);

        double closestDist = Double.MAX_VALUE;
        List<InterruptionInterval> closestDistList = null;

        while (uniqueCombinationsIterator.hasNext()) {
            List<InterruptionInterval> list = uniqueCombinationsIterator.next();
            List<InterruptionInterval> closestList = closestIntervalList(durationList, list, direction);

            double dist = 0;
            for (int i = 0; i < minSize; i++) {
                InterruptionInterval intvl = closestList.get(i);
                InterruptionDuration dur = durationList.get(i);

                dist += intvl.distance(dur, direction);
            }

            List<InterruptionInterval> copy = null;
            if (list.size() < intervalList.size()) {
                copy = new ArrayList<>(intervalList);
                copy.removeAll(list);
                for (InterruptionInterval intvl : copy) {
                    dist += (intvl.getMaxDuration() - intvl.getMinDuration()) / 2.0;
                }
            }

            if (dist < closestDist) {
                closestDist = dist;
                closestDistList = new ArrayList<>(closestList);
                if (copy != null) {
                    closestDistList.addAll(copy);
                }

                if (dist == 0) {
                    break;
                }
            }
        }

        return closestDistList;
    }

    /**
     * Substract a duration set from this interval set
     *
     * @param idsToSubstract
     *            The duration set to substract from this interval set
     * @param direction
     *            The direction to use for the substraction
     * @return The interval set difference
     */
    public InterruptionIntervalSetDiff substract(InterruptionDurationSet idsToSubstract, Direction direction) {
        InterruptionIntervalSet iisToSubstract = new InterruptionIntervalSet(direction);
        iisToSubstract.addDurationSet(idsToSubstract);
        return substract(iisToSubstract);
    }

    /**
     * Substract a interval set from this interval set
     *
     * @param iisToSubstract
     *            The interval set to substract from this interval set
     * @return The interval set difference
     */
    public InterruptionIntervalSetDiff substract(InterruptionIntervalSet iisToSubstract) {
        InterruptionIntervalSetDiff iisd = new InterruptionIntervalSetDiff();

        // Create a set of all the reasons we need to substract, so we can treat
        // the ones we didn't treat if needed
        Set<InterruptionReason> substractReasons = new HashSet<>(iisToSubstract.map.keySet());

        // For each entry in our local multimap
        for (Entry<InterruptionReason, Collection<InterruptionInterval>> entry : map.asMap().entrySet()) {
            // Check if there is data to substract
            List<InterruptionInterval> substract = new ArrayList<>(iisToSubstract.map.get(entry.getKey()));

            // Get the size of the local list (number of intervals for that
            // reason)
            int thisSize = entry.getValue().size();
            // Compute the sum of minimum and maximum durations of these
            // intervals
            double thisMinTime = 0;
            double thisMaxTime = 0;
            int thisCounter = 0;
            List<TimestampInterval> thisTiList = new LinkedList<>();
            for (InterruptionInterval ii : entry.getValue()) {
                thisMinTime += ii.getMinDuration();
                thisMaxTime += ii.getMaxDuration();
                thisCounter += ii.getCounter();
                thisTiList.addAll(ii.getTsIntervals());
            }

            // If there is nothing to substract, just add the previously
            // computed durations as the diff
            if (substract.isEmpty()) {
                if (fDirection == Direction.VARIABLE_EQ_VALUE || fDirection == Direction.VARIABLE_UNDER_VALUE) {
                    // Create the difference interval that we'll populate with
                    // the time data
                    InterruptionInterval iiDiff = new InterruptionInterval(entry.getKey());

                    iiDiff.setMinDuration(thisMinTime);
                    iiDiff.setMaxDuration(thisMaxTime);
                    iiDiff.setCounter(thisCounter);
                    iiDiff.addTsInterval(thisTiList);
                    iisd.add(iiDiff, entry.getValue().size());
                }
                continue;
            }
            // ... and remove that reason from the set of all the reasons we
            // need to substract
            substractReasons.remove(entry.getKey());

            // Then get the size of the list to substract (number of intervals
            // for that reason)
            int subsSize = substract.size();
            // Compute the sum of minimum and maximum durations of these
            // intervals
            double subsMinTime = 0;
            double subsMaxTime = 0;
            int subsCounter = 0;
            for (InterruptionInterval ii : substract) {
                subsMinTime += ii.getMinDuration();
                subsMaxTime += ii.getMaxDuration();
                subsCounter += ii.getCounter();
            }

            // Finally, compute the size, minimum and maximum differences
            int sizeCalc = thisSize - subsSize;
            double minCalc = thisMinTime - subsMinTime;
            double maxCalc = thisMaxTime - subsMaxTime;
            int counterCalc = thisCounter - subsCounter;
            if (fDirection == Direction.VARIABLE_UNDER_VALUE) {
                sizeCalc = Math.max(sizeCalc, 0);
                minCalc = Math.max(minCalc, 0);
                maxCalc = Math.max(maxCalc, 0);
                counterCalc = Math.max(counterCalc, 0);
            } else if (fDirection == Direction.VARIABLE_OVER_VALUE) {
                sizeCalc = Math.max(-sizeCalc, 0);
                minCalc = Math.max(-minCalc, 0);
                maxCalc = Math.max(-maxCalc, 0);
                counterCalc = Math.max(-counterCalc, 0);
            }

            // Only create an interval if one of those information is different
            // than 0
            if (sizeCalc != 0 || minCalc != 0 || maxCalc != 0) {
                // Create the difference interval that we'll populate with the
                // time data
                InterruptionInterval iiDiff = new InterruptionInterval(entry.getKey());

                iiDiff.setMinDuration(Math.min(minCalc, maxCalc));
                iiDiff.setMaxDuration(Math.max(minCalc, maxCalc));
                iiDiff.setCounter(counterCalc);
                iiDiff.addTsInterval(thisTiList);

                iisd.add(iiDiff, sizeCalc);
            }
        }

        if (fDirection == Direction.VARIABLE_EQ_VALUE || fDirection == Direction.VARIABLE_OVER_VALUE) {
            // For each entry left in the set of all the reasons we need to
            // substract
            for (InterruptionReason ir : substractReasons) {
                List<InterruptionInterval> substract = new ArrayList<>(iisToSubstract.map.get(ir));

                int subsSize = -substract.size();
                double subsMinTime = 0;
                double subsMaxTime = 0;
                int subsCounter = 0;
                List<TimestampInterval> subsTiList = new LinkedList<>();
                for (InterruptionInterval ii : substract) {
                    subsMinTime -= ii.getMinDuration();
                    subsMaxTime -= ii.getMaxDuration();
                    subsCounter -= ii.getCounter();
                    subsTiList.addAll(ii.getTsIntervals());
                }

                InterruptionInterval iiDiff = new InterruptionInterval(ir);
                iiDiff.setMinDuration(subsMaxTime);
                iiDiff.setMaxDuration(subsMinTime);
                iiDiff.setCounter(subsCounter);
                iiDiff.addTsInterval(subsTiList);

                iisd.add(iiDiff, subsSize);
            }
        }

        return iisd;
    }

    /**
     * Add a duration set to that interval set, by matching each element to the
     * closest.
     *
     * @param ids
     *            The duration set to add
     */
    public void addDurationSet(InterruptionDurationSet ids) {
        /*
         * Increment the number of duration set we put in that interval, will
         * help to weight it for the number of occurrences
         */
        counter++;

        /*
         * For each interruption reason, treat the list of interruption duration
         * to match it in the list of interval for that reason in our interval
         * set
         */
        for (Entry<InterruptionReason, Collection<InterruptionDuration>> entry : ids.entrySetList()) {
            List<InterruptionInterval> intList = new ArrayList<>(map.get(entry.getKey()));

            /*
             * If no interval correspond to the current reason, create a new
             * interval in the multimap for that reason, with that duration.
             */
            if (intList.isEmpty()) {
                for (InterruptionDuration id : entry.getValue()) {
                    map.put(entry.getKey(), new InterruptionInterval(id));
                }
                continue;
            }

            List<InterruptionInterval> cIiList;
            List<InterruptionDuration> cIdList;
            List<InterruptionDuration> entryAsList = new ArrayList<>(entry.getValue());
            if (intList.size() > entry.getValue().size()) {
                // If the list of intervals is bigger, it's the one we want to
                // permute
                cIiList = closestPermutationInterval(intList, entryAsList, fDirection);
                cIdList = entryAsList;
            } else {
                cIiList = intList;
                cIdList = closestPermutationDuration(intList, entryAsList, fDirection);
            }

            // If cIdList is null, there was a problem
            if (cIdList == null) {
                throw new RuntimeException("cIdList is null and shouldn't be!"); //$NON-NLS-1$
            }
            // If cIiList is null, there was a problem
            if (cIiList == null) {
                throw new RuntimeException("cIiList is null and shouldn't be!"); //$NON-NLS-1$
            }

            // Add the duration to all existing interval
            int minSize = Math.min(cIiList.size(), cIdList.size());
            for (int i = 0; i < minSize; i++) {
                cIiList.get(i).addDuration(cIdList.get(i));
            }

            // Add the duration that did not fit in the intervals
            for (int i = minSize; i < cIdList.size(); i++) {
                map.put(entry.getKey(), new InterruptionInterval(cIdList.get(i)));
            }
        }
    }

    /**
     * Verify if a duration set fits in the current interval set. For a duration
     * set to fit, it needs to be composed of the exact same interruption
     * reasons, for the exact same number of times.
     *
     * @param ids
     *            The duration set to verify
     * @return true if it fits, false else
     */
    public boolean fits(InterruptionDurationSet ids) {
        Map<InterruptionReason, Integer> iikeymap = StateMachineVariableHelpers.keyMap(map);
        Map<InterruptionReason, Integer> idkeymap = StateMachineVariableHelpers.keyMap(ids.map);

        if (idkeymap.keySet().size() != iikeymap.keySet().size()) {
            return false;
        }

        for (Entry<InterruptionReason, Integer> entry : idkeymap.entrySet()) {
            Integer i = iikeymap.remove(entry.getKey());
            if (i == null || i != entry.getValue()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compute the distance between a duration set and the interval set.
     *
     * @param ids
     *            The duration set
     * @return The distance
     */
    public InterruptionDurationSet distance(InterruptionDurationSet ids) {
        InterruptionDurationSet diff = new InterruptionDurationSet();
        HashMultimap<InterruptionReason, InterruptionInterval> copy = HashMultimap.create(map);

        // For all the data that are in the duration set
        for (Entry<InterruptionReason, Collection<InterruptionDuration>> entry : ids.map.asMap().entrySet()) {
            List<InterruptionInterval> intList = new ArrayList<>(copy.removeAll(entry.getKey()));
            if (intList.isEmpty()) {
                if (fDirection == Direction.VARIABLE_UNDER_VALUE || fDirection == Direction.VARIABLE_EQ_VALUE) {
                    for (InterruptionDuration id : entry.getValue()) {
                        diff.add(new InterruptionDuration(id.getReason(), id.getDuration()));
                    }
                }
                continue;
            }

            List<InterruptionInterval> cIiList;
            List<InterruptionDuration> cIdList;
            List<InterruptionDuration> entryAsList = new ArrayList<>(entry.getValue());
            if (intList.size() > entry.getValue().size()) {
                // If the list of intervals is bigger, it's the one we want to
                // permute
                cIiList = closestPermutationInterval(intList, entryAsList, fDirection);
                cIdList = entryAsList;
            } else {
                cIiList = intList;
                cIdList = closestPermutationDuration(intList, entryAsList, fDirection);
            }

            // If cIdList is null, there was a problem
            if (cIdList == null) {
                throw new RuntimeException("cIdList is null and shouldn't be!"); //$NON-NLS-1$
            }
            // If cIiList is null, there was a problem
            if (cIiList == null) {
                throw new RuntimeException("cIiList is null and shouldn't be!"); //$NON-NLS-1$
            }

            // Add the differences for those who do not fit in the interval
            int minSize = Math.min(cIiList.size(), cIdList.size());
            for (int i = 0; i < minSize; i++) {
                InterruptionDuration id = cIdList.get(i);
                double dist = cIiList.get(i).distance(id, fDirection);

                if (dist > 0) {
                    diff.add(new InterruptionDuration(id.getReason(), dist));
                }
            }

            // Add the differences for the durations who entirely did not fit
            if (fDirection == Direction.VARIABLE_UNDER_VALUE || fDirection == Direction.VARIABLE_EQ_VALUE) {
                for (int i = minSize; i < cIdList.size(); i++) {
                    InterruptionDuration id = cIdList.get(i);
                    diff.add(new InterruptionDuration(id.getReason(), id.getDuration()));
                }
            }

            // Add the differences for the intervals that weren't used
            if (fDirection == Direction.VARIABLE_OVER_VALUE || fDirection == Direction.VARIABLE_EQ_VALUE) {
                for (int i = minSize; i < cIiList.size(); i++) {
                    InterruptionInterval ii = cIiList.get(i);
                    diff.add(new InterruptionDuration(ii.getReason(), ii.getMinDuration()));
                }
            }
        }

        // For all the intervals that had reasons not shared with the duration
        // set
        for (Entry<InterruptionReason, InterruptionInterval> entry : copy.entries()) {
            diff.add(new InterruptionDuration(entry.getKey(), entry.getValue().getMinDuration()));
        }

        return diff;
    }

    /**
     * Verify whether a duration matches in one of the interval of this set
     *
     * @param id
     *            The duration to check
     * @return Whether or not the duration matches in one of the interval of
     *         this set
     */
    public boolean matches(InterruptionDuration id) {
        List<InterruptionInterval> list = new ArrayList<>(map.get(id.reason));

        if (list.isEmpty()) {
            return false;
        }

        for (InterruptionInterval ii : list) {
            if (ii.isMatching(id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Remove an interval of this set that matches a duration
     *
     * @param id
     *            The duration to check
     * @return Whether or not the duration matched in one of the interval of
     *         this set
     */
    public boolean removeMatching(InterruptionDuration id) {
        List<InterruptionInterval> list = new ArrayList<>(map.get(id.reason));

        if (list.isEmpty()) {
            return false;
        }

        for (int i = 0; i < list.size(); i++) {
            InterruptionInterval ii = list.get(i);
            if (ii.isMatching(id)) {
                list.remove(i);
                return true;
            }
        }

        return false;
    }

    @Override
    public InterruptionIntervalSet clone() {
        InterruptionIntervalSet iis = new InterruptionIntervalSet();
        iis.map.putAll(map);
        iis.counter = this.counter;
        iis.fDirection = this.fDirection;

        return iis;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((map == null) ? 0 : map.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        InterruptionIntervalSet other = (InterruptionIntervalSet) obj;
        if (map == null) {
            if (other.map != null) {
                return false;
            }
        } else if (!map.equals(other.map)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String str = "IntervalSet(" + counter + "): [\n"; //$NON-NLS-1$ //$NON-NLS-2$
        for (Entry<InterruptionReason, Collection<InterruptionInterval>> entry : map.asMap().entrySet()) {
            double minTime = 0;
            double maxTime = 0;
            for (InterruptionInterval ii : entry.getValue()) {
                minTime += ii.getMinDuration();
                maxTime += ii.getMaxDuration();
            }

            /*
             * double minTime = Collections.min(entry.getValue(), new
             * Comparator<InterruptionInterval>() {
             *
             * @Override public int compare(InterruptionInterval ii1,
             * InterruptionInterval ii2) { return
             * Double.valueOf(ii1.getMinDuration()).compareTo(ii2.getMinDuration
             * ()); } }).getMinDuration(); double maxTime =
             * Collections.max(entry.getValue(), new
             * Comparator<InterruptionInterval>() {
             *
             * @Override public int compare(InterruptionInterval ii1,
             * InterruptionInterval ii2) { return
             * Double.valueOf(ii1.getMaxDuration()).compareTo(ii2.getMaxDuration
             * ()); } }).getMinDuration();
             */

            str += "    [" + entry.getKey() + ", " + entry.getValue().size() + ", [" + minTime + ", " + maxTime + "]]\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        }
        str += "]"; //$NON-NLS-1$
        return str;
    }

    /**
     * @return A one-line string representation of this interval set
     */
    public String toStringOneLine() {
        boolean first = true;
        String str = "["; //$NON-NLS-1$
        for (Entry<InterruptionReason, Collection<InterruptionInterval>> entry : map.asMap().entrySet()) {
            double minTime = 0;
            double maxTime = 0;
            for (InterruptionInterval ii : entry.getValue()) {
                minTime += ii.getMinDuration();
                maxTime += ii.getMaxDuration();
            }

            if (first) {
                first = false;
            } else {
                str += ", "; //$NON-NLS-1$
            }
            str += "[" + entry.getKey() + ", " + entry.getValue().size() + ", [" + minTime + ", " + maxTime + "]]";//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        }
        str += "]"; //$NON-NLS-1$
        return str;
    }

    /**
     * @return The keymap of this interval set
     */
    public Map<InterruptionReason, Integer> keyMap() {
        return StateMachineVariableHelpers.keyMap(map);
    }

    /**
     * @return A collection of all the intervals in this set
     */
    public Collection<InterruptionInterval> getIntervals() {
        return map.values();
    }

    /**
     * @return A collection of all the intervals in this set, merged per reason
     */
    public Collection<InterruptionInterval> getMergedIntervals() {
        Collection<InterruptionInterval> iiCollection = new ArrayList<>(map.asMap().entrySet().size());
        for (Entry<InterruptionReason, Collection<InterruptionInterval>> entry : map.asMap().entrySet()) {
            InterruptionInterval newii = new InterruptionInterval(entry.getKey());

            double minTime = 0;
            double maxTime = 0;
            for (InterruptionInterval ii : entry.getValue()) {
                minTime += ii.getMinDuration();
                maxTime += ii.getMaxDuration();
                newii.addTsInterval(ii.getTsIntervals());
            }

            newii.setMinDuration(minTime);
            newii.setMaxDuration(maxTime);
            newii.setCounter(entry.getValue().size());

            iiCollection.add(newii);
        }
        return iiCollection;
    }
}