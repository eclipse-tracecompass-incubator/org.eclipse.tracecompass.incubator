/*******************************************************************************
 * Copyright (c) 2024 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.gpu.core.trace;

import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Interface to use for traces which include GPU events and API events used to
 * pilot the GPU.
 *
 * @author Arnaud Fiorini
 */
public interface IGpuTrace extends ITmfTrace {

    /**
     * Get the event layout of this trace. Many known concepts from the Linux
     * kernel may be exported under different names, depending on the tracer.
     *
     * @return The event layout
     */
    IGpuTraceEventLayout getGpuTraceEventLayout();
}
