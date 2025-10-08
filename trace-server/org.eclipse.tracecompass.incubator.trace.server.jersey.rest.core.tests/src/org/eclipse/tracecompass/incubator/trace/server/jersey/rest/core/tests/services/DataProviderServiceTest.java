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

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.DataProvider;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.MarkerSet;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.MarkerSetsResponse;
import org.junit.Test;

/**
 * Test the {@link DataProviderService} with focus root level endpoints
 *
 * @author Loic Prieur-Drevon
 * @author Geneviève Bastien
 * @author Bernd Hufmann
 */
public class DataProviderServiceTest extends RestServerTest {

    /**
     * Test getting the data provider descriptors
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testProviders() throws ApiException {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        List<DataProvider> descriptors = getDataProviderDescriptors(exp.getUUID());
        for (DataProvider desc : sfExpectedDataProviderDescriptorStub) {
            assertTrue(desc.getName(), descriptors.contains(desc));
        }
    }

    /**
     * Test getting a single data provider descriptor
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testProvider() throws ApiException {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        DataProvider descriptor = sfExpApi.getProvider(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID);
        assertNotNull(descriptor);

        assertEquals(EXPECTED_CALLSTACK_PROVIDER_DESCRIPTOR, descriptor);
    }

    /**
     * Test of getting marker sets
     *
     * Note: For this test a marker set extension is defined in the plugin.xml
     * of this test plug-in.
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testGetMarkerSets() throws ApiException {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        MarkerSetsResponse outputResponseStub = sfAnnotationApi.getMarkerSets(exp.getUUID());
        assertNotNull(outputResponseStub);
        List<MarkerSet> markerSets = outputResponseStub.getModel();
        assertFalse(markerSets.isEmpty());
        assertEquals("Example", markerSets.get(0).getName());
        assertEquals("example.id", markerSets.get(0).getId());
    }
}
