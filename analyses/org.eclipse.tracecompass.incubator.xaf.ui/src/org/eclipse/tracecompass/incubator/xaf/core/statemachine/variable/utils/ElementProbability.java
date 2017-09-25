/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

/**
 * Representation of the probability of a given element considering its computed
 * weight, its number of occurrences, and its distance.
 *
 * @author Raphaël Beamonte
 *
 * @param <T>
 *            The type of the element to which we assign a probability
 */
@SuppressWarnings("rawtypes")
public class ElementProbability<T> implements Comparable<ElementProbability> {
    /** The element */
    public final T element;
    /** The number of occurrences of that element */
    public final int occurrences;
    /** The distance of that element */
    public final int distance;
    /** The computed weight of that element */
    public final double probability;

    /**
     * @param element
     *            The element
     * @param occurrences
     *            The number of occurrences of that element
     * @param distance
     *            The distance of that element
     * @param probability
     *            The computed weight of that element
     */
    public ElementProbability(T element, int occurrences, int distance, double probability) {
        this.element = element;
        this.occurrences = occurrences;
        this.distance = distance;
        this.probability = probability;
    }

    @Override
    public int compareTo(ElementProbability ehsp) {
        // The more the probability, the more we're happy
        int probcmp = Double.compare(ehsp.probability, probability);
        if (probcmp != 0) {
            return probcmp;
        }

        // The more the distance, the less we're happy
        int distcmp = Integer.compare(distance, ehsp.distance);
        if (distcmp != 0) {
            return distcmp;
        }

        // The more the occurrences, the more we're happy
        return Integer.compare(ehsp.occurrences, occurrences);
    }
}