/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineReport;

class RandomCombinationsIteratorUnique implements Iterator<int[]> {

    private final int n, k;
    private Integer count = null;
    private int[] origin, solution;
    private Random rand = new Random();
    private Set<Integer> used = new TreeSet<>();

    public RandomCombinationsIteratorUnique(int n, int k) {
        this.n = n;
        this.k = k;

        init();
    }

    public RandomCombinationsIteratorUnique(int n, int k, int max) {
        this.n = n;
        this.k = k;
        this.count = max;

        init();
    }

    private void init() {
        StateMachineReport.debug("Using the random combinations iterator unique!"); //$NON-NLS-1$

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

        int test = 0;
        do {
            if (test > 0) {
                StateMachineReport.debug("REMATCH " + test + " with " + Arrays.toString(solution)); //$NON-NLS-1$ //$NON-NLS-2$
            }
            test++;

            int tmp, j;
            // Knuth shuffle
            /*if (k <= n/2) {
                for (int i = 0; i < k; i++) {
                    j = i + rand.nextInt(n - i);
                    solution[i] = origin[j];
                    origin[j] = origin[i];
                    origin[i] = solution[i];
                }
            } else {
                for (int i = k; i < n; i++) {
                    j = rand.nextInt(k);
                    tmp = origin[i];
                    origin[i] = origin[j];
                    origin[j] = tmp;
                }
                System.arraycopy(origin, 0, solution, 0, solution.length);
            }*/
            // Knuth shuffle: order doesn't matter, we only roll the dices for the elements that are OUTSIDE
            // https://en.wikipedia.org/wiki/Reservoir_sampling
            /*for (int i = k; i < n; i++) {
                j = rand.nextInt(i + 1);
                tmp = origin[i];
                origin[i] = origin[j];
                origin[j] = tmp;
            }
            System.arraycopy(origin, 0, solution, 0, solution.length);*/
            // Basic reservoir
            /*for (int i = 0; i < n; i++) {
                if (i < k) {
                    solution[i] = origin[i];
                } else {
                    int randomPos = rand.nextInt(i + 1);

                    tmp = origin[i];
                    origin[i] = origin[randomPos];
                    origin[randomPos] = tmp;

                    if (randomPos < k) {
                        solution[randomPos] = origin[randomPos];
                    }
                }
            }*/
            // Reservoir algorithm R
            /*System.arraycopy(origin, 0, solution, 0, solution.length);
            for (int i = k; i < n; i++) {
                j = rand.nextInt(i + 1);

                tmp = origin[i];
                origin[i] = origin[j];
                origin[j] = tmp;

                if (j < k) {
                    solution[j] = origin[j];
                }
            }*/
            // Fast geometric approximation of reservoir sampling
            System.arraycopy(origin, 0, solution, 0, k);
            int idx;
            int max = Math.min(n, (k * 4) + 1);
            for (idx = k; idx < max; idx++) {
                j = rand.nextInt(idx + 1);

                tmp = origin[idx];
                origin[idx] = origin[j];
                origin[j] = tmp;

                if (j < k) {
                    solution[j] = origin[j];
                }
            }
            while (idx > 0 && idx < n) {
                double p = (double) k / idx;
                double u = rand.nextDouble();
                int g = (int) Math.floor(Math.log(u) / Math.log(1 - p));
                idx += g;
                if (idx > 0 && idx < n) {
                    j = rand.nextInt(k);  // integer from 0 inclusive to k exclusive

                    solution[j] = origin[idx];
                    origin[idx] = origin[j];
                    origin[j] = solution[j];

                    idx++;
                }
            }
            // Reservoir algorithm L
            /*System.arraycopy(origin, 0, solution, 0, solution.length);
            int R = k;
            double W = Math.exp(Math.log(rand.nextDouble()) / k);
            while (true) {
                int S = (int) Math.floor(Math.log(rand.nextDouble()) / Math.log(1 - W));
                R += S + 1;
                //System.out.println("W: " + W);
                //System.out.println("S: " + S);
                //System.out.println("R: " + R);
                if (R < n) {
                    int idx = (int) Math.floor(k * rand.nextDouble());

                    tmp = origin[idx];
                    origin[idx] = origin[R];
                    origin[R] = tmp;

                    solution[idx] = origin[idx];
                    W = W * Math.exp(Math.log(rand.nextDouble()) / k);
                } else {
                    break;
                }
            }*/
            Arrays.sort(solution);

            //System.out.println("solution: " + Arrays.toString(solution));
            //System.exit(0);
        } while (!used.add(getArrayHashCode(solution)));
        //System.out.println(Arrays.toString(solution));

        return solution;
    }

    public static int getArrayHashCode(int[] array) {
        int hash = 7;

        for (int i = 0; i < array.length; i++) {
            hash = 29 * hash + array[i];
        }

        return hash;
    }

}