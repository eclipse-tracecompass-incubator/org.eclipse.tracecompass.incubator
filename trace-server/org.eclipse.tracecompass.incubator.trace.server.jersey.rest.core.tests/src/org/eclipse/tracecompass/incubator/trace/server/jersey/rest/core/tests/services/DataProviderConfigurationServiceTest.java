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
import java.util.UUID;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.ErrorResponseImpl;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TestDataProviderFactory;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.config.TestSchemaConfigurationSource;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.OutputConfigurationsApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ConfigurationSourceType;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.DataProvider;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputConfigurationQueryParameters;
import org.junit.Test;

/**
 * Test the {@link DataProviderService} class with focus on data provider
 * configuration endpoints
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings("null")
public class DataProviderConfigurationServiceTest extends RestServerTest {
    private static final String UNKNOWN_TYPE_ID = "unknown.config.type.id";

    private static final String CONFIG_NAME = "My configuration";
    private static final String CONFIG_DESCRIPTION = "My description";

    private static final OutputConfigurationsApi sfConfigApi = new OutputConfigurationsApi(sfApiClient);

    /**
     * Tests querying the data provider config types
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testDataProviderConfigTypes() throws ApiException {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        // Get all config types
        List<ConfigurationSourceType> configTypesListModel = sfConfigApi.getConfigurationTypes1(exp.getUUID(), TestDataProviderFactory.ID);
        assertNotNull(configTypesListModel);
        assertTrue(configTypesListModel.size() == 1);
        Optional<ConfigurationSourceType> optional = configTypesListModel.stream().filter(config -> config.getId().equals(TestSchemaConfigurationSource.TYPE.getId())).findAny();
        assertTrue(optional.isPresent());

        // Test Valid config type ID
        ConfigurationSourceType singleConfigType = sfConfigApi.getConfigurationType1(exp.getUUID(), TestDataProviderFactory.ID, optional.get().getId());
        assertNotNull(singleConfigType);
        assertEquals(optional.get().getId(), singleConfigType.getId());

        // Test config types for data provider that can't be configured
        List<ConfigurationSourceType> emptyConfigTypesListModel = sfConfigApi.getConfigurationTypes1(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID);
        assertNotNull(emptyConfigTypesListModel);
        assertTrue(emptyConfigTypesListModel.isEmpty());
    }

    /**
     * Tests error cases when querying data provider config types
     */
    @Test
    public void testDataProviderConfigTypesErrors() {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        WebTarget configTypesEndpoint = getConfigEndpoint(UNKNOWN_EXP_UUID, TestDataProviderFactory.ID);

        // Unknown experiment (not uuid string)
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

        // Unknown experiment
        try {
            sfConfigApi.getConfigurationTypes1(UUID.randomUUID(), TestDataProviderFactory.ID);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_TRACE, errorResponse.getTitle());
        }

        // Unknown experiment
        try {
            sfConfigApi.getConfigurationType1(UUID.randomUUID(), TestDataProviderFactory.ID, TestSchemaConfigurationSource.TYPE.getId());
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_TRACE, errorResponse.getTitle());
        }

        try {
            sfConfigApi.getConfigurationType1(exp.getUUID(), TestDataProviderFactory.ID, TestSchemaConfigurationSource.TYPE.getId());
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_TRACE, errorResponse.getTitle());
        }

        // Unknown data provider
        try {
            sfConfigApi.getConfigurationTypes1(exp.getUUID(), UNKNOWN_DP_ID);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER, errorResponse.getTitle());
        }

        try {
            sfConfigApi.getConfigurationType1(exp.getUUID(), UNKNOWN_DP_ID, TestSchemaConfigurationSource.TYPE.getId());
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER, errorResponse.getTitle());
        }

        // Test config type is not applicable for another data provider
        try {
            sfConfigApi.getConfigurationType1(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID, TestSchemaConfigurationSource.TYPE.getId());
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER, errorResponse.getTitle());
        }

        try {
            sfConfigApi.getConfigurationType1(exp.getUUID(), TestDataProviderFactory.ID, UNKNOWN_TYPE_ID);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_CONFIGURATION_TYPE, errorResponse.getTitle());
        }
    }

    /**
     * Tests the creation and deletion of the derived data providers
     *
     * @throws URISyntaxException
     *             if such error occurs
     * @throws IOException
     *             if such error occurs
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testCreationDeletionOfDerivedDataProviders() throws IOException, URISyntaxException, ApiException {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        Map<String, Object> params = readParametersFromJson(VALID_JSON_FILENAME);

        OutputConfigurationQueryParameters queryParams = new OutputConfigurationQueryParameters()
                .name(CONFIG_NAME)
                .description(CONFIG_DESCRIPTION)
                .sourceTypeId(TestSchemaConfigurationSource.TYPE.getId())
                .parameters(params);

        // Successful creation
        DataProvider derivedDp = assertDpPost(exp.getUUID(), TestDataProviderFactory.ID, queryParams);
        assertNotNull(derivedDp);

        // Successful deletion
        DataProvider deletedDp = sfConfigApi.deleteDerivedProvider(exp.getUUID(), TestDataProviderFactory.ID, derivedDp.getId());
        assertNotNull(deletedDp);
        assertEquals(derivedDp, deletedDp);
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
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        Map<String, Object> params = readParametersFromJson(VALID_JSON_FILENAME);

        OutputConfigurationQueryParameters queryParams = new OutputConfigurationQueryParameters()
                .name(CONFIG_NAME)
                .description(CONFIG_DESCRIPTION)
                .sourceTypeId(TestSchemaConfigurationSource.TYPE.getId())
                .parameters(params);

        // Unknown experiment
        try {
            sfConfigApi.createProvider(UUID.randomUUID(), TestDataProviderFactory.ID, queryParams);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_TRACE, errorResponse.getTitle());
        }

        // Unknown data provider
        try {
            sfConfigApi.createProvider(exp.getUUID(), UNKNOWN_DP_ID, queryParams);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER + ": " + UNKNOWN_DP_ID, errorResponse.getTitle());
        }

        // Test config type is not applicable for another data provider
        try {
            sfConfigApi.createProvider(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID, queryParams);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER, errorResponse.getTitle());
        }

        // Invalid config type ID
        try {
            queryParams.sourceTypeId(UNKNOWN_TYPE_ID);
            sfConfigApi.createProvider(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID, queryParams);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER, errorResponse.getTitle());
        }
    }

    /**
     * Tests error cases when deleting derived data providers
     *
     * @throws URISyntaxException
     *             if such error occurs
     * @throws IOException
     *             if such error occurs
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testDeletionOfDerivedDataProvidersErrors() throws IOException, URISyntaxException, ApiException {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        // Unknown experiment
        try {
            sfConfigApi.deleteDerivedProvider(UUID.randomUUID(), TestDataProviderFactory.ID, UNKNOWN_DP_ID);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_TRACE, errorResponse.getTitle());
        }

        // Unknown input data provider
        try {
            sfConfigApi.deleteDerivedProvider(exp.getUUID(), UNKNOWN_DP_ID, UNKNOWN_DP_ID);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER + ": " + UNKNOWN_DP_ID, errorResponse.getTitle());
        }

        // Unknown derived data provider
        try {
            sfConfigApi.deleteDerivedProvider(exp.getUUID(), TestDataProviderFactory.ID, UNKNOWN_DP_ID);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_DERIVED_PROVIDER + ": " + UNKNOWN_DP_ID, errorResponse.getTitle());
        }

        Map<String, Object> params = readParametersFromJson(VALID_JSON_FILENAME);

        // Successful creation
        OutputConfigurationQueryParameters queryParams = new OutputConfigurationQueryParameters()
                .name("My name")
                .description("My Description")
                .sourceTypeId(TestSchemaConfigurationSource.TYPE.getId())
                .parameters(params);

        DataProvider derivedDp = assertDpPost(exp.getUUID(), TestDataProviderFactory.ID, queryParams);
        assertNotNull(derivedDp);

        // Test config type is not applicable for another data provider
        try {
            sfConfigApi.deleteDerivedProvider(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID, derivedDp.getId());
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_PROVIDER, errorResponse.getTitle());
        }

        // Successful deletion
        DataProvider delProvider = sfConfigApi.deleteDerivedProvider(exp.getUUID(), TestDataProviderFactory.ID, derivedDp.getId());
        assertNotNull(delProvider);
        assertEquals(derivedDp, delProvider);
    }

    /**
     * @param expUuid
     *            thr experiment to create a derived data provider
     * @param dpId
     *            the data provider to create a derived data provider
     * @param queryParams
     *            the configuration with input parameters to post
     * @return the derived data provider descriptor stub
     * @throws ApiException
     *             if such exception occurs
     */
    private static DataProvider assertDpPost(UUID expUuid, String dpId, OutputConfigurationQueryParameters queryParams) throws ApiException {
        DataProvider result = sfConfigApi.createProvider(expUuid, TestDataProviderFactory.ID, queryParams);
        assertEquals(queryParams.getName(), result.getConfiguration().getName());
        assertEquals(queryParams.getDescription(), result.getConfiguration().getDescription());
        assertEquals(queryParams.getSourceTypeId(), result.getConfiguration().getSourceTypeId());
        assertEquals(queryParams.getParameters(), result.getConfiguration().getParameters());
        return result;
    }
}
