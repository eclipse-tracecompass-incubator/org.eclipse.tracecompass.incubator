/*******************************************************************************
 * Copyright (c) 2024, 2025 Ericsson and others
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.ErrorResponseImpl;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.DataProviderDescriptorStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ExperimentModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TestDataProviderFactory;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TmfConfigurationSourceTypeStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.config.TestSchemaConfigurationSource;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.junit.Test;

/**
 * Test the {@link DataProviderService} class with focus on data provider configuration endpoints
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings("null")
public class DataProviderConfigurationServiceTest extends RestServerTest {
    private static final String UNKNOWN_TYPE_ID = "unknown.config.type.id";

    private static final String CONFIG_NAME = "My configuration";
    private static final String CONFIG_DESCRIPTION = "My description";

    private static final GenericType<List<TmfConfigurationSourceTypeStub>> DP_CONFIG_TYPES_SET_TYPE = new GenericType<>() {};

    /**
     * Tests querying the data provider config types
     */
    @Test
    public void testDataProviderConfigTypes() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        WebTarget configTypesEndpoint = getConfigEndpoint(exp.getUUID().toString(), TestDataProviderFactory.ID);

        // Get all config types
        List<TmfConfigurationSourceTypeStub> configTypesListModel = configTypesEndpoint.request(MediaType.APPLICATION_JSON).get(DP_CONFIG_TYPES_SET_TYPE);
        assertNotNull(configTypesListModel);
        assertTrue(configTypesListModel.size() == 1);
        Optional<TmfConfigurationSourceTypeStub> optional = configTypesListModel.stream().filter(config -> config.getId().equals(TestSchemaConfigurationSource.TYPE.getId())).findAny();
        assertTrue(optional.isPresent());

        // Test Valid config type ID
        WebTarget singleTypeEndpoint = configTypesEndpoint.path(optional.get().getId());
        TmfConfigurationSourceTypeStub singleConfigType = singleTypeEndpoint.request(MediaType.APPLICATION_JSON).get(TmfConfigurationSourceTypeStub.class);
        assertNotNull(singleConfigType);
        assertEquals(optional.get().getId(), singleConfigType.getId());

        // Test config types for data provider that can't be configured
        WebTarget configTypesEndpoint2 = getConfigEndpoint(exp.getUUID().toString(), CALL_STACK_DATAPROVIDER_ID);
        List<TmfConfigurationSourceTypeStub> emptyConfigTypesListModel = configTypesEndpoint2.request(MediaType.APPLICATION_JSON).get(DP_CONFIG_TYPES_SET_TYPE);
        assertNotNull(emptyConfigTypesListModel);
        assertTrue(emptyConfigTypesListModel.isEmpty());
    }

    /**
     * Tests error cases when querying data provider config types
     */
    @Test
    public void testDataProviderConfigTypesErrors() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        WebTarget configTypesEndpoint = getConfigEndpoint(UNKNOWN_EXP_UUID, TestDataProviderFactory.ID);

        // Unknown experiment
        try (Response response = configTypesEndpoint.request(MediaType.APPLICATION_JSON).get()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_TRACE, response.readEntity(ErrorResponseImpl.class).getTitle());
        }

        WebTarget singleTypeEndpoint = configTypesEndpoint.path(TestSchemaConfigurationSource.TYPE.getId());
        try (Response response = singleTypeEndpoint.request(MediaType.APPLICATION_JSON).get()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_TRACE, response.readEntity(ErrorResponseImpl.class).getTitle());
        }

        // Unknown data provider
        configTypesEndpoint = getConfigEndpoint(exp.getUUID().toString(), UNKNOWN_DP_ID);
        try (Response response = configTypesEndpoint.request(MediaType.APPLICATION_JSON).get()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER, response.readEntity(ErrorResponseImpl.class).getTitle());
        }

        singleTypeEndpoint = configTypesEndpoint.path(TestSchemaConfigurationSource.TYPE.getId());
        try (Response response = singleTypeEndpoint.request(MediaType.APPLICATION_JSON).get()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER, response.readEntity(ErrorResponseImpl.class).getTitle());
        }

        // Test config type is not applicable for another data provider
        configTypesEndpoint = getConfigEndpoint(exp.getUUID().toString(), CALL_STACK_DATAPROVIDER_ID);
        singleTypeEndpoint = configTypesEndpoint.path(TestSchemaConfigurationSource.TYPE.getId());
        try (Response response = singleTypeEndpoint.request(MediaType.APPLICATION_JSON).get()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER, response.readEntity(ErrorResponseImpl.class).getTitle());
        }

        configTypesEndpoint = getConfigEndpoint(exp.getUUID().toString(), TestDataProviderFactory.ID);
        singleTypeEndpoint = configTypesEndpoint.path(UNKNOWN_TYPE_ID);
        try (Response response = singleTypeEndpoint.request(MediaType.APPLICATION_JSON).get()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_CONFIGURATION_TYPE, response.readEntity(ErrorResponseImpl.class).getTitle());
        }
    }

    /**
     * Tests the creation and deletion of the derived data providers
     *
     * @throws URISyntaxException
     *             if such error occurs
     * @throws IOException
     *             if such error occurs
     */
    @Test
    public void testCreationDeletionOfDerivedDataProviders() throws IOException, URISyntaxException {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        WebTarget dpCreationEndpoint = getDpCreationEndpoint(exp.getUUID().toString(), TestDataProviderFactory.ID);
        Map<String, Object> params = readParametersFromJson(VALID_JSON_FILENAME);
        ITmfConfiguration configuration = new TmfConfiguration.Builder()
                .setName(CONFIG_NAME)
                .setDescription(CONFIG_DESCRIPTION)
                .setSourceTypeId(TestSchemaConfigurationSource.TYPE.getId())
                .setParameters(params)
                .build();

        // Successful creation
        DataProviderDescriptorStub derivedDp = assertDpPost(dpCreationEndpoint, configuration);
        assertNotNull(derivedDp);

        // Successful deletion
        WebTarget dpDeletionEndpoint = dpCreationEndpoint.path(derivedDp.getId());
        try (Response response = dpDeletionEndpoint.request().delete()) {
            assertNotNull(response);
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            DataProviderDescriptorStub deletedDp = response.readEntity(DataProviderDescriptorStub.class);
            assertEquals(derivedDp, deletedDp);
        }
    }

    /**
     * Tests error cases when creating derived data providers
     *
     * @throws URISyntaxException
     *             if such error occurs
     * @throws IOException
     *             if such error occurs
     */
    @Test
    public void testCreationOfDerivedDataProvidersErrors() throws IOException, URISyntaxException {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        WebTarget dpCreationEndpoint = getDpCreationEndpoint(UNKNOWN_EXP_UUID, TestDataProviderFactory.ID);
        Map<String, Object> params = readParametersFromJson(VALID_JSON_FILENAME);

        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setName(CONFIG_NAME)
                .setDescription(CONFIG_DESCRIPTION)
                .setSourceTypeId(TestSchemaConfigurationSource.TYPE.getId())
                .setParameters(params);
        ITmfConfiguration configuration = builder.build();

        // Unknown experiment
        try (Response response = assertDpPostWithErrors(dpCreationEndpoint, configuration)) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_TRACE, response.readEntity(ErrorResponseImpl.class).getTitle());
        }

        // Unknown data provider
        dpCreationEndpoint = getDpCreationEndpoint(exp.getUUID().toString(), UNKNOWN_DP_ID);
        try (Response response = assertDpPostWithErrors(dpCreationEndpoint, configuration)) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER + ": " + UNKNOWN_DP_ID, response.readEntity(ErrorResponseImpl.class).getTitle());
        }

        // Test config type is not applicable for another data provider
        dpCreationEndpoint = getDpCreationEndpoint(exp.getUUID().toString(), CALL_STACK_DATAPROVIDER_ID);
        try (Response response = assertDpPostWithErrors(dpCreationEndpoint, configuration)) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER, response.readEntity(ErrorResponseImpl.class).getTitle());
        }

        // Invalid config type ID
        dpCreationEndpoint = getDpCreationEndpoint(exp.getUUID().toString(), TestDataProviderFactory.ID);
        builder = new TmfConfiguration.Builder()
                .setName(CONFIG_NAME)
                .setDescription(CONFIG_DESCRIPTION)
                .setSourceTypeId(UNKNOWN_TYPE_ID)
                .setParameters(params);

        configuration = builder.build();
        try (Response response = assertDpPostWithErrors(dpCreationEndpoint, configuration)) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_CONFIGURATION_TYPE, response.readEntity(ErrorResponseImpl.class).getTitle());
        }
    }

    /**
     * Tests error cases when deleting derived data providers
     *
     * @throws URISyntaxException
     *             if such error occurs
     * @throws IOException
     *             if such error occurs
     */
    @Test
    public void testDeletionOfDerivedDataProvidersErrors() throws IOException, URISyntaxException {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        // Unknown experiment
        WebTarget dpCreationEndpoint = getDpCreationEndpoint(UNKNOWN_EXP_UUID, TestDataProviderFactory.ID);
        WebTarget dpDeletionEndpoint = dpCreationEndpoint.path(UNKNOWN_DP_ID);
        try (Response response = dpDeletionEndpoint.request().delete()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_TRACE, response.readEntity(ErrorResponseImpl.class).getTitle());
        }

        // Unknown input data provider
        dpCreationEndpoint = getDpCreationEndpoint(exp.getUUID().toString(), UNKNOWN_DP_ID);
        dpDeletionEndpoint = dpCreationEndpoint.path(UNKNOWN_DP_ID);
        try (Response response = dpDeletionEndpoint.request().delete()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER + ": " + UNKNOWN_DP_ID, response.readEntity(ErrorResponseImpl.class).getTitle());
        }

        // Unknown derived data provider
        dpCreationEndpoint = getDpCreationEndpoint(exp.getUUID().toString(), TestDataProviderFactory.ID);
        dpDeletionEndpoint = dpCreationEndpoint.path(UNKNOWN_DP_ID);
        try (Response response = dpDeletionEndpoint.request().delete()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_DERIVED_PROVIDER + ": " + UNKNOWN_DP_ID, response.readEntity(ErrorResponseImpl.class).getTitle());
        }

        Map<String, Object> params = readParametersFromJson(VALID_JSON_FILENAME);
        ITmfConfiguration configuration = new TmfConfiguration.Builder()
                .setName("My name")
                .setDescription("My Description")
                .setSourceTypeId(TestSchemaConfigurationSource.TYPE.getId())
                .setParameters(params)
                .build();

        // Successful creation
        DataProviderDescriptorStub derivedDp = assertDpPost(dpCreationEndpoint, configuration);
        assertNotNull(derivedDp);

        // Test config type is not applicable for another data provider
        dpCreationEndpoint = getDpCreationEndpoint(exp.getUUID().toString(), CALL_STACK_DATAPROVIDER_ID);
        dpDeletionEndpoint = dpCreationEndpoint.path(derivedDp.getId());
        try (Response response = dpDeletionEndpoint.request().delete()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER, response.readEntity(ErrorResponseImpl.class).getTitle());
        }

        // Successful deletion
        dpCreationEndpoint = getDpCreationEndpoint(exp.getUUID().toString(), TestDataProviderFactory.ID);
        dpDeletionEndpoint = dpCreationEndpoint.path(derivedDp.getId());
        try (Response response = dpDeletionEndpoint.request().delete()) {
            assertNotNull(response);
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
        }
    }

}
