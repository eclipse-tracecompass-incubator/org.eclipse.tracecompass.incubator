/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.profiling.core.trace;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.ctf.core.event.aspect.CtfCpuAspect;

/**
 * "CPU" event aspect for Perf CTF traces.
 *
 * @author Matthew Khouzam
 */
class PerfCpuAspect extends TmfCpuAspect {

    private static final String CPU_FIELD = "perf_cpu"; //$NON-NLS-1$
    private static final CtfCpuAspect CTF_BASE_ASPECT = new CtfCpuAspect();

    @Override
    public @Nullable Integer resolve(ITmfEvent event) {
        final ITmfEventField field = event.getContent().getField(CPU_FIELD);
        if (field != null) {
            final Object value = field.getValue();
            if (value instanceof Long) {
                return ((Long) value).intValue();
            }
        }
        return CTF_BASE_ASPECT.resolve(event);
    }
}
