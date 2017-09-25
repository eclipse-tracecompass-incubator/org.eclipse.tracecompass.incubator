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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.eclipse.jdt.annotation.Nullable;

/**
*
* @author Raphaël Beamonte
*
* @param <T>
*/
class UniqueCombinationsIteratorSample<@Nullable T> implements Iterator<List<T>> {
    final private List<T> list;
    final private Iterator<int[]> iterator;
    final private int combinationSize;

    public UniqueCombinationsIteratorSample(List<T> list, int combinationSize) {
        this.list = list;
        this.combinationSize = combinationSize;

        double population = CombinatoricsUtils.binomialCoefficientDouble(list.size(), combinationSize);
        double sample = sampleSize(population);
        //StateMachineReport.debug(String.format("C(N:%d, K:%d) = POPULATION = %d / POPULATION/2 = %.2f / SAMPLE = %d", list.size(), combinationSize, (int) population, population / 2, (int) sample)); //$NON-NLS-1$

        if (sample < population / 2) {
            iterator = new RandomCombinationsIteratorUnique(list.size(), this.combinationSize, (int) sample);
        } else {
            iterator = new Combinations(list.size(), this.combinationSize).iterator();
        }
    }

    private static double sampleSize(Double population) {
        double z = 1.96;    // z-score for confidence 95%
        double p = 0.5;     // Distribution = 50%
        double e = 0.05;    // Error margin = 5%
        //double z = 2.576; // z-score for confidence 99%
        //double e = 0.03;  // Error margin = 3%
        //double e = 125e-5;// Error margin = 0.125%
        //double e = 5e-3;  // Error margin = 0.5%

        return StateMachineVariableHelpers.sampleSize(population, z, p, e);
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public List<T> next() {
        if (!iterator.hasNext()) {
            return null;
        }

        List<T> combination = new ArrayList<>(combinationSize);
        for (int i : iterator.next()) {
            combination.add(list.get(i));
        }
        return combination;
    }
}