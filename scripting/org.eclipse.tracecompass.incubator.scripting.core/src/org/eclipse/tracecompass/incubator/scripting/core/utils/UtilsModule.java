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

import java.util.List;
import java.util.function.Function;

import org.eclipse.ease.modules.WrapToScript;

/**
 * A utility scripting module that wraps certain classes in more convenient
 * objects. Script developers should try to use the wrapped class directly first
 * as it may not be problematic in some cases and if there are execution
 * problems when running the script, use the wrapper instead.
 *
 * @author Geneviève Bastien
 */
public class UtilsModule {

    /**
     * Create a new list wrapper. Useful when passing {@link Function} classes
     * to module methods where the function return value is a {@link List}. Some
     * scripting engines do not handle very well those objects.
     *
     * @param <T>
     *            The type of elements that will be in the list
     * @return The {@link ListWrapper} object with an empty list
     */
    @WrapToScript
    public <T> ListWrapper<T> createListWrapper() {
        return new ListWrapper<>();
    }

    /**
     * Convert a string to an array of string, as some scripting languages do
     * not easily support arrays (like jython)
     *
     * @param string
     *            The element to add to the array
     * @return An array of string containing the element in parameter
     */
    @WrapToScript
    public String[] strToArray(String string) {
        String[] array = new String[1];
        array[0] = string;
        return array;
    }

}
