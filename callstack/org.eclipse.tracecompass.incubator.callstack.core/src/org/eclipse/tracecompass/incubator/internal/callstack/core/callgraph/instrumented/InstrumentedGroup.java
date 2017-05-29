/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.instrumented;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.LeafGroupNode;

/**
 * @author Geneviève Bastien
 */
public class InstrumentedGroup extends LeafGroupNode {

    public InstrumentedGroup(@NonNull String name, ICallStackGroupDescriptor descriptor) {
        super(name, descriptor);
    }

}
