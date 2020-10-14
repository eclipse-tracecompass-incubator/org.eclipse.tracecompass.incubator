/*******************************************************************************
 * Copyright (c) 2018, 2020 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.services;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.ExperimentManagerService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ExperimentModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TraceModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Test the {@link ExperimentManagerService}
 *
 * @author Loic Prieur-Drevon
 */
public class ExperimentManagerServiceTest extends RestServerTest {

    private static final String TEST = "test";
    private static final @NonNull ImmutableSet<TraceModelStub> CONTEXT_SWITCH_SET = ImmutableSet.of(CONTEXT_SWITCHES_KERNEL_STUB, CONTEXT_SWITCHES_UST_STUB);
    private static final @NonNull ExperimentModelStub EXPECTED = new ExperimentModelStub(TEST, CONTEXT_SWITCH_SET);

    /**
     * Basic test for the {@link ExperimentManagerService}
     */
    @Test
    public void testExperiment() {
        WebTarget application = getApplicationEndpoint();
        WebTarget traces = application.path(TRACES);
        WebTarget expTarget = application.path(EXPERIMENTS);

        TraceModelStub ustStub = assertPost(traces, CONTEXT_SWITCHES_UST_STUB);
        TraceModelStub kernelStub = assertPost(traces, CONTEXT_SWITCHES_KERNEL_STUB);
        assertEquals(CONTEXT_SWITCH_SET, getTraces(traces));

        assertEquals("experiment set should be empty at this point", Collections.emptySet(), getExperiments(expTarget));

        List<String> traceUUIDs = new ArrayList<>();
        traceUUIDs.add(ustStub.getUUID().toString());
        traceUUIDs.add(kernelStub.getUUID().toString());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, EXPECTED.getName());
        parameters.put(TRACES, traceUUIDs);

        Response response = expTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        ExperimentModelStub expStub = response.readEntity(ExperimentModelStub.class);
        assertEquals("Failed to POST the experiment", EXPECTED, expStub);
        assertEquals("Failed to add experiment to set of experiments", Collections.singleton(EXPECTED), getExperiments(expTarget));
        assertEquals("Adding an experiment should not change the trace set", CONTEXT_SWITCH_SET, getTraces(traces));
        assertEquals("Failed to get the experiment by its UUID", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().get(ExperimentModelStub.class));

        assertEquals("Failed to DELETE the experiment", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().delete().readEntity(ExperimentModelStub.class));
        assertEquals("experiment set should be empty at this point", Collections.emptySet(), getExperiments(expTarget));
        assertEquals("Deleting an experiment should not change the trace set", CONTEXT_SWITCH_SET, getTraces(traces));
        response.close();
    }

    /**
     * Test posting an experiment twice, it should be OK
     */
    @Test
    public void testRePost() {
        WebTarget application = getApplicationEndpoint();
        WebTarget traces = application.path(TRACES);
        WebTarget expTarget = application.path(EXPERIMENTS);

        TraceModelStub ustStub = assertPost(traces, CONTEXT_SWITCHES_UST_STUB);
        TraceModelStub kernelStub = assertPost(traces, CONTEXT_SWITCHES_KERNEL_STUB);
        assertEquals(null, CONTEXT_SWITCH_SET, getTraces(traces));

        assertEquals("experiment set should be empty at this point", Collections.emptySet(), getExperiments(expTarget));

        List<String> traceUUIDs = new ArrayList<>();
        traceUUIDs.add(ustStub.getUUID().toString());
        traceUUIDs.add(kernelStub.getUUID().toString());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, EXPECTED.getName());
        parameters.put(TRACES, traceUUIDs);

        Response response = expTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        ExperimentModelStub expStub = response.readEntity(ExperimentModelStub.class);
        assertEquals("Failed to POST the experiment", EXPECTED, expStub);
        assertEquals("Failed to add experiment to set of experiments", Collections.singleton(EXPECTED), getExperiments(expTarget));
        assertEquals("Adding an experiment should not change the trace set", CONTEXT_SWITCH_SET, getTraces(traces));
        assertEquals("Failed to get the experiment by its UUID", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().get(ExperimentModelStub.class));
        response.close();

        // Make a second post with the same name and traces, should return the experiment
        Response response2 = expTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        ExperimentModelStub expStub2 = response2.readEntity(ExperimentModelStub.class);
        assertEquals("Status of second post", Status.OK.getStatusCode(), response2.getStatus());
        assertEquals("Failed to POST the experiment a second time", EXPECTED, expStub2);
        assertEquals("There should still be only one experiment", Collections.singleton(EXPECTED), getExperiments(expTarget));
        assertEquals("Adding an experiment should not change the trace set", CONTEXT_SWITCH_SET, getTraces(traces));
        assertEquals("Failed to get the experiment by its UUID", EXPECTED, expTarget.path(expStub2.getUUID().toString()).request().get(ExperimentModelStub.class));
        response2.close();

        assertEquals("Failed to DELETE the experiment", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().delete().readEntity(ExperimentModelStub.class));
        assertEquals("experiment set should be empty at this point", Collections.emptySet(), getExperiments(expTarget));
        assertEquals("Deleting an experiment should not change the trace set", CONTEXT_SWITCH_SET, getTraces(traces));

    }

    /**
     * Test posting an experiment with different conflicting situations
     */
    @Test
    public void testPostConflicts() {
        WebTarget application = getApplicationEndpoint();
        WebTarget traces = application.path(TRACES);
        WebTarget expTarget = application.path(EXPERIMENTS);

        TraceModelStub ustStub = assertPost(traces, CONTEXT_SWITCHES_UST_STUB);
        TraceModelStub kernelStub = assertPost(traces, CONTEXT_SWITCHES_KERNEL_STUB);
        TraceModelStub arm64Stub = assertPost(traces, ARM_64_KERNEL_STUB);
        ImmutableSet<TraceModelStub> traceSet = ImmutableSet.of(CONTEXT_SWITCHES_UST_STUB, CONTEXT_SWITCHES_KERNEL_STUB, ARM_64_KERNEL_STUB);
        assertEquals(null, traceSet, getTraces(traces));

        assertEquals("experiment set should be empty at this point", Collections.emptySet(), getExperiments(expTarget));

        List<String> traceUUIDs = new ArrayList<>();
        traceUUIDs.add(kernelStub.getUUID().toString());
        traceUUIDs.add(ustStub.getUUID().toString());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, EXPECTED.getName());
        parameters.put(TRACES, traceUUIDs);

        Response response = expTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        ExperimentModelStub expStub = response.readEntity(ExperimentModelStub.class);
        assertEquals("Failed to POST the experiment", EXPECTED, expStub);
        assertEquals("Failed to add experiment to set of experiments", Collections.singleton(EXPECTED), getExperiments(expTarget));
        assertEquals("Adding an experiment should not change the trace set", traceSet, getTraces(traces));
        assertEquals("Failed to get the experiment by its UUID", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().get(ExperimentModelStub.class));
        response.close();

        // Post same name experiment, but different traces, should return a conflict
        List<String> traceUUIDs2 = new ArrayList<>();
        traceUUIDs2.add(kernelStub.getUUID().toString());
        Map<String, Object> parameters2 = new HashMap<>();
        parameters2.put(NAME, EXPECTED.getName());
        parameters2.put(TRACES, traceUUIDs2);
        Response response2 = expTarget.request().post(Entity.json(new QueryParameters(parameters2, Collections.emptyList())));
        assertEquals("Expected a conflict for posting different experiment", Status.CONFLICT.getStatusCode(), response2.getStatus());
        assertEquals("Conflict should return original experiment name", EXPECTED.getName(), response2.readEntity(ExperimentModelStub.class).getName());
        assertEquals("There should still be only one experiment", ImmutableSet.of(EXPECTED), getExperiments(expTarget));
        assertEquals("Failing to add an experiment should not change the trace set", traceSet, getTraces(traces));
        assertEquals("Failed to get the experiment by its UUID", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().get(ExperimentModelStub.class));
        response2.close();

        // Post same experiment name, but with traces with the same names, but not the same traces
        List<String> traceUUIDs3 = new ArrayList<>();
        traceUUIDs3.add(arm64Stub.getUUID().toString());
        traceUUIDs3.add(ustStub.getUUID().toString());
        Map<String, Object> parameters3 = new HashMap<>();
        parameters3.put(NAME, EXPECTED.getName());
        parameters3.put(TRACES, traceUUIDs3);
        Response response3 = expTarget.request().post(Entity.json(new QueryParameters(parameters3, Collections.emptyList())));
        assertEquals("Expected a conflict for posting different experiment", Status.CONFLICT.getStatusCode(), response3.getStatus());
        assertEquals("Conflict should return original experiment name", EXPECTED.getName(), response3.readEntity(ExperimentModelStub.class).getName());
        assertEquals("There should still be only one experiment", ImmutableSet.of(EXPECTED), getExperiments(expTarget));
        assertEquals("Failing to add an experiment should not change the trace set", traceSet, getTraces(traces));
        assertEquals("Failed to get the new experiment by its UUID", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().get(ExperimentModelStub.class));
        response3.close();

        assertEquals("Failed to DELETE the experiment", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().delete().readEntity(ExperimentModelStub.class));
        assertEquals("experiment set should be empty at this point", Collections.emptySet(), getExperiments(expTarget));
        assertEquals("Deleting an experiment should not change the trace set", traceSet, getTraces(traces));
    }

}
