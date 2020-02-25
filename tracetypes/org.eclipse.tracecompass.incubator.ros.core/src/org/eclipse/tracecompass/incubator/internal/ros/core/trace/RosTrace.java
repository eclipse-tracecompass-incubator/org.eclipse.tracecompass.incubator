/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.trace;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.core.trace.layout.IRosEventLayout;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventFactory;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

import com.google.common.collect.ImmutableSet;

/**
 * Class which contains ROS traces
 *
 * @author Christophe Bedard
 */
public class RosTrace extends CtfTmfTrace {

    /**
     * Base confidence if the trace contains an init_node event
     */
    private static final int CONFIDENCE = 101;

    private static final @NonNull Collection<ITmfEventAspect<?>> ROS_ASPECTS;
    static {
        ImmutableSet.Builder<ITmfEventAspect<?>> builder = ImmutableSet.builder();
        builder.addAll(CtfTmfTrace.CTF_ASPECTS).build();
        ROS_ASPECTS = builder.build();
    }

    private @NonNull Collection<ITmfEventAspect<?>> fRosTraceAspects = ImmutableSet.copyOf(ROS_ASPECTS);

    private @Nullable IRosEventLayout fLayout = null;

    /**
     * Default constructor
     */
    public RosTrace() {
        super(CtfTmfEventFactory.instance());
    }

    /**
     * Protected constructor for child classes
     *
     * @param factory
     *            the event factory for this specific trace
     */
    protected RosTrace(@NonNull CtfTmfEventFactory factory) {
        super(factory);
    }

    /**
     * Get the event layout to use with this trace. This normally depends on the
     * tracer's version.
     *
     * @return the event layout
     */
    public @NonNull IRosEventLayout getEventLayout() {
        IRosEventLayout layout = fLayout;
        if (layout == null) {
            throw new IllegalStateException("Cannot get the layout of a non-initialized trace!"); //$NON-NLS-1$
        }
        return layout;
    }

    @Override
    public void initTrace(IResource resource, String path,
            Class<? extends ITmfEvent> eventType) throws TmfTraceException {
        super.initTrace(resource, path, eventType);

        // Determine the event layout to use
        IRosEventLayout layout = getLayout();
        fLayout = layout;

        // Nothing else
    }

    private static @NonNull IRosEventLayout getLayout() {
        // Only one layout for the moment
        return checkNotNull(IRosEventLayout.getDefault());
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return fRosTraceAspects;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation sets the confidence to 101 if the trace contains an
     * init_node event. The confidence is incremented for every other event from
     * the layout present in the trace.
     */
    @Override
    public IStatus validate(final IProject project, final String path) {
        IStatus status = super.validate(project, path);
        if (status instanceof CtfTraceValidationStatus) {
            Collection<String> eventNames = ((CtfTraceValidationStatus) status).getEventNames();
            // Make sure the trace contains an event from the roscpp tracepoint provider
            if (!eventNames.stream().anyMatch(event -> event.startsWith(IRosEventLayout.PROVIDER_NAME))) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The trace is not a ROS trace."); //$NON-NLS-1$
            }
            // Increment confidence for each other event that is present
            int conf = CONFIDENCE;
            conf += eventNames.stream().filter(eventName -> getLayout().getEventNames().contains(eventName)).count();
            return new TraceValidationStatus(conf, Activator.PLUGIN_ID);
        }
        return status;
    }
}
