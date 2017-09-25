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
 * Class used to represent an action to be executed on a map key when merging
 * two keys in one.
 *
 * @author Raphaël Beamonte
 * @param <K>
 *            The type of the keys
 */
public abstract class ActionOnMapKey<K> {
    /**
     * The action to execute
     *
     * @param key1
     *            The first key
     * @param key2
     *            The second key
     * @return The new key
     */
    public abstract K execute(K key1, K key2);
}