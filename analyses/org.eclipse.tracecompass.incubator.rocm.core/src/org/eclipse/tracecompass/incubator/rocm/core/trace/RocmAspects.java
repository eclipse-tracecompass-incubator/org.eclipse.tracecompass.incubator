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
            return event.getContent().getFieldValue(Integer.class, "context._pid");
        }
    };


    private static final List<ITmfEventAspect<?>> ASPECTS = ImmutableList.of(PID_ASPECT);

    private RocmAspects() {
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
