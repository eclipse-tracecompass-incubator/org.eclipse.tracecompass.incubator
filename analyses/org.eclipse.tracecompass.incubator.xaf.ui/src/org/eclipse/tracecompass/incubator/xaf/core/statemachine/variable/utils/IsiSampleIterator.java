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
import java.util.Iterator;
import java.util.Random;

import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance.InstanceStepInformation;

/**
 * Iterator that allows to automatically sample and return the right number of
 * instance step information to analyze.
 *
 * @author Raphaël Beamonte
 */
public class IsiSampleIterator implements Iterator<InstanceStepInformation> {

    private final Iterator<InstanceStepInformation> iterator;
    private final Collection<InstanceStepInformation> collection;
    private final int size;
    private int count;

    /**
     * Whether to use the full population of elements or to sample it
     */
    public final boolean useFullPopulation;

    /**
     * Constructor
     * @param collection The full collection of instance step information
     */
    public IsiSampleIterator(Collection<InstanceStepInformation> collection) {
        this.collection = collection;
        double population = collection.size();
        double sample = sampleSize(population);

        if (sample < population) {
            useFullPopulation = false;
            size = (int)sample;
            count = size;
            iterator = new Iterator<InstanceStepInformation>() {
                private Random rand = new Random();
                private ArrayList<InstanceStepInformation> isiList = new ArrayList<>(collection);

                @Override
                public boolean hasNext() {
                    return (count > 0);
                }

                @Override
                public InstanceStepInformation next() {
                    int select = rand.nextInt(isiList.size());
                    return isiList.remove(select);
                }
            };
        } else {
            size = collection.size();
            count = size;
            iterator = collection.iterator();
            useFullPopulation = true;
        }
    }

    private static double sampleSize(Double population) {
        double z = 1.96;    // z-score for confidence 95%
        double p = 0.5;     // Distribution = 50%
        double e = 0.05;    // Error margin = 5%

        return StateMachineVariableHelpers.sampleSize(population, z, p, e);
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public InstanceStepInformation next() {
        count--;
        return iterator.next();
    }

    /**
     * @return The number of elements that will be returned by the iterator
     */
    public int size() {
        return size;
    }

    /**
     * @return The number of elements that still have to be returned by the iterator
     */
    public int count() {
        return count;
    }

    /**
     * Ask for one more element from the iterator, if available
     */
    public void inc() {
        if (collection.size() > count) {
            count++;
        }
    }

}