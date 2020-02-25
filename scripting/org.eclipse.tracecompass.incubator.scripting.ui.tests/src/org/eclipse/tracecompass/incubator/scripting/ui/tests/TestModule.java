/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.ui.tests;

import java.util.function.Function;

import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.tracecompass.incubator.scripting.core.tests.perf.ScriptingBenchmark;

/**
 * EASE module for scripting purposes
 *
 * @author Geneviève Bastien
 */
public class TestModule {

    /**
     * Execute a simple computation of java instructions
     */
    @WrapToScript
    public void doJavaLoop() {
        int base = ScriptingBenchmark.INITIAL_VALUE;
        long value = base;
        while (base < ScriptingBenchmark.LIMIT) {
            if (value == 1) {
                value = base++;
            }
            if (value % 2 == 0) {
                value = value / 2;
            } else {
                value = 3 * value + 1;
            }
        }
    }

    /**
     * Execute a simple computation, but each loop calls a callback received in
     * parameters to update the value
     *
     * @param function
     *            The function callback to execute
     */
    @WrapToScript
    public void doLoopWithCallback(Function<Long, Number> function) {
        int base = ScriptingBenchmark.INITIAL_VALUE;
        long value = base;
        while (base < 20000) {
            if (value == 1) {
                value = base++;
            }
            value = function.apply(value).longValue();
        }
    }

    /**
     * Compute one value of the sequence
     *
     * @param value
     *            The value to use to compute
     * @return The result of the computation
     */
    @WrapToScript
    public long compute(Long value) {
        if (value % 2 == 0) {
            return value / 2;
        }
        return 3 * value + 1;
    }

}
