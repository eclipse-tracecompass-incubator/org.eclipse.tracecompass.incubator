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
class UniqueCombinationsIterator<@Nullable T> implements Iterator<List<T>> {
    final private List<T> list;
    final private Iterator<int[]> iterator;
    final private int combinationSize;

    private static final double THRESHOLD = 1e6;

    public UniqueCombinationsIterator(List<T> list, int combinationSize) {
        this.list = list;
        //this.combinationSize = Math.min(combinationSize, list.size());
        this.combinationSize = combinationSize;
        if (CombinatoricsUtils.binomialCoefficientDouble(list.size(), combinationSize) <= THRESHOLD) {
            iterator = new Combinations(list.size(), this.combinationSize).iterator();
        } else {
            iterator = new RandomCombinationsIterator(list.size(), this.combinationSize, (int) THRESHOLD);
        }
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