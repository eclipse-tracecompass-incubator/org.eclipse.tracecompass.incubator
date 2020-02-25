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

package org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.builder;

/**
 * A class representing an equality runner used in the build process
 *
 * @author Raphaël Beamonte
 *
 * @param <T>
 *            Indistinctive type
 */
public class EqualityRunner<T> {
    /**
     * Return whether t0 and t1 are equal
     *
     * @param t0
     *            The first element
     * @param t1
     *            The second element
     * @return Whether t0 and t1 are equal
     */
    public boolean isEqual(T t0, T t1) {
        if (t0 == null) {
            return (t1 == null);
        }
        return t0.equals(t1);
    }

    /**
     * Return the common value for elements t0 and t1
     *
     * @param t0
     *            The first element
     * @param t1
     *            The second element
     * @return The common value
     */
    public T commonValue(T t0, T t1) {
        return t0;
    }
}