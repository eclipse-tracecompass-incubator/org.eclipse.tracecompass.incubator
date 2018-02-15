/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.XmlManagerService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Basic test for the {@link XmlManagerService}.
 *
 * @author Loic Prieur-Drevon
 */
public class XmlManagerServiceTest extends RestServerTest {
    private static final Bundle XML_CORE_TESTS = Platform.getBundle("org.eclipse.tracecompass.tmf.analysis.xml.core.tests");
    private static final GenericType<Map<String, String>> MAP_STRING_STRING_TYPE = new GenericType<Map<String, String>>() {
    };

    /**
     * Test that XML files POST to the server correctly, and are accessible for
     * providers
     *
     * @throws URISyntaxException
     *             if this URL is not formatted strictly according to to RFC2396 and
     *             cannot be converted to a URI.
     * @throws IOException
     *             if an error occurs during the URL conversion
     */
    @Test
    public void test() throws URISyntaxException, IOException {
        WebTarget application = getApplicationEndpoint();
        WebTarget xmlEndpoint = application.path("xml");

        String invalidPath = getPath("test_xml_files/test_invalid/test_invalid.xml");
        Response invalidResponse = xmlEndpoint.request(MediaType.APPLICATION_JSON)
                .post(Entity.form(new Form(PATH, invalidPath)));
        assertEquals(500, invalidResponse.getStatus());
        assertEquals("Invalid path should not be added to XML collection",
                0, xmlEndpoint.request().get(MAP_STRING_STRING_TYPE).size());

        String validPath = getPath("test_xml_files/test_valid/test_valid.xml");
        Response validResponse = xmlEndpoint.request(MediaType.APPLICATION_JSON)
                .post(Entity.form(new Form(PATH, validPath)));
        assertEquals(200, validResponse.getStatus());
        Map<String, String> map = xmlEndpoint.request().get(MAP_STRING_STRING_TYPE);
        assertEquals("valid XML should have posted successfully",
                Collections.singleton("test_valid.xml"), map.keySet());

        WebTarget traces = application.path("traces");
        assertPost(traces, CONTEXT_SWITCHES_KERNEL_STUB);

        WebTarget xmlProviderPath = traces.path(CONTEXT_SWITCHES_KERNEL_UUID.toString())
                .path(DataProviderServiceTest.PROVIDERS_PATH)
                // path to the tree XY data provider from the valid XML file
                .path("org.eclipse.linuxtools.tmf.analysis.xml.core.tests.xy")
                .path(DataProviderServiceTest.TREE_PATH);
        Response xmlTree = xmlProviderPath.request(MediaType.APPLICATION_JSON).get();
        assertEquals("The end point for the XML data provider should be available.", 200, xmlTree.getStatus());

        assertEquals(200, xmlEndpoint.path("test_valid.xml").request().delete().getStatus());
        assertEquals("XML file should have been deleted",
                0, xmlEndpoint.request().get(MAP_STRING_STRING_TYPE).size());

        Response noXmlTree = xmlProviderPath.request(MediaType.APPLICATION_JSON).get();
        assertEquals("The end point for the XML data provider should no longer be available.",
                405, noXmlTree.getStatus());
    }

    private static @NonNull String getPath(String bundlePath) throws URISyntaxException, IOException {
        assertNotNull(XML_CORE_TESTS);
        URL location = FileLocator.find(XML_CORE_TESTS, new Path(bundlePath), null);
        String path = FileLocator.toFileURL(location).toURI().getPath();
        assertNotNull(path);
        return path;
    }

}
