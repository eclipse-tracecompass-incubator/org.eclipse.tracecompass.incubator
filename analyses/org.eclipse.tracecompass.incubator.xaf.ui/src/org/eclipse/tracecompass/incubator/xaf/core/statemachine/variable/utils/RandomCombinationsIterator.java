/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import java.util.Iterator;
import java.util.Random;

import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineReport;

// Knuth shuffle: https://stackoverflow.com/questions/136474/best-way-to-pick-a-random-subset-from-a-collection
class RandomCombinationsIterator implements Iterator<int[]> {

    private final int n, k;
    private Integer count = null;
    private int[] origin, solution;
    private Random rand = new Random();

    public RandomCombinationsIterator(int n, int k) {
        this.n = n;
        this.k = k;

        init();
    }

    public RandomCombinationsIterator(int n, int k, int max) {
        this.n = n;
        this.k = k;
        this.count = max;

        init();
    }

    private void init() {
        StateMachineReport.debug("Using the random combinations iterator!"); //$NON-NLS-1$

        origin = new int[n];
        for (int i = 0; i < n; i++) {
            origin[i] = i;
        }
        solution = new int[k];
    }

    @Override
    public boolean hasNext() {
        return (count == null || count > 0);
    }

    @Override
    public int[] next() {
        if (count != null) {
            if (count <= 0) {
                return null;
            }
            count--;
        }

        int tmp, j;
        for (int i = 0; i < k; i++) {
            j = i + rand.nextInt(n - i);
            tmp = origin[i];
            origin[i] = origin[j];
            origin[j] = tmp;

            solution[i] = origin[i];
        }
        return solution;
    }

}