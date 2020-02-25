/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.event;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;

/**
 * A trace compass log aspect
 *
 * @author Matthew Khouzam
 *
 * @param <T>
 */
interface ITraceEventAspect<T> extends ITmfEventAspect<T> {

    @Override
    default @Nullable T resolve(@NonNull ITmfEvent event) {
        if (event instanceof TraceEventEvent) {
            return resolveTCL((TraceEventEvent) event);
        }
        return null;
    }

    T resolveTCL(@NonNull TraceEventEvent event);

}
