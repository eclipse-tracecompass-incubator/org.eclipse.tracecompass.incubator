/*******************************************************************************
 * Copyright (c) 2018, 2024 Ericsson and others
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.DataProviderDescriptorStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ExperimentModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.MarkerSetStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.MarkerSetsOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.junit.Test;

/**
 * Test the {@link DataProviderService} with focus root level endpoints
 *
 * @author Loic Prieur-Drevon
 * @author Geneviève Bastien
 * @author Bernd Hufmann
 */
@SuppressWarnings("null")
public class DataProviderServiceTest extends RestServerTest {

    /**
     * Test getting the data provider descriptors
     */
    @Test
    public void testProviders() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        WebTarget experiments = getApplicationEndpoint().path(EXPERIMENTS);
        WebTarget providers = experiments.path(exp.getUUID().toString())
                .path(OUTPUTS_PATH);

        Set<DataProviderDescriptorStub> descriptors = getDataProviderDescriptors(providers);
        for (DataProviderDescriptorStub desc : sfExpectedDataProviderDescriptorStub) {
            assertTrue(desc.getName(), descriptors.contains(desc));
        }
    }

    /**
     * Test getting a single data provider descriptor
     */
    @Test
    public void testProvider() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        WebTarget experiments = getApplicationEndpoint().path(EXPERIMENTS);
        WebTarget provider = experiments.path(exp.getUUID().toString())
                .path(OUTPUTS_PATH).path(CALL_STACK_DATAPROVIDER_ID);

        DataProviderDescriptorStub descriptor = provider.request(MediaType.APPLICATION_JSON).get(DataProviderDescriptorStub.class);
        assertNotNull(descriptor);

        assertEquals(EXPECTED_CALLSTACK_PROVIDER_DESCRIPTOR, descriptor);
    }

    /**
     * Test of getting marker sets
     *
     * Note: For this test a marker set extension is defined in the plugin.xml
     * of this test plug-in.
     */
    @Test
    public void testGetMarkerSets() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        MarkerSetsOutputResponseStub outputResponseStub = getMarkerSetsEndpoint(exp.getUUID().toString()).request(MediaType.APPLICATION_JSON).get(MarkerSetsOutputResponseStub.class);
        assertNotNull(outputResponseStub);
        List<MarkerSetStub> markerSets = outputResponseStub.getModel();
        assertFalse(markerSets.isEmpty());
        assertEquals("Example", markerSets.get(0).getName());
        assertEquals("example.id", markerSets.get(0).getId());
    }
}
