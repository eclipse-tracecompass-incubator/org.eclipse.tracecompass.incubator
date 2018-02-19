/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.layout;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.DefaultEventLayout;

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
    private static final @NonNull String TID = "pid"; //$NON-NLS-1$
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
}
