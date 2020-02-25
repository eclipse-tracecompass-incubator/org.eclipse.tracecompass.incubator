/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.variable.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.base.Joiner;

/**
 * Special object that allows to compute relative responsibilities according to
 * the elements put inside.
 *
 * @author Raphaël Beamonte
 *
 * @param <K> The type of object to compute the responsibility for
 */
public class ResponsibilityMap<K extends Comparable<K>> extends TreeMap<K, Double> {
    private static final long serialVersionUID = 1L;
    private int fWeight = 1;

    /**
     * Constructor
     */
    public ResponsibilityMap() {}

    /**
     * Constructor
     *
     * @param comparator The comparator to use in order to compare the objects
     */
    public ResponsibilityMap(Comparator<? super K> comparator) {
        super(comparator);
    }

    /**
     * Constructor
     *
     * @param initialWeight The initial weight to use for the responsibility map
     *                      (i.e. 0 means no value before any insertion)
     */
    public ResponsibilityMap(int initialWeight) {
        fWeight = initialWeight;
    }

    /**
     * Constructor
     *
     * @param comparator The comparator to use in order to compare the objects
     * @param initialWeight The initial weight to use for the responsibility map
     *                      (i.e. 0 means no value before any insertion)
     */
    public ResponsibilityMap(int initialWeight, Comparator<? super K> comparator) {
        super(comparator);
        fWeight = initialWeight;
    }

    /**
     * Add a value in the responsibility map for a given key
     *
     * @param key The key that will receive that added value
     * @param value The added value
     * @param aomk An object specifying special actions to be executed on the key
     *             if it already exists
     */
    public void merge(K key, Double value, ActionOnMapKey<K> aomk) {
        K key1 = key;
        Double currentValue = get(key);
        if (currentValue == null) {
            currentValue = 0.0;
        } else if (aomk != null) {
            K key2 = key;
            for (K k : keySet()) {
                if (Objects.equals(k, key)) {
                    key2 = k;
                    break;
                }
            }
            if (key2 != key) {
                remove(key);
                key1 = aomk.execute(key, key2);
            }
        }
        put(key1, ((currentValue * fWeight) + value) / (fWeight + 1));
    }

    /**
     * Merge a full responsibility map into this one
     *
     * @param rm The responsibility map to merge
     */
    public void mergeAll(ResponsibilityMap<K> rm) {
        mergeAll(rm, null);
    }

    /**
     * Merge a full responsibility map into this one
     *
     * @param rm The responsibility map to merge
     * @param aomk An object specifying special actions to be executed on the keys
     *             if they already exist
     */
    public void mergeAll(ResponsibilityMap<K> rm, ActionOnMapKey<K> aomk) {
        ResponsibilityMap<K> remote = rm.clone(comparator(), aomk);
        TreeMap<K, Double> newMap = new TreeMap<>();

        for (Map.Entry<K, Double> entry : entrySet()) {
            K key = entry.getKey();
            Double addValue = .0;
            if (remote.containsKey(entry.getKey())) {
                if (aomk != null) {
                    K key2 = remote.ceilingKey(key);
                    if (key2 != key) {
                        key = aomk.execute(key, key2);
                    }
                }
                addValue = Objects.requireNonNull(remote.remove(entry.getKey()));
            }
            /*StateMachineReport.debug("  WORKING ON KEY " + entry.getKey() + " WHICH IS NOW " + key);
            StateMachineReport.debug("  PREVIOUS PROBA = " + entry.getValue() + " (W=" + weight + ")");
            StateMachineReport.debug("  ADDED PROBA = " + addValue + " (W=" + remote.weight + ")");*/
            newMap.put(key, ((entry.getValue() * fWeight) + (addValue * remote.fWeight)) / (fWeight + remote.fWeight));
        }

        for (Map.Entry<K, Double> entry : remote.entrySet()) {
            /*StateMachineReport.debug("  ADDING NEW KEY " + entry.getKey());
            StateMachineReport.debug("  NO PREVIOUS PROBA (W=" + weight + ")");
            StateMachineReport.debug("  ADDED PROBA = " + entry.getValue() + " (W=" + remote.weight + ")");*/
            newMap.put(entry.getKey(), (entry.getValue() * remote.fWeight) / (fWeight + remote.fWeight));
        }

        fWeight += remote.fWeight;
        clear();
        putAll(newMap);
        //StateMachineReport.debug("Now this ? " + this + "\n");
    }

    @Override
    public ResponsibilityMap<K> clone() {
        ResponsibilityMap<K> rm = new ResponsibilityMap<K>(comparator());
        rm.putAll(this);
        rm.fWeight = fWeight;

        return rm;
    }

    /**
     * Clone the current responsibility map
     *
     * @param comparator The comparator to be used
     * @return A clone of the current responsibility map
     */
    public ResponsibilityMap<K> clone(Comparator<? super K> comparator) {
        return clone(comparator, null);
    }


    /**
     * Clone the current responsibility map
     *
     * @param comparator The comparator to be used
     * @param aomk An object specifying special actions to be executed on the keys
     *             if they already exist
     * @return A clone of the current responsibility map
     */
    public ResponsibilityMap<K> clone(Comparator<? super K> comparator, ActionOnMapKey<K> aomk) {
        if ((comparator == null && comparator() == null)
                || (comparator() != null && comparator().equals(comparator))) {
            return clone();
        }

        ResponsibilityMap<K> rm = new ResponsibilityMap<K>(comparator);
        for (Map.Entry<K, Double> entry : entrySet()) {
            K key = entry.getKey();
            Double currentValue = rm.get(entry.getKey());
            if (currentValue == null) {
                currentValue = .0;
            } else if (aomk != null) {
                K key2 = key;
                for (K k : rm.keySet()) {
                    if ((comparator() != null && comparator().compare(key, k) == 0)
                            || (comparator() == null && Objects.requireNonNull(key).compareTo(k) == 0)) {
                        key2 = k;
                        break;
                    }
                }
                if (key2 != key) {
                    rm.remove(key);
                    key = aomk.execute(key, key2);
                }
            }
            rm.put(key, entry.getValue() + currentValue);
        }
        rm.fWeight = fWeight;

        return rm;
    }

    /**
     * Change the weight of the responsibility map
     *
     * @param newWeight The new weight
     */
    public void setWeight(int newWeight) {
        fWeight = newWeight;
    }

    /**
     * @return The weight of the responsibility map
     */
    public int getWeight() {
        return fWeight;
    }

    /**
     * @return A collection, ordered by decreasing responsibility, of the elements of the
     *         responsibility map that have a computed responsibility of at least MAX-STDEV,
     *         where MAX is the responsibility of the element with the maximum responsibility,
     *         and STDEV is the standard deviation on the elements' responsibility.
     */
    public Collection<Map.Entry<K, Double>> getTopStdDevCollection() {
        Set<Map.Entry<K, Double>> entries = new TreeSet<>(new ResponsibilityMap.EntryComparator<K>());
        entries.addAll(this.entrySet());

        if (!entries.isEmpty()) {
            double substract = getStdDev();
            if (this.size() == 2) {
                substract = Math.min(substract * 2, 0.1);
            }
            double minProba = Math.max(entries.iterator().next().getValue() - substract, 0);
            entries.removeIf(entry -> entry.getValue() < minProba);
        }

        return entries;
    }

    /**
     * @return A collection, ordered by decreasing responsibility, of the elements of the
     *         responsibility map
     */
    public Collection<Map.Entry<K, Double>> getCollection() {
        Set<Map.Entry<K, Double>> entries = new TreeSet<>(new ResponsibilityMap.EntryComparator<K>());
        entries.addAll(this.entrySet());

        return entries;
    }

    /**
     * @return The sum of the responsabilities of all the elements of the responsibility map
     */
    public double getSum() {
        double sum = 0;
        for (Map.Entry<K, Double> entry : entrySet()) {
            sum += entry.getValue();
        }
        return sum;
    }

    /**
     * @return The mean responsibility of the responsibility map
     */
    public double getMean() {
        return getSum() / this.size();
    }

    /**
     * @return The variance of the responsibility of the responsibility map
     */
    public double getVariance() {
        double mean = getMean();
        double variance = 0;
        for (Double d : this.values()) {
            variance += Math.pow(d - mean, 2);
        }
        variance /= this.size();
        return variance;
    }

    /**
     * @return The standard deviation of the responsibility of the responsibility map
     */
    public double getStdDev() {
        return Math.sqrt(getVariance());
    }

    /**
     * @return The minimum probability to consider, computed by MAX-STDEV, where MAX
     *         is the responsibility of the element with the maximum responsibility,
     *         and STDEV is the standard deviation on the elements' responsibility.
     */
    public double getMinProba() {
        if (this.size() > 0) {
            double substract = getStdDev();
            if (this.size() == 2) {
                /* For specific cases with only two elements, we want to
                 * manage the case where they are close... We chose here
                 * to consider closeness up to 10%, not more.
                 */
                substract = Math.min(substract * 2, 0.1);
            }
            return Math.max(getCollection().iterator().next().getValue() - substract, 0);
        }
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fWeight, super.hashCode());
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other.getClass().equals(ResponsibilityMap.class)) {
            return fWeight == ((ResponsibilityMap<?>) other).fWeight && super.equals(other);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("[W=%d][S=%f]%s", fWeight, getSum(), super.toString()); //$NON-NLS-1$
    }

    /**
     * @return A pretty string representing the responsibility map
     */
    public String toStringPretty() {
        List<String> mapContent = new ArrayList<>();
        for (Map.Entry<K, Double> entry : getCollection()) {
            mapContent.add(String.format("%s, responsibility of %.2f%%", //$NON-NLS-1$
                    entry.getKey(),
                    entry.getValue() * 100
                    ));
        }

        return String.format("Responsibility[W=%d][S=%f][%n\t%s%n]", fWeight, getSum(), Joiner.on("%n\t").join(mapContent)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * A comparator for the entries of the responsibility map that allows to
     * compare by value and not by key
     *
     * @author Raphaël Beamonte
     *
     * @param <K> The type of elements stored in the entry
     */
    public static class EntryComparator<K extends Comparable<K>> implements Comparator<Map.Entry<K, Double>> {
        @Override
        public int compare(Map.Entry<K, Double> entry1, Map.Entry<K, Double> entry2) {
            int cmp = entry2.getValue().compareTo(entry1.getValue());
            if (cmp == 0) {
                if (entry1.getKey() == null) {
                    if (entry2.getKey() == null) {
                        return 0;
                    }
                    return 1;
                }
                return Objects.requireNonNull(entry1.getKey()).compareTo(entry2.getKey());
            }
            return cmp;
        }
    }

}