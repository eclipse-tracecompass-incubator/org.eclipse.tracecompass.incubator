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

import java.util.Collection;

import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Interface to define the layout of the events and their fields.
 *
 * @author Arnaud Fiorini
 */
public interface IGpuTraceEventLayout {

    /**
     * This function returns the layout for each API in this trace type.
     *
     * @return a collection of api event layout
     */
    public abstract Collection<IApiEventLayout> getApiLayouts();

    /**
     * This function returns the API corresponding to the API call that this
     * event describes.
     *
     * @param event
     *            the event object
     * @return the api layout corresponding to that event
     */
    public abstract IApiEventLayout getCorrespondingApiLayout(ITmfEvent event);

    /**
     * This function returns whether or not this event is a call that will
     * result in a memory copy between the host and a device or between two
     * devices.
     *
     * @param event
     *            the event object
     * @return whether the event corresponds to a memory copy
     */
    public abstract boolean isMemcpyBegin(ITmfEvent event);

    /**
     * This function returns whether or not this event is a call that will
     * result in a kernel launch that will happen on a GPU.
     *
     * @param event
     *            the event object
     * @return whether the event corresponds to a kernel launch
     */
    public abstract boolean isLaunchBegin(ITmfEvent event);

    /**
     * This function returns whether or not this event is corresponding to an
     * API function call.
     *
     * @param event
     *            the event object
     * @return whether the event corresponds to an API call
     */
    public abstract boolean isApiEvent(ITmfEvent event);

    /**
     * This API layout is used to describe the fields of the events related to
     * one API.
     */
    public interface IApiEventLayout {
        /**
         * This function returns wether or not this event describes the
         * beginning of a call.
         *
         * @param event
         *            the event object
         * @return whether the event timestamp is the beginning
         */
        public abstract boolean isBeginEvent(ITmfEvent event);

        /**
         * This function extracts the name of the event.
         *
         * @param event
         *            the event object
         * @return the event name
         */
        public abstract String getEventName(ITmfEvent event);

        /**
         * This function returns the name of the API that this layout describes.
         *
         * @return the API name
         */
        public abstract String getApiName();
    }

    /**
     * This function returns the field to get the thread ID.
     *
     * @return the field name or empty string if not applicable
     */
    public abstract String fieldThreadId();

    /**
     * This function returns the field to get the duration of the call.
     *
     * @return the field name or empty string if not applicable
     */
    public abstract String fieldDuration();
}
