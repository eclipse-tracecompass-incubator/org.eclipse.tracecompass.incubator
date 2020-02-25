/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper around a list, to facilitate scripts creating, accessing and
 * returning list types
 *
 * @author Geneviève Bastien
 *
 * @param <T>
 *            The type of object to go in the list
 */
public class ListWrapper<T> {

    private final List<T> fList = new ArrayList<>();

    /**
     * Constructor
     */
    public ListWrapper() {
        // Do nothing
    }

    /**
     * Get the list being wrapped. This object can be used directly, like a Java
     * list.
     *
     * @return The list object, that can be used like a Java list.
     */
    public List<T> getList() {
        return fList;
    }

}
