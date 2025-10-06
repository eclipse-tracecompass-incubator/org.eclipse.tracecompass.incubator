/*******************************************************************************
 * Copyright (c) 2023, 2024 Ericsson
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response.Status;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.ConfigurationManagerService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.config.TestSchemaConfigurationSource.Parameters;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.NewRestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ConfigurationsApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.XyApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ConfigurationParameterDescriptor;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ConfigurationQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ConfigurationSourceType;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ModelConfiguration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYTreeResponse;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.Bundle;

import com.google.gson.Gson;

/**
 * Basic test for the {@link ConfigurationManagerService}.
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings("restriction")
public class ConfigurationManagerServiceTest extends NewRestServerTest {

    private static final Bundle XML_CORE_TESTS = Platform.getBundle("org.eclipse.tracecompass.tmf.analysis.xml.core.tests");

    private static final String UNKNOWN_TYPE = "test-test-test";
    private static final String PATH_INVALID = "test_xml_files/test_invalid/";
    private static final String PATH_VALID = "test_xml_files/test_valid/";
    private static final String PATH = "path";
    private static final String XML_ANALYSIS_TYPE_ID = "org.eclipse.tracecompass.tmf.core.config.xmlsourcetype";
    private static final String CONFIG_WITH_SCHEMA_ANALYSIS_TYPE_ID = "org.eclipse.tracecompass.tmf.core.config.testschemasourcetype";
    private static final String EXPECTED_TYPE_NAME = "XML Data-driven analyses"; //$NON-NLS-1$
    private static final String EXPECTED_TYPE_DESCRIPTION = "Data-driven analyses described in XML"; //$NON-NLS-1$
    private static final String EXPECTED_KEY_NAME = "path";
    private static final String EXPECTED_PARAM_DESCRIPTION = "URI to XML analysis file";
    private static final String EXPECTED_DATA_TYPE = "STRING";
    private static String VALID_NAME = "test_valid";
    private static String VALID_XML_FILE = VALID_NAME + ".xml";
    private static String INVALID_XML_FILE = "test_invalid.xml";
    private static final String EXPECTED_CONFIG_NAME = VALID_NAME;
    private static final String EXPECTED_CONFIG_ID = VALID_XML_FILE;
    private static final String EXPECTED_CONFIG_DESCRIPTION = "XML Data-driven analysis: " + VALID_NAME;
    private static final String PATH_TO_INVALID_PATH = getPath(PATH_INVALID + INVALID_XML_FILE);
    private static final String PATH_TO_VALID_PATH = getPath(PATH_VALID + VALID_XML_FILE);

    private static final String EXPECTED_JSON_CONFIG_NAME = "My Config Name";
    private static final String EXPECTED_JSON_CONFIG_DESCRIPTION = "My Config Description";
    private static final String EXPECTED_JSON_CONFIG_ID = "My Config Id";

    private static final ConfigurationsApi sfConfigApi = new ConfigurationsApi(sfApiClient);

    /**
     * Empty the XML directory after the test
     */
    @After
    public void emptyXmlFolder() {
        File fFolder = XmlUtils.getXmlFilesPath().toFile();
        if (!(fFolder.isDirectory() && fFolder.exists())) {
            return;
        }
        for (File xmlFile : fFolder.listFiles()) {
            xmlFile.delete();
        }
    }

    /**
     * Test getting configuration source types and verify existing XML source
     * type
     *
     * @throws ApiException
     *             if such exception occurred
     */
    @Test
    public void testSourceType() throws ApiException {
        List<ConfigurationSourceType> configurations = sfConfigApi.getConfigurationTypes();
        assertTrue(configurations.size() > 0);
        Optional<ConfigurationSourceType> optional = configurations.stream().filter(config -> config.getId().equals(XML_ANALYSIS_TYPE_ID)).findAny();
        assertTrue(optional.isPresent());

        ConfigurationSourceType type = optional.get();
        assertEquals(XML_ANALYSIS_TYPE_ID, type.getId());
        assertEquals(EXPECTED_TYPE_NAME, type.getName());
        assertEquals(EXPECTED_TYPE_DESCRIPTION, type.getDescription());
        List<ConfigurationParameterDescriptor> descriptors = type.getParameterDescriptors();
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        ConfigurationParameterDescriptor desc = descriptors.get(0);
        assertEquals(EXPECTED_KEY_NAME, desc.getKeyName());
        assertEquals(EXPECTED_DATA_TYPE, desc.getDataType());
        assertEquals(EXPECTED_PARAM_DESCRIPTION, desc.getDescription());
        assertTrue(desc.getRequired());

        ConfigurationSourceType sourceStub = sfConfigApi.getConfigurationType(XML_ANALYSIS_TYPE_ID);
        assertEquals(XML_ANALYSIS_TYPE_ID, sourceStub.getId());
        assertEquals(EXPECTED_TYPE_NAME, sourceStub.getName());
        assertEquals(EXPECTED_TYPE_DESCRIPTION, sourceStub.getDescription());
        descriptors = sourceStub.getParameterDescriptors();
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        desc = descriptors.get(0);
        assertEquals(EXPECTED_KEY_NAME, desc.getKeyName());
        assertEquals(EXPECTED_DATA_TYPE, desc.getDataType());
        assertEquals(EXPECTED_PARAM_DESCRIPTION, desc.getDescription());
        assertTrue(desc.getRequired());

        // Verify configuration source type with schema
        Optional<ConfigurationSourceType> optional2 = configurations.stream().filter(config -> config.getId().equals("org.eclipse.tracecompass.tmf.core.config.testschemasourcetype")).findAny();
        assertTrue(optional2.isPresent());
        ConfigurationSourceType type2 = optional2.get();
        Object schema = type2.getSchema();
        // Verify that schema exists
        assertNotNull(schema);
    }

    /**
     * Test POST to create configurations using XML configuration source type.
     *
     * @throws ApiException
     *             if such error occurred
     */
    @Test
    public void testCreateGetAndDelete() throws ApiException {
        // Missing path
        try {
            createConfig(null);
        } catch (ApiException ex) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("Missing path", errorResponse.getTitle());
            assertEquals("No XML configuration should exists because no path provided", 0, getConfigurations().size());
        }

        // XML file doesn't exists
        try {
            createConfig(PATH_TO_INVALID_PATH + UNKNOWN_TYPE);
        } catch (ApiException ex) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("An error occurred while validating the XML file.", errorResponse.getTitle());
            assertEquals("No XML configuration should exists because xml file doesn't exists", 0, getConfigurations().size());
        }

        // Invalid XML file
        try {
            createConfig(PATH_TO_INVALID_PATH);
        } catch (ApiException ex) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertTrue(errorResponse.getTitle().contains("XML Parsing error"));
            assertEquals("No XML configuration should exists duo to invalid XML file", 0, getConfigurations().size());
        }

        // Unknown type
        try {
            createConfig(PATH_TO_VALID_PATH, false);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("Configuration source type doesn't exist", errorResponse.getTitle());
            assertEquals("No XML configuration should exists due to unknown configuration source type", 0, getConfigurations().size());
        }

        // Valid XML file
        ModelConfiguration config = createConfig(PATH_TO_VALID_PATH);
        validateConfig(config);

        List<ModelConfiguration> configurations = getConfigurations();
        assertEquals("Valid XML configuration should be added", 1, configurations.size());
        assertTrue("XML configuration instance should exist", configurations.stream().anyMatch(conf -> conf.getId().equals(VALID_XML_FILE)));

        config = getConfiguration(VALID_XML_FILE);
        assertNotNull(config);
        assertTrue("XML configuration instance should exist", config.getId().equals(VALID_XML_FILE));

        ModelConfiguration delConfig = deleteConfig(EXPECTED_CONFIG_ID);
        assertEquals(config, delConfig);
    }

    /**
     * Test POST to create configurations using a schema.
     *
     * @throws IOException
     *             if exception occurs
     * @throws URISyntaxException
     *             if exception occurs
     * @throws ApiException
     *             if such exception occurred
     */
    @Test
    public void testCreateGetAndDeleteSchema() throws URISyntaxException, IOException, ApiException {
        ModelConfiguration config = createJsonConfig(VALID_JSON_FILENAME);
        assertNotNull(config);
        validateJsonConfig(config);

        List<ModelConfiguration> configurations = getConfigurations(CONFIG_WITH_SCHEMA_ANALYSIS_TYPE_ID);
        assertEquals("Valid JSON configuration should be added", 1, configurations.size());
        assertTrue("Valid configuration instance should exist", configurations.stream().anyMatch(conf -> conf.getName().equals(EXPECTED_JSON_CONFIG_NAME)));

        ModelConfiguration config2 = getConfiguration(CONFIG_WITH_SCHEMA_ANALYSIS_TYPE_ID, configurations.get(0).getId());
        assertNotNull(config2);
        assertTrue("JSON configuration instance should exist", config2.getId().equals(EXPECTED_JSON_CONFIG_ID));

        ModelConfiguration delConfig = deleteConfig(CONFIG_WITH_SCHEMA_ANALYSIS_TYPE_ID, config.getId());
        assertNotNull(delConfig);
        assertEquals("JSON configuration should have been deleted", 0, getConfigurations(CONFIG_WITH_SCHEMA_ANALYSIS_TYPE_ID).size());
    }

    /**
     * Test PUT to update configurations using XML configuration source type.
     *
     * @throws ApiException
     *             if such exception occurred
     */
    @Test
    public void testUpdate() throws ApiException {
        // Valid XML file
        ModelConfiguration config = createConfig(PATH_TO_VALID_PATH);
        assertNotNull(config);

        // Missing path
        try {
            updateConfig(null, EXPECTED_CONFIG_ID);
        } catch (ApiException ex) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("Missing path", errorResponse.getTitle());
        }

        // XML file doesn't exists
        try {
            updateConfig(PATH_TO_INVALID_PATH + UNKNOWN_TYPE, EXPECTED_CONFIG_ID);
        } catch (ApiException ex) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("An error occurred while validating the XML file.", errorResponse.getTitle());
        }

        // Invalid XML file
        try {
            updateConfig(PATH_TO_INVALID_PATH, EXPECTED_CONFIG_ID);
        } catch (ApiException ex) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertTrue(errorResponse.getTitle().contains("XML Parsing error"));
        }

        // Unknown type
        try {
            updateConfig(PATH_TO_INVALID_PATH, EXPECTED_CONFIG_ID, false);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("Configuration source type doesn't exist", errorResponse.getTitle());
        }

        // Unknown config
        try {
            updateConfig(PATH_TO_INVALID_PATH, EXPECTED_CONFIG_ID, true, false);
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("Configuration instance doesn't exist for type org.eclipse.tracecompass.tmf.core.config.xmlsourcetype", errorResponse.getTitle());
        }

        // Valid XML file
        config = updateConfig(PATH_TO_VALID_PATH, EXPECTED_CONFIG_ID);
        validateConfig(config);

        List<ModelConfiguration> configurations = getConfigurations();
        assertEquals("Valid XML configuration should be added", 1, configurations.size());
        assertTrue("XML configuration instance should exist", configurations.stream().anyMatch(conf -> conf.getId().equals(VALID_XML_FILE)));

        ModelConfiguration delConfig = deleteConfig(EXPECTED_CONFIG_ID);
        assertNotNull(delConfig);
        assertEquals("XML configuration should have been deleted", 0, getConfigurations().size());
    }

    /**
     * Test create and created XML data providers.
     * @throws ApiException if such exception occurred
     */
    @Test
    public void testXmlDataProvider() throws ApiException {
        ModelConfiguration config = createConfig(PATH_TO_VALID_PATH);
        validateConfig(config);

        Experiment exp = assertPostExperiment(sfContextSwitchesKernelNotInitializedStub.getName(), sfContextSwitchesKernelNotInitializedStub);
        XyApi xyApi = new XyApi(sfApiClient);

        TreeParameters params = new TreeParameters();
        TreeQueryParameters queryParams = new TreeQueryParameters().parameters(params);

        XYTreeResponse xmlTree = xyApi.getXYTree(exp.getUUID(), "org.eclipse.linuxtools.tmf.analysis.xml.core.tests.xy", queryParams);
        assertTrue("The endpoint for the XML data provider should be available", !xmlTree.getStatus().equals(XYTreeResponse.StatusEnum.FAILED));

        ModelConfiguration deletedConfig = deleteConfig(EXPECTED_CONFIG_ID);
        assertEquals(config, deletedConfig);
        assertEquals("XML configuration should have been deleted", 0, getConfigurations().size());

        try {
            xmlTree = xyApi.getXYTree(exp.getUUID(), "org.eclipse.linuxtools.tmf.analysis.xml.core.tests.xy", queryParams);
        } catch (ApiException ex) {
            assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_PROVIDER, errorResponse.getTitle());
        }
    }

    private static @NonNull String getPath(String bundlePath) {
        assertNotNull(XML_CORE_TESTS);
        URL location = FileLocator.find(XML_CORE_TESTS, new Path(bundlePath), null);
        String path = null;
        try {
            path = FileLocator.toFileURL(location).toURI().getPath();
        } catch (URISyntaxException | IOException e) {
            fail(e.toString());
        }
        assertNotNull(path);
        return path;
    }

    private static ModelConfiguration createConfig(String path) throws ApiException {
        return createConfig(path, true);
    }

    private static ModelConfiguration createConfig(String path, boolean isCorrectType) throws ApiException {
        String typeId = XML_ANALYSIS_TYPE_ID;
        if (!isCorrectType) {
            typeId = UNKNOWN_TYPE;
        }

        Map<String, Object> parameters = new HashMap<>();
        if (path != null) {
            parameters.put(PATH, path);
        }

        ConfigurationQueryParameters queryParameters = new ConfigurationQueryParameters()
                .name("ignored")
                .description("ignored")
                .parameters(parameters);

        return sfConfigApi.postConfiguration(typeId, queryParameters);
    }

    private static ModelConfiguration createJsonConfig(String jsonFileName) throws URISyntaxException, IOException, ApiException {
        String typeId = CONFIG_WITH_SCHEMA_ANALYSIS_TYPE_ID;
        Map<String, Object> params = readParametersFromJson(jsonFileName);
        ConfigurationQueryParameters queryParameters = new ConfigurationQueryParameters()
                .name("ignored")
                .description("ignored")
                .parameters(params);
        return sfConfigApi.postConfiguration(typeId, queryParameters);
    }

    private static ModelConfiguration updateConfig(String path, String id) throws ApiException {
        return updateConfig(path, id, true, true);
    }

    private static ModelConfiguration updateConfig(String path, String id, boolean isCorrectType) throws ApiException {
        return updateConfig(path, id, isCorrectType, true);
    }

    private static ModelConfiguration updateConfig(String path, String id, boolean isCorrectType, boolean isCorrectId) throws ApiException {
        String typeId = XML_ANALYSIS_TYPE_ID;
        if (!isCorrectType) {
            typeId = UNKNOWN_TYPE;
        }
        String localId = id;
        if (!isCorrectId) {
            localId = UNKNOWN_TYPE;
        }
        Map<String, Object> parameters = new HashMap<>();
        if (path != null) {
            parameters.put(PATH, path);
        }

        ConfigurationQueryParameters queryParameters = new ConfigurationQueryParameters()
                .name("ignored")
                .description("ignored")
                .parameters(parameters);

        return sfConfigApi.putConfiguration(typeId, localId, queryParameters);
    }

    private static ModelConfiguration deleteConfig(String id) throws ApiException {
        return deleteConfig(null, id);
    }

    private static ModelConfiguration deleteConfig(String type, String id) throws ApiException {

        String requestType = type;
        if (requestType == null) {
            requestType = XML_ANALYSIS_TYPE_ID;
        }
        return sfConfigApi.deleteConfiguration(requestType, id);
    }

    private static List<ModelConfiguration> getConfigurations() throws ApiException {
        return getConfigurations(null);
    }

    private static List<ModelConfiguration> getConfigurations(@Nullable String type) throws ApiException {
        String requestType = type;
        if (requestType == null) {
            requestType = XML_ANALYSIS_TYPE_ID;
        }
        return sfConfigApi.getConfigurations(requestType);
    }

    private static ModelConfiguration getConfiguration(String configId) throws ApiException {
        return getConfiguration(null, configId);
    }

    private static ModelConfiguration getConfiguration(String type, String configId) throws ApiException {
        String requestType = type;
        if (requestType == null) {
            requestType = XML_ANALYSIS_TYPE_ID;
        }
        return sfConfigApi.getConfiguration(requestType, configId);
    }

    private static void validateConfig(ModelConfiguration config) {
        assertNotNull(config);
        assertEquals(EXPECTED_CONFIG_NAME, config.getName());
        assertEquals(EXPECTED_CONFIG_ID, config.getId());
        assertEquals(XML_ANALYSIS_TYPE_ID, config.getSourceTypeId());
        assertEquals(EXPECTED_CONFIG_DESCRIPTION, config.getDescription());
        assertTrue(config.getParameters().isEmpty());
    }

    @SuppressWarnings("null")
    private static void validateJsonConfig(ModelConfiguration config) {
        assertEquals(EXPECTED_JSON_CONFIG_NAME, config.getName());
        assertEquals(EXPECTED_JSON_CONFIG_ID, config.getId());
        assertEquals(CONFIG_WITH_SCHEMA_ANALYSIS_TYPE_ID, config.getSourceTypeId());
        assertEquals(EXPECTED_JSON_CONFIG_DESCRIPTION, config.getDescription());
        Map<String, Object> parameters = config.getParameters();
        assertNotNull(parameters);
        String json = new Gson().toJson(parameters);
        Parameters paramObj = new Gson().fromJson(json, Parameters.class);
        assertNotNull(paramObj);
        assertNotNull(paramObj.getCpus());
        assertEquals(3, paramObj.getCpus().size());
        assertEquals("(123)345-567", paramObj.getPhone());
        assertEquals("my-thread", paramObj.getThread());
        assertNull(paramObj.getLabel());
    }
}
