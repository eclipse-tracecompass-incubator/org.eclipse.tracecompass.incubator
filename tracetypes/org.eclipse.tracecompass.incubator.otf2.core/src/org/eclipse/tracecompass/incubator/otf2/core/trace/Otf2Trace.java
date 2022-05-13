/**********************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.otf2.core.trace;

import static org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants.OTF2_TYPE_GROUP;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.otf2.core.Activator;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2GlobalDefinitions;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.filter.ITmfFilter;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceContext;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.context.CtfLocation;
import org.eclipse.tracecompass.tmf.ctf.core.context.CtfLocationInfo;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;

/**
 * OTF2 Traces converted into CTF format. These traces are identified using the
 * tracer_name environment variable.
 *
 * @author Yoann Heitz
 */
public class Otf2Trace extends CtfTmfTrace {

    private Collection<ITmfEventAspect<?>> fAspects = TmfBaseAspects.getBaseAspects();

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return fAspects;
    }

    private static final int CONFIDENCE = 100;

    /**
     * Constructor
     */
    public Otf2Trace() {
        super();
    }

    @Override
    public @Nullable IStatus validate(final @Nullable IProject project, final @Nullable String path) {
        IStatus status = super.validate(project, path);
        if (status instanceof CtfTraceValidationStatus) {
            Map<String, String> environment = ((CtfTraceValidationStatus) status).getEnvironment();
            String domain = environment.get("tracer_name"); //$NON-NLS-1$
            if (domain == null || !domain.equals("\"otf2\"")) { //$NON-NLS-1$
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "This trace is not an OTF2 trace"); //$NON-NLS-1$
            }
            return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
        }
        return status;
    }

    @Override
    public TmfTraceContext createTraceContext(TmfTimeRange selection, TmfTimeRange windowRange, @Nullable IFile editorFile, @Nullable ITmfFilter filter) {
        return new TmfTraceContext(selection, windowRange, editorFile, filter);
    }

    @Override
    public void initTrace(final @Nullable IResource resource, final @Nullable String path,
            final @Nullable Class<? extends @Nullable ITmfEvent> eventType) throws TmfTraceException {
        super.initTrace(resource, path, eventType);
        ImmutableList.Builder<ITmfEventAspect<?>> builder = new Builder<>();
        builder.addAll(fAspects);
        builder.addAll(createCountersAndNodesAspects());
        fAspects = builder.build();
    }

    private Collection<ITmfEventAspect<?>> createCountersAndNodesAspects() {
        ImmutableSet.Builder<ITmfEventAspect<?>> aspectsBuilder = new ImmutableSet.Builder<>();
        ITmfContext context = seekEvent(new CtfLocation(new CtfLocationInfo(0L, 0L)));
        ITmfEvent event = getNext(context);

        Otf2SystemTree systemTree = new Otf2SystemTree();
        Otf2SystemMetrics systemMetrics = new Otf2SystemMetrics();
        Map<Integer, String> stringIds = new HashMap<>();

        boolean readingDefinitions = true;

        while (event != null && readingDefinitions) {
            String eventName = event.getName();
            Matcher matcher = IOtf2Constants.OTF2_EVENT_NAME_PATTERN.matcher(eventName);

            if (matcher.matches() && matcher.group(OTF2_TYPE_GROUP).equals(IOtf2Constants.OTF2_EVENT)) {
                readingDefinitions = false;
            }

            if (matcher.matches() && matcher.group(OTF2_TYPE_GROUP).equals(IOtf2Constants.OTF2_GLOBAL_DEFINITION)) {
                String definition = matcher.group(IOtf2Constants.OTF2_NAME_GROUP);
                switch (definition) {
                case IOtf2GlobalDefinitions.OTF2_STRING:
                    addString(event, stringIds);
                    break;
                case IOtf2GlobalDefinitions.OTF2_SYSTEM_TREE_NODE:
                    systemTree.addSystemTreeNode(event);
                    break;
                case IOtf2GlobalDefinitions.OTF2_LOCATION_GROUP:
                    systemTree.addLocationGroup(event);
                    break;
                case IOtf2GlobalDefinitions.OTF2_LOCATION:
                    systemTree.addLocation(event);
                    break;
                case IOtf2GlobalDefinitions.OTF2_METRIC_MEMBER:
                    systemMetrics.addMetricMember(event, stringIds);
                    break;
                case IOtf2GlobalDefinitions.OTF2_METRIC_CLASS:
                    systemMetrics.addMetricClass(event);
                    break;
                default:
                    Activator.getInstance().logWarning("The following group name is not supported: " + definition); //$NON-NLS-1$
                    break;
                }
            }
            event = getNext(context);
        }
        aspectsBuilder.addAll(systemTree.getSystemAspects(stringIds));
        aspectsBuilder.addAll(systemMetrics.getCounterAspects());
        return aspectsBuilder.build();
    }

    private static void addString(ITmfEvent event, Map<Integer, String> stringIds) {
        ITmfEventField content = event.getContent();
        Integer id = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_SELF);
        id = (int) (id != null ? id : IOtf2Constants.OTF2_UNKNOWN_STRING);
        String stringValue = content.getFieldValue(String.class, IOtf2Fields.OTF2_STRING_VALUE);
        stringValue = stringValue != null ? stringValue : IOtf2Constants.UNKNOWN_STRING;
        stringIds.put(id, stringValue);
    }
}
