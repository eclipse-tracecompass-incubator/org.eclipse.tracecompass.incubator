/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.callstack;

/**
 * Interface to be implemented by leaf elements of the callstack hierarchy, ie
 * those that contain actual callstacks.
 *
 * @author Geneviève Bastien
 */
public interface ICallStackLeafElement extends ICallStackElement {

    /**
     * Get the callstack associated with this element
     *
     * @return The call stack
     */
    CallStack getCallStack();

}
