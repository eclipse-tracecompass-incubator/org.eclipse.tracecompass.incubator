/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.rocm.core.trace;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old.ApiEventHandler;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old.RocmStrings;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;

import com.google.common.collect.ImmutableList;

/**
 * Some aspects to help the user navigate the ROCm events.
 *
 * @author Arnaud Fiorini
 */
@org.eclipse.jdt.annotation.NonNullByDefault
public class RocmAspects {
    private static final ITmfEventAspect<Integer> PID_ASPECT = new ITmfEventAspect<>() {
        @Override
        public String getName() {
            return Messages.getMessage(Messages.AspectName_PID);
        }

        @Override
        public String getHelpText() {
            return ITmfEventAspect.EMPTY_STRING;
        }

        @Override
        public @Nullable Integer resolve(ITmfEvent event) {
            return event.getContent().getFieldValue(Integer.class, RocmStrings.PID);
        }
    };

    private static final ITmfEventAspect<Integer> TID_ASPECT = new ITmfEventAspect<>() {
        @Override
        public String getName() {
            return Messages.getMessage(Messages.AspectName_TID);
        }

        @Override
        public String getHelpText() {
            return ITmfEventAspect.EMPTY_STRING;
        }

        @Override
        public @Nullable Integer resolve(ITmfEvent event) {
            return event.getContent().getFieldValue(Integer.class, RocmStrings.TID);
        }
    };

    private static final ITmfEventAspect<Integer> QUEUE_ID_ASPECT = new ITmfEventAspect<>() {
        @Override
        public String getName() {
            return Messages.getMessage(Messages.AspectName_QueueID);
        }

        @Override
        public String getHelpText() {
            return ITmfEventAspect.EMPTY_STRING;
        }

        @Override
        public @Nullable Integer resolve(ITmfEvent event) {
            return event.getContent().getFieldValue(Integer.class, RocmStrings.QUEUE_ID);
        }
    };

    private static final ITmfEventAspect<Integer> STREAM_ID_ASPECT = new ITmfEventAspect<>() {
        @Override
        public String getName() {
            return Messages.getMessage(Messages.AspectName_StreamID);
        }

        @Override
        public String getHelpText() {
            return ITmfEventAspect.EMPTY_STRING;
        }

        @Override
        public @Nullable Integer resolve(ITmfEvent event) {
            return event.getContent().getFieldValue(Integer.class, RocmStrings.STREAM_ID);
        }
    };

    private static final ITmfEventAspect<Integer> QUEUE_INDEX_ASPECT = new ITmfEventAspect<>() {
        @Override
        public String getName() {
            return Messages.getMessage(Messages.AspectName_QueueIndex);
        }

        @Override
        public String getHelpText() {
            return ITmfEventAspect.EMPTY_STRING;
        }

        @Override
        public @Nullable Integer resolve(ITmfEvent event) {
            return event.getContent().getFieldValue(Integer.class, RocmStrings.QUEUE_INDEX);
        }
    };

    private static final ITmfEventAspect<String> FUNCTION_NAME_ASPECT = new ITmfEventAspect<>() {
        @Override
        public String getName() {
            return Messages.getMessage(Messages.AspectName_FunctionName);
        }

        @Override
        public String getHelpText() {
            return ITmfEventAspect.EMPTY_STRING;
        }

        @Override
        public @Nullable String resolve(ITmfEvent event) {
            if (event.getName().endsWith("_api")) { //$NON-NLS-1$
                return ApiEventHandler.getFunctionApiName(event);
            }
            String name = event.getContent().getFieldValue(String.class, RocmStrings.NAME);
            if (name == null) {
                name = event.getContent().getFieldValue(String.class, RocmStrings.KERNEL_NAME);
            }
            return name;
        }
    };

    private static final List<ITmfEventAspect<?>> ASPECTS = ImmutableList.of(
            getPIDAspect(),
            getTIDAspect(),
            getQueueIDAspect(),
            getStreamIDAspect(),
            getQueueIndexAspect(),
            getFunctionNameAspect());

    private RocmAspects() {
    }

    /**
     * Get the aspect for the event pid
     *
     * @return The process ID
     */
    public static ITmfEventAspect<Integer> getPIDAspect() {
        return PID_ASPECT;
    }

    /**
     * Get the aspect for the event tid
     *
     * @return The thread ID
     */
    public static ITmfEventAspect<Integer> getTIDAspect() {
        return TID_ASPECT;
    }

    /**
     * Get the aspect for the event HSA queue ID
     *
     * @return The queue ID
     */
    public static ITmfEventAspect<Integer> getQueueIDAspect() {
        return QUEUE_ID_ASPECT;
    }

    /**
     * Get the aspect for the event HIP stream ID
     *
     * @return The stream ID
     */
    public static ITmfEventAspect<Integer> getStreamIDAspect() {
        return STREAM_ID_ASPECT;
    }

    /**
     * Get the aspect for the event queue index
     *
     * @return The event index in its HSA queue
     */
    public static ITmfEventAspect<Integer> getQueueIndexAspect() {
        return QUEUE_INDEX_ASPECT;
    }

    /**
     * Get the name of the function executed represented by the event
     *
     * @return The function name
     */
    public static ITmfEventAspect<String> getFunctionNameAspect() {
        return FUNCTION_NAME_ASPECT;
    }

    /**
     * Get the list of all Rocm aspects
     *
     * @return the list of aspects
     */
    public static List<ITmfEventAspect<?>> getAllAspects() {
        return ASPECTS;
    }
}
