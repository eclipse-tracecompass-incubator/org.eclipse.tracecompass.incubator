/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.layout;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.DefaultEventLayout;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.IGenericFtraceConstants;

/**
 * Event and field definitions for ftrace traces.
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
public class GenericFtraceEventLayout extends DefaultEventLayout {

    /* Field names */
    private static final @NonNull String NEXT_PID = "next_pid"; //$NON-NLS-1$
    private static final @NonNull String PREV_PID = "prev_pid"; //$NON-NLS-1$
    private static final @NonNull String CHILD_TID = "child_pid"; //$NON-NLS-1$
    private static final @NonNull String TID = "pid"; //$NON-NLS-1$
    private static final @NonNull String CPU_FREQUENCY = "cpu_frequency"; //$NON-NLS-1$
    private static @Nullable GenericFtraceEventLayout INSTANCE;

    /**
     * The instance of this event layout
     * <p>
     * This object is completely immutable, so no need to create additional
     * instances via the constructor.
     *
     * @return the instance
     */
    public static synchronized @NonNull GenericFtraceEventLayout getInstance() {
        GenericFtraceEventLayout inst = INSTANCE;
        if (inst == null) {
            inst = new GenericFtraceEventLayout();
            INSTANCE = inst;
        }
        return inst;
    }

    @Override
    public String fieldNextTid() {
        return NEXT_PID;
    }

    @Override
    public String fieldPrevTid() {
        return PREV_PID;
    }

    @Override
    public String fieldTid() {
        return TID;
    }

    @Override
    public String fieldParentTid() {
        return TID;
    }

    @Override
    public String fieldChildTid() {
        return CHILD_TID;
    }

    @Override
    public @NonNull String eventCpuFrequency() {
        return CPU_FREQUENCY;
    }

    @Override
    public @NonNull String eventStatedumpProcessState() {
        return IGenericFtraceConstants.PROCESS_DUMP_EVENT_NAME;
    }

}
