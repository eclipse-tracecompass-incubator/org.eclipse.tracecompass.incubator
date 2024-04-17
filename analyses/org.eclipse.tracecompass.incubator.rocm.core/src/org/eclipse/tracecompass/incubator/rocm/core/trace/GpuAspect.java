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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfDeviceAspect;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;

/**
 * This aspect describes which GPU is related to each GPU activity or Kernel
 * event
 *
 * @author Arnaud Fiorini
 */
public final class GpuAspect extends TmfDeviceAspect {

    /** The singleton instance */
    public static final GpuAspect INSTANCE = new GpuAspect();

    private GpuAspect() {
    }

    @Override
    public String getName() {
        return Messages.getMessage(Messages.AspectName_GPU);
    }

    @Override
    public String getHelpText() {
        return Messages.getMessage(Messages.AspectHelpText_GPU);
    }

    /**
     * Gets the GPU number if available on an event
     *
     * @param event
     *            The event to get the GPU number from
     * @return the GPU number of the GPU on which this event was executed or
     *         {@code null}
     */
    @Override
    public @Nullable Integer resolve(ITmfEvent event) {
        if (!(event instanceof CtfTmfEvent)) {
            return null;
        }
        return null;
    }

    /**
     * Consider all sub-instance of this type "equal", so that they get merged
     * in a single CPU column/aspect.
     *
     * @param other
     *            The object to compare it with
     * @return true if the object is of the same type
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return (other instanceof GpuAspect);
    }

    @Override
    public String getDeviceType() {
        return "gpu"; //$NON-NLS-1$
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
