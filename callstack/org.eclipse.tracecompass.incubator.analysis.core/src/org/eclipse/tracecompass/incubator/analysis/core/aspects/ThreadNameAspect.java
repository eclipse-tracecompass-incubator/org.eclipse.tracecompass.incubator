/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.aspects;

import org.eclipse.tracecompass.incubator.internal.analysis.core.aspects.Messages;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;

/**
 * Thread name aspect, an aspect to determine the name of the name of a thread,
 * Can be duplicates.
 *
 * @author Matthew Khouzam
 *
 */
public abstract class ThreadNameAspect implements ITmfEventAspect<String> {

    @Override
    public String getName() {
        return String.valueOf(Messages.ThreadNameAspect_name);
    }

    @Override
    public String getHelpText() {
        return String.valueOf(Messages.ThreadNameAspect_description);
    }
}
