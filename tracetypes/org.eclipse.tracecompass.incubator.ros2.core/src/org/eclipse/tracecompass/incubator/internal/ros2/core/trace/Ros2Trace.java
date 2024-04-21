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

package org.eclipse.tracecompass.incubator.internal.ros2.core.trace;

import java.util.Collection;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros2.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.core.trace.layout.IRos2EventLayout;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.context.CtfLocation;
import org.eclipse.tracecompass.tmf.ctf.core.context.CtfLocationInfo;
import org.eclipse.tracecompass.tmf.ctf.core.context.CtfTmfContext;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventFactory;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;
import org.osgi.framework.Version;

import com.google.common.collect.ImmutableSet;

/**
 * Class which contains ROS traces
 *
 * @author Christophe Bedard
 */
public class Ros2Trace extends CtfTmfTrace {

    /**
     * Base confidence if the trace contains a ros2:* event
     */
    private static final int CONFIDENCE = 101;

    private static final @NonNull Collection<ITmfEventAspect<?>> ROS_ASPECTS;
    static {
        ImmutableSet.Builder<ITmfEventAspect<?>> builder = ImmutableSet.builder();
        builder.addAll(CtfTmfTrace.CTF_ASPECTS).build();
        ROS_ASPECTS = builder.build();
    }

    /**
     * Last version before the 'version' field was added. As it turns out, the
     * field was added in 0.1.0.
     */
    private static final @NonNull String TRACETOOLS_VERSION_UNKNOWN = "0.0.0"; //$NON-NLS-1$

    private @NonNull Collection<ITmfEventAspect<?>> fRos2TraceAspects = ImmutableSet.copyOf(ROS_ASPECTS);

    private @Nullable IRos2EventLayout fLayout = null;
    private @Nullable Version fTracetoolsVersion = null;

    /**
     * Default constructor
     */
    public Ros2Trace() {
        super(CtfTmfEventFactory.instance());
    }

    /**
     * Protected constructor for child classes
     *
     * @param factory
     *            the event factory for this specific trace
     */
    protected Ros2Trace(@NonNull CtfTmfEventFactory factory) {
        super(factory);
    }

    /**
     * Get the event layout to use with this trace. This normally depends on the
     * tracer's version.
     *
     * @return the event layout
     */
    public @NonNull IRos2EventLayout getEventLayout() {
        IRos2EventLayout layout = fLayout;
        if (layout == null) {
            throw new IllegalStateException("Cannot get the layout of a non-initialized trace!"); //$NON-NLS-1$
        }
        return layout;
    }

    /**
     * @return the version of the ROS 2 tracetools package used to generate the
     *         trace
     */
    public @NonNull Version getTracetoolsVersion() {
        Version tracetoolsVersion = fTracetoolsVersion;
        if (null == tracetoolsVersion) {
            throw new IllegalStateException("Cannot get the tracetools version of a non-initialized trace"); //$NON-NLS-1$
        }
        return tracetoolsVersion;
    }

    @Override
    public void initTrace(IResource resource, String path,
            Class<? extends ITmfEvent> eventType) throws TmfTraceException {
        super.initTrace(resource, path, eventType);

        // Determine the event layout to use
        fLayout = getLayout();
        fTracetoolsVersion = getTracetoolsVersion(fLayout);
    }

    private static @NonNull IRos2EventLayout getLayout() {
        // Only one layout for the moment
        return Objects.requireNonNull(IRos2EventLayout.getDefault());
    }

    private @NonNull Version getTracetoolsVersion(IRos2EventLayout layout) {
        return getVersionFromEvent(layout.eventRclInit(), layout);
    }

    private @NonNull Version getVersionFromEvent(@NonNull String versionEventName, IRos2EventLayout layout) {
        CtfTmfEvent event = seekEvent(versionEventName);
        if (null == event) {
            /**
             * This could happen if the tracepoint is not enabled, in which case
             * we just have to assume a version.
             */
            Activator.getInstance().logError("Cannot get event " + versionEventName); //$NON-NLS-1$
            return new Version(TRACETOOLS_VERSION_UNKNOWN);
        }
        String versionStr = (String) event.getContent().getFieldValue(Object.class, layout.fieldVersion());
        if (null == versionStr) {
            /**
             * If the event exists, but the field doesn't exist, default to the
             * version right before the field was added.
             */
            return new Version(TRACETOOLS_VERSION_UNKNOWN);
        }
        return new Version(versionStr);
    }

    private @Nullable CtfTmfEvent seekEvent(@NonNull String eventName) {
        // Seek the first event and then advance until we get the right event
        CtfLocation location = new CtfLocation(new CtfLocationInfo(0L, 0L));
        ITmfContext result = seekEvent(location);
        CtfTmfContext context = (CtfTmfContext) result;
        while (!context.getCurrentEvent().getName().equals(eventName)) {
            if (!context.advance()) {
                return null;
            }
        }
        return context.getCurrentEvent();
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return fRos2TraceAspects;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation sets the confidence to 101 if the trace contains at
     * least one ros2:* event. The confidence is incremented for every other
     * event from the layout present in the trace.
     */
    @Override
    public IStatus validate(final IProject project, final String path) {
        IStatus status = super.validate(project, path);
        if (status instanceof CtfTraceValidationStatus) {
            Collection<String> eventNames = ((CtfTraceValidationStatus) status).getEventNames();
            /**
             * Make sure the trace contains an event from the ros2 tracepoint
             * provider.
             */
            if (eventNames.stream().noneMatch(event -> event.startsWith(IRos2EventLayout.PROVIDER_NAME))) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The trace is not a ROS 2 trace."); //$NON-NLS-1$
            }
            // Increment confidence for each other event that is present
            int conf = CONFIDENCE;
            conf += eventNames.stream().filter(eventName -> getLayout().getEventNames().contains(eventName)).count();
            return new TraceValidationStatus(conf, Activator.PLUGIN_ID);
        }
        return status;
    }
}
