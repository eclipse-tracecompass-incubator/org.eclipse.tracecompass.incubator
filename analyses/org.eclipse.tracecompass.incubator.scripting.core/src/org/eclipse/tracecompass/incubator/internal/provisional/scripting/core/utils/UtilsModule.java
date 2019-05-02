/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.provisional.scripting.core.utils;

import org.eclipse.ease.modules.WrapToScript;

/**
 * A utility scripting module
 *
 * @author Geneviève Bastien
 */
public class UtilsModule {

    /**
     * Create a new list wrapper
     *
     * @return The list wrapper with an empty list
     */
    @WrapToScript
    public <T> ListWrapper<T> createListWrapper() {
        return new ListWrapper<>();
    }

}
