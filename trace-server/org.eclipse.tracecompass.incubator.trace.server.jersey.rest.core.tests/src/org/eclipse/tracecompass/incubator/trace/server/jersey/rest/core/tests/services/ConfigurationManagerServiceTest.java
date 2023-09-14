/*******************************************************************************
 * Copyright (c) 2023 Ericsson
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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.ConfigurationManagerService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ExperimentModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TmfConfigurationSourceTypeStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TmfConfigurationStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Basic test for the {@link ConfigurationManagerService}.
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings("restriction")
public class ConfigurationManagerServiceTest extends RestServerTest {

    private static final Bundle XML_CORE_TESTS = Platform.getBundle("org.eclipse.tracecompass.tmf.analysis.xml.core.tests");

    private static final String UNKNOWN_TYPE = "test-test-test";
    private static final String PATH_INVALID = "test_xml_files/test_invalid/";
    private static final String PATH_VALID = "test_xml_files/test_valid/";
    private static final String PATH = "path";
    private static final String XML_ANALYSIS_TYPE_ID = "org.eclipse.tracecompass.tmf.core.config.xmlsourcetype";
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

    private static final GenericType<TmfConfigurationStub> CONFIGURATION = new GenericType<>() { };
    private static final GenericType<List<TmfConfigurationStub>> LIST_CONFIGURATION_TYPE = new GenericType<>() { };
    private static final GenericType<TmfConfigurationSourceTypeStub> CONFIGURATION_SOURCE = new GenericType<>() { };
    private static final GenericType<List<TmfConfigurationSourceTypeStub>> LIST_CONFIGURATION_SOURCE_TYPE = new GenericType<>() { };

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
     */
    @Test
    public void testSourceType() {
        WebTarget endpoint = getApplicationEndpoint()
                .path(CONFIG_PATH)
                .path(TYPES_PATH);

        List<TmfConfigurationSourceTypeStub> configurations = endpoint.request().get(LIST_CONFIGURATION_SOURCE_TYPE);
        assertTrue(configurations.size() > 0);
        Optional<TmfConfigurationSourceTypeStub> optional = configurations.stream().filter(config -> config.getId().equals(XML_ANALYSIS_TYPE_ID)).findAny();
        assertTrue(optional.isPresent());

        ITmfConfigurationSourceType type = optional.get();
        assertEquals(XML_ANALYSIS_TYPE_ID, type.getId());
        assertEquals(EXPECTED_TYPE_NAME, type.getName());
        assertEquals(EXPECTED_TYPE_DESCRIPTION, type.getDescription());
        List<ITmfConfigParamDescriptor> descriptors = type.getConfigParamDescriptors();
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        ITmfConfigParamDescriptor desc = descriptors.get(0);
        assertEquals(EXPECTED_KEY_NAME, desc.getKeyName());
        assertEquals(EXPECTED_DATA_TYPE, desc.getDataType());
        assertEquals(EXPECTED_PARAM_DESCRIPTION, desc.getDescription());
        assertTrue(desc.isRequired());

        endpoint = getApplicationEndpoint()
                .path(CONFIG_PATH)
                .path(TYPES_PATH)
                .path(XML_ANALYSIS_TYPE_ID);
        TmfConfigurationSourceTypeStub sourceStub = endpoint.request().get(CONFIGURATION_SOURCE);
        assertEquals(XML_ANALYSIS_TYPE_ID, sourceStub.getId());
        assertEquals(EXPECTED_TYPE_NAME, sourceStub.getName());
        assertEquals(EXPECTED_TYPE_DESCRIPTION, sourceStub.getDescription());
        descriptors = sourceStub.getConfigParamDescriptors();
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        desc = descriptors.get(0);
        assertEquals(EXPECTED_KEY_NAME, desc.getKeyName());
        assertEquals(EXPECTED_DATA_TYPE, desc.getDataType());
        assertEquals(EXPECTED_PARAM_DESCRIPTION, desc.getDescription());
        assertTrue(desc.isRequired());
    }

    /**
     * Test POST to create configurations using XML configuration source type.
     */
    @Test
    public void testCreateGetAndDelete() {
        // Missing path
        try (Response response = createConfig(null) ) {
            assertEquals(400, response.getStatus());
            assertEquals("No XML configuration should exists because no path provided", 0, getConfigurations().size());
        }

        // XML file doesn't exists
        try (Response response = createConfig(PATH_TO_INVALID_PATH + UNKNOWN_TYPE) ) {
            assertEquals(400, response.getStatus());
            assertEquals("No XML configuration should exists because xml file doesn't exists", 0, getConfigurations().size());
        }

        // Invalid XML file
        try (Response response = createConfig(PATH_TO_INVALID_PATH) ) {
            assertEquals(400, response.getStatus());
            assertEquals("No XML configuration should exists duo to invalid XML file", 0, getConfigurations().size());
        }

        // Unknown type
        try (Response response = createConfig(PATH_TO_VALID_PATH, false) ) {
            assertEquals(404, response.getStatus());
            assertEquals("No XML configuration should exists due to unknown configuration source type", 0, getConfigurations().size());
        }

        // Valid XML file
        try (Response response = createConfig(PATH_TO_VALID_PATH)) {
            assertEquals(200, response.getStatus());
            TmfConfigurationStub config = response.readEntity(CONFIGURATION);
            validateConfig(config);
        }

        List<TmfConfigurationStub> configurations = getConfigurations();
        assertEquals("Valid XML configuration should be added", 1, configurations.size());
        assertTrue("XML configuration instance should exist", configurations.stream().anyMatch(conf -> conf.getId().equals(VALID_XML_FILE)));

        TmfConfigurationStub config = getConfiguration(VALID_XML_FILE);
        assertNotNull(config);
        assertTrue("XML configuration instance should exist", config.getId().equals(VALID_XML_FILE));

        try (Response response = deleteConfig(EXPECTED_CONFIG_ID)) {
            assertEquals(200, response.getStatus());
            assertEquals("XML configuration should have been deleted", 0, getConfigurations().size());
        }
    }

    /**
     * Test PUT to update configurations using XML configuration source type.
     */
    @Test
    public void testUpdate() {
        // Valid XML file
        try (Response response = createConfig(PATH_TO_VALID_PATH)) {
            assertEquals(200, response.getStatus());
        }

        // Missing path
        try (Response response = updateConfig(null, EXPECTED_CONFIG_ID)) {
            assertEquals(400, response.getStatus());
        }

        // XML file doesn't exists
        try (Response response = updateConfig(PATH_TO_INVALID_PATH + UNKNOWN_TYPE, EXPECTED_CONFIG_ID)) {
            assertEquals(400, response.getStatus());
        }

        // Invalid XML file
        try (Response response = updateConfig(PATH_TO_INVALID_PATH, EXPECTED_CONFIG_ID)) {
            assertEquals(400, response.getStatus());
        }

        // Unknown type
        try (Response response = updateConfig(PATH_TO_INVALID_PATH, EXPECTED_CONFIG_ID, false)) {
            assertEquals(404, response.getStatus());
        }

        // Unknown config
        try (Response response = updateConfig(PATH_TO_INVALID_PATH, EXPECTED_CONFIG_ID, true, false)) {
            assertEquals(404, response.getStatus());
        }

        // Valid XML file
        try (Response response = updateConfig(PATH_TO_VALID_PATH, EXPECTED_CONFIG_ID)) {
            assertEquals(200, response.getStatus());
            TmfConfigurationStub config = response.readEntity(CONFIGURATION);
            validateConfig(config);
        }

        List<TmfConfigurationStub> configurations = getConfigurations();
        assertEquals("Valid XML configuration should be added", 1, configurations.size());
        assertTrue("XML configuration instance should exist", configurations.stream().anyMatch(conf -> conf.getId().equals(VALID_XML_FILE)));

        try (Response response = deleteConfig(EXPECTED_CONFIG_ID)) {
            assertEquals(200, response.getStatus());
            assertEquals("XML configuration should have been deleted", 0, getConfigurations().size());
        }
    }

    /**
     * Test create and created XML data providers.
     */
    @Test
    public void testXmlDataProvider() {
        try (Response response = createConfig(PATH_TO_VALID_PATH)) {
            assertEquals(200, response.getStatus());
            TmfConfigurationStub config = response.readEntity(CONFIGURATION);
            validateConfig(config);
        }
        ExperimentModelStub exp = assertPostExperiment(CONTEXT_SWITCHES_KERNEL_STUB.getName(), CONTEXT_SWITCHES_KERNEL_STUB);
        WebTarget xmlProviderPath = getXYTreeEndpoint(exp.getUUID().toString(), "org.eclipse.linuxtools.tmf.analysis.xml.core.tests.xy");
        Map<String, Object> parameters = new HashMap<>();
        try (Response xmlTree = xmlProviderPath.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals("The endpoint for the XML data provider should be available", 200, xmlTree.getStatus());
        }

        try (Response response = deleteConfig(EXPECTED_CONFIG_ID)) {
            assertEquals(200, response.getStatus());
            assertEquals("XML configuration should have been deleted", 0, getConfigurations().size());
        }

        try (Response noXmlTree = xmlProviderPath.request(MediaType.APPLICATION_JSON).get()) {
            assertEquals("The endpoint for the XML data provider should not be available anymore",
                405, noXmlTree.getStatus());
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

    private static Response createConfig(String path) {
        return createConfig(path, true);
    }

    private static Response createConfig(String path, boolean isCorrectType) {
        String typeId = XML_ANALYSIS_TYPE_ID;
        if (!isCorrectType) {
            typeId = UNKNOWN_TYPE;
        }
        WebTarget endpoint = getApplicationEndpoint()
                .path(CONFIG_PATH)
                .path(TYPES_PATH)
                .path(typeId)
                .path(CONFIG_INSTANCES_PATH);

        Map<String, Object> parameters = new HashMap<>();
        if (path != null) {
            parameters.put(PATH, path);
        }
        return endpoint.request(MediaType.APPLICATION_JSON)
                .post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
    }

    private static Response updateConfig(String path, String id) {
        return updateConfig(path, id, true, true);
    }

    private static Response updateConfig(String path, String id, boolean isCorrectType) {
        return updateConfig(path, id, isCorrectType, true);
    }

    private static Response updateConfig(String path, String id, boolean isCorrectType, boolean isCorrectId) {
        String typeId = XML_ANALYSIS_TYPE_ID;
        if (!isCorrectType) {
            typeId = UNKNOWN_TYPE;
        }
        String localId = id;
        if (!isCorrectId) {
            localId = UNKNOWN_TYPE;
        }
        WebTarget endpoint = getApplicationEndpoint()
                .path(CONFIG_PATH)
                .path(TYPES_PATH)
                .path(typeId)
                .path(CONFIG_INSTANCES_PATH)
                .path(localId);

        Map<String, Object> parameters = new HashMap<>();
        if (path != null) {
            parameters.put(PATH, path);
        }
        return endpoint.request(MediaType.APPLICATION_JSON)
                .put(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
    }

    private static Response deleteConfig(String id) {
        WebTarget endpoint = getApplicationEndpoint()
                .path(CONFIG_PATH)
                .path(TYPES_PATH)
                .path(XML_ANALYSIS_TYPE_ID)
                .path(CONFIG_INSTANCES_PATH);
        return endpoint.path(id).request().delete();
    }

    private static List<TmfConfigurationStub> getConfigurations() {
        WebTarget endpoint = getApplicationEndpoint()
                .path(CONFIG_PATH)
                .path(TYPES_PATH)
                .path(XML_ANALYSIS_TYPE_ID)
                .path(CONFIG_INSTANCES_PATH);
        return endpoint.request().get(LIST_CONFIGURATION_TYPE);
    }

    private static TmfConfigurationStub getConfiguration(String configId) {
        WebTarget endpoint = getApplicationEndpoint()
                .path(CONFIG_PATH)
                .path(TYPES_PATH)
                .path(XML_ANALYSIS_TYPE_ID)
                .path(CONFIG_INSTANCES_PATH)
                .path(configId);
        return endpoint.request().get(CONFIGURATION);
    }

    private static void validateConfig(ITmfConfiguration config) {
        assertNotNull(config);
        assertEquals(EXPECTED_CONFIG_NAME, config.getName());
        assertEquals(EXPECTED_CONFIG_ID, config.getId());
        assertEquals(XML_ANALYSIS_TYPE_ID, config.getSourceTypeId());
        assertEquals(EXPECTED_CONFIG_DESCRIPTION, config.getDescription());
        assertTrue(config.getParameters().isEmpty());
    }
}
