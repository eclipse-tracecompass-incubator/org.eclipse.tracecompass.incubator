/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.core.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.tracex.core.Activator;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTimerDirection;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSource;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.io.ResourceUtil;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/** Provides persisted imported-trace clocks through the standard configuration service. */
public class TraceXClockConfigurationSource implements ITmfConfigurationSource {

    /** Configuration source extension identifier. */
    public static final String ID = "org.eclipse.tracecompass.incubator.tracex.core.clock"; //$NON-NLS-1$
    private static final String TRACE_TYPE = "org.eclipse.tracecompass.incubator.tracex.core.trace"; //$NON-NLS-1$
    private static final String TRACE_URI = "trace_uri"; //$NON-NLS-1$
    private static final String DIRECTION = "timer_direction"; //$NON-NLS-1$
    private static final String FREQUENCY = "ticks_per_second"; //$NON-NLS-1$
    private static final Set<String> KEYS = Set.of(TRACE_URI, DIRECTION, FREQUENCY);
    private static final ITmfConfigurationSourceType TYPE = new TmfConfigurationSourceType.Builder()
            .setId(ID).setName("TraceX clock") //$NON-NLS-1$
            .setDescription("Configure an imported TraceX trace while its trace and experiments are closed") //$NON-NLS-1$
            .setConfigParamDescriptors(List.of(
                    new TmfConfigParamDescriptor.Builder().setKeyName(TRACE_URI)
                            .setDescription("File path of the imported trace").build(), //$NON-NLS-1$
                    new TmfConfigParamDescriptor.Builder().setKeyName(DIRECTION)
                            .setDescription("increasing or decreasing").build(), //$NON-NLS-1$
                    new TmfConfigParamDescriptor.Builder().setKeyName(FREQUENCY)
                            .setDescription("Positive decimal ticks per second; omit to leave physical time unknown") //$NON-NLS-1$
                            .setIsRequired(false).build()))
            .build();

    @Override
    public ITmfConfigurationSourceType getConfigurationSourceType() {
        return TYPE;
    }

    @Override
    @Deprecated(since = "10.0", forRemoval = true)
    public ITmfConfiguration create(Map<String, Object> parameters)
            throws TmfConfigurationException {
        return apply(null, parameters);
    }

    @Override
    @Deprecated(since = "10.0", forRemoval = true)
    public ITmfConfiguration update(String id, Map<String, Object> parameters)
            throws TmfConfigurationException {
        return apply(id, parameters);
    }

    @Override
    public ITmfConfiguration create(ITmfConfiguration configuration)
            throws TmfConfigurationException {
        return apply(null, configuration.getParameters());
    }

    @Override
    public ITmfConfiguration update(String id, ITmfConfiguration configuration)
            throws TmfConfigurationException {
        return apply(id, configuration.getParameters());
    }

    /** Validate the entire request before updating one closed imported resource. */
    private ITmfConfiguration apply(@Nullable String id, Map<String, Object> parameters)
            throws TmfConfigurationException {
        try {
            if (!KEYS.containsAll(parameters.keySet())) {
                throw new IllegalArgumentException("Unknown TraceX clock parameter"); //$NON-NLS-1$
            }
            String uri = requiredString(parameters, TRACE_URI);
            TraceXTimerDirection direction = TraceXTimerDirection.parse(requiredString(parameters, DIRECTION));
            Object value = parameters.get(FREQUENCY);
            if (value != null && !(value instanceof String)) {
                throw new IllegalArgumentException("Frequency must be a decimal string"); //$NON-NLS-1$
            }
            IResource target = null;
            for (IResource resource : getResources()) {
                if (Path.of(location(resource)).toAbsolutePath().normalize()
                        .equals(Path.of(uri).toAbsolutePath().normalize())) {
                    if (target != null) {
                        throw new IllegalArgumentException("Trace path identifies more than one imported trace"); //$NON-NLS-1$
                    }
                    target = resource;
                }
            }
            if (target == null || (id != null && !id.equals(configurationId(target)))) {
                throw new IllegalArgumentException("Imported TraceX trace not found"); //$NON-NLS-1$
            }
            requireClosed(target);
            TraceXTrace trace = new TraceXTrace();
            try {
                trace.initTrace(target, location(target), null);
                trace.configureClock(direction, (String) value);
                return configuration(target, trace);
            } finally {
                trace.dispose();
            }
        } catch (CoreException | TmfTraceException | IllegalArgumentException e) {
            throw new TmfConfigurationException(e.getMessage(), e);
        }
    }

    /** Reject edits while a live trace or experiment owns the resource. */
    private static void requireClosed(IResource resource) {
        for (ITmfTrace opened : TmfTraceManager.getInstance().getOpenedTraces()) {
            for (ITmfTrace child : TmfTraceManager.getTraceSet(opened)) {
                if (resource.equals(child.getResource())) {
                    throw new IllegalArgumentException("Close the trace and its experiments before changing its clock"); //$NON-NLS-1$
                }
            }
        }
    }

    /** Return a nonempty string parameter or reject its type and absence. */
    private static String requiredString(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (!(value instanceof String) || ((String) value).isBlank()) {
            throw new IllegalArgumentException("Missing or invalid parameter: " + key); //$NON-NLS-1$
        }
        return (String) value;
    }

    /** Locate imported TraceX resources without depending on desktop or server bundles. */
    protected List<IResource> getResources() throws CoreException {
        List<IResource> resources = new ArrayList<>();
        ResourcesPlugin.getWorkspace().getRoot().accept(resource -> {
            if (resource.getType() == IResource.FILE) {
                if (TRACE_TYPE.equals(resource.getPersistentProperty(TmfCommonConstants.TRACETYPE))
                        && (ResourceUtil.isSymbolicLink(resource)
                                || resource.getPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER) != null)) {
                    resources.add(resource);
                }
                return false;
            }
            return true;
        });
        return resources;
    }

    /** Resolve symbolic trace resources through the framework's location contract. */
    private static String location(IResource resource) throws CoreException {
        IPath path = ResourceUtil.getLocation(resource);
        if (path == null) {
            throw new IllegalArgumentException("Imported trace location is unavailable"); //$NON-NLS-1$
        }
        return path.toOSString();
    }

    /** Produce a stable workspace configuration identifier without exposing resource paths. */
    private static String configurationId(IResource resource) {
        return UUID.nameUUIDFromBytes(resource.getFullPath().toString()
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** Describe the persisted clock of one imported trace. */
    private static ITmfConfiguration configuration(IResource resource, TraceXTrace trace)
            throws CoreException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(TRACE_URI, location(resource));
        parameters.put(DIRECTION, trace.getTimerDirection().getPropertyValue());
        trace.getTicksPerSecond().ifPresent(value -> parameters.put(FREQUENCY, Long.toString(value)));
        return new TmfConfiguration.Builder().setId(configurationId(resource))
                .setName(resource.getName()).setSourceTypeId(ID).setParameters(parameters).build();
    }

    @Override
    public @Nullable ITmfConfiguration get(String id) {
        return getConfigurations().stream().filter(config -> id.equals(config.getId())).findFirst().orElse(null);
    }

    @Override
    public @Nullable ITmfConfiguration remove(String id) {
        ITmfConfiguration previous = get(id);
        if (previous != null) {
            try {
                Map<String, Object> parameters = new HashMap<>(previous.getParameters());
                parameters.put(DIRECTION, TraceXTimerDirection.INCREASING.getPropertyValue());
                parameters.remove(FREQUENCY);
                apply(id, parameters);
            } catch (TmfConfigurationException e) {
                Activator.getInstance().logError("Failed to reset TraceX clock", e); //$NON-NLS-1$
                return null;
            }
        }
        return previous;
    }

    @Override
    public boolean contains(String id) {
        return get(id) != null;
    }

    @Override
    public List<ITmfConfiguration> getConfigurations() {
        List<ITmfConfiguration> configurations = new ArrayList<>();
        try {
            for (IResource resource : getResources()) {
                TraceXTrace trace = new TraceXTrace();
                try {
                    trace.initTrace(resource, location(resource), null);
                    // Default clocks have no customized configuration to remove.
                    if (trace.getTimerDirection() != TraceXTimerDirection.INCREASING
                            || trace.getTicksPerSecond().isPresent()) {
                        configurations.add(configuration(resource, trace));
                    }
                } finally {
                    trace.dispose();
                }
            }
        } catch (CoreException | TmfTraceException | IllegalArgumentException e) {
            Activator.getInstance().logError("Failed to read TraceX clocks", e); //$NON-NLS-1$
        }
        return configurations;
    }

    @Override
    public void dispose() {
        // Settings belong to trace resources, not this configuration source instance.
    }
}
