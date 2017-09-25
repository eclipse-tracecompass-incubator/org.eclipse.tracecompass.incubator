/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariable;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariableCounter;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;

/**
 * Class that represents an adaptive state machine constraint
 * @author Raphaël Beamonte
 */
public class StateMachineConstraintAdaptive extends StateMachineConstraint {

    private Set<Status> adaptiveConstraintStatusSet = null;
    private Multiset<Double> storedValues = TreeMultiset.create();

    private Operator savedOperator = null;
    private Double savedValue = null;
    private boolean allInstancesValid = false;

    /**
     * Create a new adaptive state machine constraint
     * @param varName The name of the variable on which that constraint applies
     * @param operator The operator used in that constraint
     * @param value The value the variable is compared to
     */
    public StateMachineConstraintAdaptive(String varName, Operator operator, String value) {
        super(varName, operator, ValueType.CONSTANT, value);
    }

    @Override
    public Status verify(Map<String, StateMachineVariable> variables, ITmfEvent event, StateMachineInstance stateMachineInstance) {
        Status cs = new Status(this);
        StateMachineVariable v = variables.get(getVarName());

        if (v != null) {
            Double numericalValue = v.getNumericalValue(super.getValue(), event, stateMachineInstance);
            if (numericalValue != null) {
                storedValues.add(numericalValue);
                savedOperator = null;
                savedValue = null;

                String actualLeftValue = v.getFormattedValue(super.getValue(), event, stateMachineInstance);
                cs.setAdaptive(numericalValue, actualLeftValue);

                if (adaptiveConstraintStatusSet == null) {
                    stateMachineInstance.getStateMachineInstanceGroup().addAdaptiveConstraint(this);
                    adaptiveConstraintStatusSet = new HashSet<>();
                }
                adaptiveConstraintStatusSet.add(cs);
            }
        }

        if (cs.getStatus() == null) {
            cs.setUncertain();
        }

        return cs;
    }

    /**
     * Revalidate the adaptive constraint according to the computed operators and values
     */
    public void revalidate() {
        boolean uncertain = getOperator().equals(Operator.ADAPTIVE) || getAdaptiveValue() == null;

        for (Status cs : adaptiveConstraintStatusSet) {
            if (cs.getStatus() != StatusValue.ADAPTIVE) {
                continue;
            }

            if (uncertain || cs.getAdaptiveValue() == null) {
                cs.setUncertain();
            } else {
                Integer comparison = cs.getAdaptiveValue().compareTo(getAdaptiveValue());
                Object actualLeftValue = cs.getActualLeftValue();

                if (!getOperator().isSatisfied(comparison)) {
                    cs.setInvalid(actualLeftValue, null, null);
                } else {
                    cs.setValid(actualLeftValue, null, null);
                }
            }
        }
    }

    @Override
    public boolean canBeUsed() {
        return !getOperator().isAdaptive() && (!StateMachineUtils.isValueAdaptive(super.getValue()) || getAdaptiveValue() != null);
    }

    @Override
    public Operator getOperator() {
        if (!super.getOperator().isAdaptive()) {
            // The operator is not adaptive, return it directly
            return super.getOperator();
        }

        if (savedOperator != null) {
            // We already computed the operator
            return savedOperator;
        }

        if (storedValues.isEmpty()) {
            // We don't have any value to compute the operator
            return super.getOperator();
        }

        // If the value is not adaptive, set this variable to the numerical value; else, it stays null
        Double notAdaptiveValue = null;
        if (!StateMachineUtils.isValueAdaptive(super.getValue())) {
            if (super.getValue().endsWith("%")) { //$NON-NLS-1$
                notAdaptiveValue = Double.parseDouble(super.getValue().substring(0, super.getValue().length() - 1));
            } else {
                notAdaptiveValue = 1. * StateMachineUtils.strToTimestamp(super.getValue()).getValue();
            }
        }

        if (allInstancesValid) {
            /* If all instances are valid, consider either the equal operator if all
             * the data have the same value, or the less or equal operator as it is the
             * most likely used operator when working on high performance systems. Anyway,
             * if we wanted to use another operator, we would have specified it! (and the
             * user still can change that when auto-generating a model!)
             */
            if (notAdaptiveValue == null) {
                if (storedValues.entrySet().size() > 1) {
                    savedOperator = Operator.LEQ;
                    savedValue = storedValues.entrySet()
                            .stream()
                            .max((v0, v1) -> Double.compare(v0.getElement(), v1.getElement()))
                            .get()
                            .getElement();
                } else {
                    savedOperator = Operator.EQ;
                    savedValue = storedValues.iterator().next();
                }
            } else {
                if (storedValues.entrySet().size() > 1) {
                    double max = storedValues.entrySet()
                            .stream()
                            .max((v0, v1) -> Double.compare(v0.getElement(), v1.getElement()))
                            .get()
                            .getElement();
                    double min = storedValues.entrySet()
                            .stream()
                            .min((v0, v1) -> Double.compare(v0.getElement(), v1.getElement()))
                            .get()
                            .getElement();
                    if (notAdaptiveValue > max) {
                        savedOperator = Operator.LT;
                    } else if (notAdaptiveValue == max) {
                        savedOperator = Operator.LEQ;
                    } else if (notAdaptiveValue < min) {
                        savedOperator = Operator.GT;
                    } else if (notAdaptiveValue == min) {
                        savedOperator = Operator.GEQ;
                    }
                } else if (notAdaptiveValue == storedValues.iterator().next()) {
                    savedOperator = Operator.EQ;
                } else {
                    savedOperator = Operator.NEQ;
                }
            }
            return savedOperator;
        }

        /* Get the value that's most present in the set of data
         */
        Double mostPresentValue = storedValues.entrySet()
                .stream()
                .max((p1, p2) -> Integer.compare(p1.getCount(), p2.getCount()))
                .get()
                .getElement();

        /* If the value is not adaptive, we can adapt the operator to this value and the
         * computed values for the different instances
         */
        if (notAdaptiveValue != null) {
            if (mostPresentValue == notAdaptiveValue) {
                savedOperator = Operator.EQ;
            } else {
                int valuesBeforeValue = 0;
                int valuesAfterValue = 0;

                Iterator<Entry<Double>> it = storedValues.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<Double> entry = it.next();
                    if (entry.getElement() < notAdaptiveValue) {
                        valuesBeforeValue += entry.getCount();
                    } else if (entry.getElement() > notAdaptiveValue) {
                        valuesAfterValue += entry.getCount();
                    }
                }

                if (valuesBeforeValue < valuesAfterValue) {
                    savedOperator = Operator.GEQ;
                }
                savedOperator = Operator.LEQ;
            }
            return savedOperator;
        }

        /* If the number of items with the most present value is at least 80% of the
         * total number of values, chose an equal operator
         */
        if ((storedValues.count(mostPresentValue) / storedValues.size()) >= .8) {
            savedOperator = Operator.EQ;
            savedValue = mostPresentValue;
            return savedOperator;
        }

        // Compute the clusters for the data
        List<List<Double>> clusters = clusterize(storedValues, getSplitValue(), false);

        // Compute the biggest cluster for the data
        int biggestCluster = IntStream
                .range(0, clusters.size())
                .boxed()
                .max((c1, c2) -> Integer.compare(clusters.get(c1).size(), clusters.get(c2).size()))
                .get();

        /*
        // TODO: remove DEBUG
        System.out.println("CLUSTERS ? " + clusters.size());
        for (List<Double> cluster : clusters) {
            System.out.println("CLUSTER of " + cluster.size() + " values between " + Collections.min(cluster) + " and " + Collections.max(cluster));
        }
        // END DEBUG
        */

        while (true) {
            // Compute the number of points before and after the biggest cluster
            int valuesBeforeCluster = 0;
            for (int i = 0; i < biggestCluster; i++) {
                valuesBeforeCluster += clusters.get(i).size();
            }
            int valuesAfterCluster = 0;
            for (int i = biggestCluster + 1; i < clusters.size(); i++) {
                valuesAfterCluster += clusters.get(i).size();
            }

            if (valuesBeforeCluster == 0 && valuesAfterCluster == 0) {
                // FIXME: Well, we'll have to see what to do there! Or not... As if we can't decide, perhaps we shouldn't!
                return Operator.ADAPTIVE;
            }

            int valuesInCluster = clusters.get(biggestCluster).size();
            double clusterPercent = valuesInCluster * 1. / storedValues.size();
            boolean before, after;
            if (clusterPercent >= .75) {
                /* Decide the constraint direction using the number of values if possible
                 */
                before = valuesBeforeCluster == 0 || valuesBeforeCluster > valuesAfterCluster;
                after = valuesAfterCluster == 0 || valuesAfterCluster > valuesBeforeCluster;
            } else {
                /* If there is more than 40% more values on one side, set the constraint
                 * to eliminate the other side
                 */
                before = (valuesAfterCluster > 0) ? valuesBeforeCluster / valuesAfterCluster >= 1.4 : false;
                after = (valuesBeforeCluster > 0) ? valuesAfterCluster / valuesBeforeCluster >= 1.4 : false;
            }
            if (before) {
                savedOperator = Operator.LEQ;
                savedValue = Collections.max(clusters.get(biggestCluster));
                return savedOperator;
            } else if (after) {
                savedOperator = Operator.GEQ;
                savedValue = Collections.min(clusters.get(biggestCluster));
                return savedOperator;
            } else {
                // Add the closest cluster to that cluster, and compute again
                int closestCluster;
                if (biggestCluster == 0) {
                    closestCluster = 1;

                    /* If we were about to merge but we're on an extremity and there is a lot
                     * of space between the clusters...
                     */
                    double space = Collections.min(clusters.get(closestCluster)) - Collections.max(clusters.get(biggestCluster));
                    double biggestClusterSpace = Collections.max(clusters.get(biggestCluster)) - Collections.min(clusters.get(biggestCluster));
                    if (space >= 3 * biggestClusterSpace + 1) {
                        savedOperator = Operator.LEQ;
                        savedValue = Collections.max(clusters.get(biggestCluster));
                        return savedOperator;
                    }
                } else if (biggestCluster == clusters.size() - 1) {
                    closestCluster = biggestCluster - 1;

                    /* If we were about to merge but we're on an extremity and there is a lot
                     * of space between the clusters...
                     */
                    double space = Collections.min(clusters.get(biggestCluster)) - Collections.max(clusters.get(closestCluster));
                    double biggestClusterSpace = Collections.max(clusters.get(biggestCluster)) - Collections.min(clusters.get(biggestCluster));
                    if (space >= 3 * biggestClusterSpace + 1) {
                        savedOperator = Operator.GEQ;
                        savedValue = Collections.min(clusters.get(biggestCluster));
                        return savedOperator;
                    }
                } else {
                    double spaceBefore = Collections.min(clusters.get(biggestCluster)) - Collections.max(clusters.get(biggestCluster - 1));
                    double spaceAfter = Collections.min(clusters.get(biggestCluster + 1)) - Collections.max(clusters.get(biggestCluster));
                    if (Math.min(spaceBefore, spaceAfter) / Math.max(spaceBefore, spaceAfter) >= 0.9) {
                        /* Space is (almost) identical, merge on the side where there is more values,
                         * or before if it's the same
                         */
                        if (valuesBeforeCluster >= valuesAfterCluster) {
                            closestCluster = biggestCluster - 1;
                        } else {
                            closestCluster = biggestCluster + 1;
                        }
                    } else if (spaceBefore < spaceAfter) {
                        closestCluster = biggestCluster - 1;
                    } else {
                        closestCluster = biggestCluster + 1;
                    }
                }

                // Perform the merge operation
                clusters.get(biggestCluster).addAll(clusters.get(closestCluster));
                clusters.remove(closestCluster);
                biggestCluster = Math.min(biggestCluster, closestCluster);
            }
        }
    }

    @Override
    public String getValue() {
        if (!StateMachineUtils.isValueAdaptive(super.getValue())) {
            return super.getValue();
        }

        Double currentAdaptiveValue = getAdaptiveValue();
        if (currentAdaptiveValue == null) {
            return super.getValue();
        }

        if (StateMachineVariableCounter.class.isAssignableFrom(getVarClass())) {
            String valueStr = "%.0f"; //$NON-NLS-1$;
            switch (getOperator()) {
            case GEQ:
            case LT:
                currentAdaptiveValue = Math.ceil(currentAdaptiveValue);
                break;
            case GT:
            case LEQ:
                currentAdaptiveValue = Math.floor(currentAdaptiveValue);
                break;
            case ADAPTIVE:
            case EQ:
            case NEQ:
            default:
                if (currentAdaptiveValue % 1 != 0) {
                    valueStr = "%.4f"; //$NON-NLS-1$;
                }
                break;
            }
            return String.format(valueStr,
                        currentAdaptiveValue);
        }

        String symbol = ""; //$NON-NLS-1$
        if (super.getValue().endsWith("%")) { //$NON-NLS-1$
            symbol = "%"; //$NON-NLS-1$
        } else {
            ITmfTimestamp t = StateMachineUtils.strToTimestamp(super.getValue());
            if (t != null) {
                switch (t.getScale()) {
                case ITmfTimestamp.SECOND_SCALE:
                    symbol = "s"; //$NON-NLS-1$
                    currentAdaptiveValue /= 1.0e9;
                    break;
                case ITmfTimestamp.MILLISECOND_SCALE:
                    symbol = "ms"; //$NON-NLS-1$
                    currentAdaptiveValue /= 1.0e6;
                    break;
                case ITmfTimestamp.MICROSECOND_SCALE:
                    symbol = "us"; //$NON-NLS-1$
                    currentAdaptiveValue /= 1.0e3;
                    break;
                case ITmfTimestamp.NANOSECOND_SCALE:
                    symbol = "ns"; //$NON-NLS-1$
                    break;
                default:
                    break;
                }
            }
        }

        return String.format("%.4f%s", //$NON-NLS-1$;
                currentAdaptiveValue,
                symbol);
    }


    /**
     * @return The adaptive value of the constraint computed from the values stored in the structure
     */
    public Double getAdaptiveValue() {
        if (getOperator() == Operator.NEQ) {
            /* Well, it's not possible to deduce a value "not to be equal to" so...
             * just don't consider this case!
             */
            return null;
        }

        if (getOperator().isAdaptive()) {
            // We couldn't deduce the operator... won't be able to work with the value!
            return null;
        }

        if (savedValue != null) {
            // If we already saved a value
            return savedValue;
        }

        if (!StateMachineUtils.isValueAdaptive(super.getValue())) {
            // If the value was not adaptive... There's no actual work to do here!
            Double val = null;
            if (super.getValue().endsWith("%")) { //$NON-NLS-1$
                val = Double.parseDouble(super.getValue().substring(0, super.getValue().length() - 1));
            } else {
                val = 1. * StateMachineUtils.strToTimestamp(super.getValue()).getValue();
            }
            savedValue = val;
            return val;
        }

        if (storedValues.isEmpty()) {
            return null;
        }


        if (allInstancesValid) {
            /* If all instances are valid, consider only the operator direction to
             * assign the element
             */
            switch (getDirection()) {
            case VARIABLE_OVER_VALUE:
                savedValue = storedValues.entrySet()
                        .stream()
                        .min((v0, v1) -> Double.compare(v0.getElement(), v1.getElement()))
                        .get()
                        .getElement();
                if (!getOperator().hasEqual()) {
                    savedValue--;
                }
                break;
            case VARIABLE_UNDER_VALUE:
                savedValue = storedValues.entrySet()
                        .stream()
                        .max((v0, v1) -> Double.compare(v0.getElement(), v1.getElement()))
                        .get()
                        .getElement();
                if (!getOperator().hasEqual()) {
                    savedValue++;
                }
                break;
            case VARIABLE_EQ_VALUE:
                if (storedValues.entrySet().size() > 1) {
                    /* All instances are valid but here... we have more than one value, and
                     * the operator is equal... which can't be working!
                     */
                    return null;
                }
                savedValue = storedValues.iterator().next();
                break;
            default:
                break;
            }
            return savedValue;
        }

        if (getOperator() == Operator.EQ) {
            return storedValues.entrySet()
                    .stream()
                    .max((p1, p2) -> Integer.compare(p1.getCount(), p2.getCount()))
                    .get()
                    .getElement();
        }

        List<List<Double>> clusters = clusterize(storedValues, getSplitValue(), false);

        int biggestCluster = IntStream
                .range(0, clusters.size())
                .boxed()
                .max((c1, c2) -> Integer.compare(clusters.get(c1).size(), clusters.get(c2).size()))
                .get();

        Double threshold = null;
        switch (getDirection()) {
        case VARIABLE_OVER_VALUE:
            threshold = clusters.get(biggestCluster)
                    .stream()
                    .min((v1, v2) -> Double.compare(v1, v2))
                    .get();
            if (!getOperator().hasEqual()) {
                threshold--;
            }
            break;
        case VARIABLE_UNDER_VALUE:
            threshold = clusters.get(biggestCluster)
                    .stream()
                    .max((v1, v2) -> Double.compare(v1, v2))
                    .get();
            if (!getOperator().hasEqual()) {
                threshold++;
            }
            break;
        case VARIABLE_EQ_VALUE:
        default:
            break;
        }

        savedValue = threshold;
        return threshold;
    }

    private Double getSplitValue() {
        if (StateMachineVariableCounter.class.isAssignableFrom(getVarClass())) {
            // TODO: not clean... we still need clustering here
            return 1.;
        }

        Double split;

        Set<Double> adjacent_diff = new TreeSet<>();
        Iterator<Double> it = storedValues.iterator();
        Double prev = null, next = it.next();
        while (it.hasNext()) {
            prev = next; next = it.next();
            adjacent_diff.add(next - prev);
        }

        List<List<Double>> clustersDiff = clusterize(adjacent_diff, 0.4, true);
        if (clustersDiff.size() > 1) {
            split = (Collections.max(clustersDiff.get(clustersDiff.size() - 2)) + Collections.min(clustersDiff.get(clustersDiff.size() - 1))) / 2.0;
        } else {
            split = clustersDiff.get(0)
                    .stream()
                    .mapToDouble(a -> a)
                    .average()
                    .getAsDouble();
        }

        return split;
    }

    private static List<List<Double>> clusterize(Collection<Double> data, double threshold, boolean isThresholdPercent) {
        List<List<Double>> clusters = new ArrayList<>();
        if (data.isEmpty()) {
            return clusters;
        }

        int current_cluster = 0;
        clusters.add(new ArrayList<>());

        Iterator<Double> it = data.iterator();
        Double prev = null,
               next = it.next(),
               diff = null;

        while (it.hasNext()) {
            prev = next;
            next = it.next();

            clusters.get(current_cluster).add(prev);

            diff = (next - prev);
            if (isThresholdPercent) {
                diff /= prev;
            }
            if (diff >= threshold) {
                current_cluster++;
                clusters.add(new ArrayList<>());
            }
        }

        clusters.get(current_cluster).add(next);
        return clusters;
    }

    /**
     * Set the parameter for this constraint to consider all the instances as valid or not
     * @param allInstancesValid Whether or not to consider all the instances as valid
     */
    public void setAllInstancesValid(boolean allInstancesValid) {
        if (allInstancesValid != this.allInstancesValid) {
            this.allInstancesValid = allInstancesValid;
            savedOperator = null;
            savedValue = null;
        }
    }

}
