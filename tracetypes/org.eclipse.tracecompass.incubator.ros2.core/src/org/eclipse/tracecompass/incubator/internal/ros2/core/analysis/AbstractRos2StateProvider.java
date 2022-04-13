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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostInfo;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcess;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostThread;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.trace.layout.IRos2EventLayout;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

/**
 * Abstract ROS 2 state provider with some common utilities.
 *
 * @author Christophe Bedard
 */
public abstract class AbstractRos2StateProvider extends AbstractTmfStateProvider {

    /** The event layout */
    protected static final IRos2EventLayout LAYOUT = IRos2EventLayout.getDefault();

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     * @param id
     *            the analysis ID
     */
    protected AbstractRos2StateProvider(ITmfTrace trace, String id) {
        super(Objects.requireNonNull(trace), Objects.requireNonNull(id));
    }

    /**
     * Basic check to figure out if further processing should be done with an
     * event
     *
     * @param event
     *            the event
     * @return true if the event should be handled, false otherwise
     */
    protected static boolean considerEvent(@NonNull ITmfEvent event) {
        // Consider if the provider name matches
        return event.getName().startsWith(IRos2EventLayout.PROVIDER_NAME) || event.getName().startsWith(IRos2EventLayout.DDS_PROVIDER_NAME);
    }

    /**
     * Check if an event is of a given type
     *
     * @param event
     *            the event
     * @param eventName
     *            the event name to check for
     * @return true if the event is of the given type, false otherwise
     */
    protected static boolean isEvent(@NonNull ITmfEvent event, @NonNull String eventName) {
        return event.getName().equals(eventName);
    }

    /**
     * Check if an event has a given field
     *
     * @param event
     *            the event
     * @param fieldName
     *            the field name
     * @return whether the event has the given field
     */
    protected static boolean hasField(@NonNull ITmfEvent event, @NonNull String fieldName) {
        return null != event.getContent().getFieldValue(Object.class, fieldName);
    }

    /**
     * Get field value from an event
     *
     * @param event
     *            the event
     * @param fieldName
     *            the field name to get
     * @return the value of the given field name
     */
    protected static Object getField(@NonNull ITmfEvent event, @NonNull String fieldName) {
        Object val = event.getContent().getFieldValue(Object.class, fieldName);
        if (val == null) {
            Activator.getInstance().logError(String.format("null '%s' field for event: %s", fieldName, event.toString())); //$NON-NLS-1$
        }
        return val;
    }

    /**
     * @param event
     *            the event
     * @return the vpid
     */
    protected static @NonNull Long getPid(@NonNull ITmfEvent event) {
        Long vpid = (Long) event.getContent().getField(LAYOUT.contextVpid()).getValue();
        if (vpid == null) {
            return (long) 0;
        }
        return vpid;
    }

    /**
     * @param event
     *            the event
     * @return the vtid
     */
    protected static @NonNull Long getTid(@NonNull ITmfEvent event) {
        Long vtid = (Long) event.getContent().getField(LAYOUT.contextVtid()).getValue();
        if (vtid == null) {
            return (long) 0;
        }
        return vtid;
    }

    /**
     * @param event
     *            the event
     * @return the corresponding HostInfo
     */
    protected static @NonNull HostInfo hostInfoFrom(@NonNull ITmfEvent event) {
        ITmfTrace trace = event.getTrace();
        String hostname = null;
        if (trace instanceof CtfTmfTrace) {
            hostname = ((CtfTmfTrace) trace).getEnvironment().get("hostname"); //$NON-NLS-1$
        }
        if (null == hostname) {
            hostname = StringUtils.EMPTY;
        }
        return new HostInfo(trace.getHostId(), hostname);
    }

    /**
     * @param event
     *            the event
     * @return the corresponding HostThread
     */
    protected static @NonNull HostThread hostThreadFrom(@NonNull ITmfEvent event) {
        return new HostThread(hostInfoFrom(event), getTid(event));
    }

    /**
     * @param event
     *            the event
     * @return the corresponding HostProcess
     */
    protected static @NonNull HostProcess hostProcessFrom(@NonNull ITmfEvent event) {
        return new HostProcess(hostInfoFrom(event), getPid(event));
    }

    /**
     * @param event
     *            the event
     * @param pointer
     *            the pointer value
     * @return the corresponding HostProcessPointer
     */
    protected static @NonNull HostProcessPointer hostProcessPointerFrom(@NonNull ITmfEvent event, @NonNull Long pointer) {
        return new HostProcessPointer(hostProcessFrom(event), pointer);
    }

    /**
     * @param event
     *            the event
     * @param handle
     *            the handle value
     * @return the corresponding Ros2ObjectHandle
     */
    protected static @NonNull Ros2ObjectHandle handleFrom(@NonNull ITmfEvent event, @NonNull Long handle) {
        return new Ros2ObjectHandle(hostProcessFrom(event), handle);
    }
}
