/**********************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils;

import static org.junit.Assert.assertEquals;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.TraceServerConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link TraceServerConfiguration} class
 *
 * @author Geneviève Bastien
 */
public class TraceServerConfigurationTest {

    private static final String PROPERTY_PORT = "traceserver.port"; //$NON-NLS-1$
    private static final String PROPERTY_USESSL = "traceserver.useSSL"; //$NON-NLS-1$
    private static final String PROPERTY_KEYSTORE = "traceserver.keystore"; //$NON-NLS-1$
    private static final String PROPERTY_KEYSTORE_PASS = "traceserver.keystorepass"; //$NON-NLS-1$

    /**
     * Reset all properties at the end of the test
     */
    @Before
    public void initializeProperties() {
        System.setProperty(PROPERTY_PORT, "");
        System.setProperty(PROPERTY_USESSL, "");
        System.setProperty(PROPERTY_KEYSTORE, "");
        System.setProperty(PROPERTY_KEYSTORE_PASS, "");
    }

    /**
     * Reset all properties at the end of the test
     */
    @After
    public void cleanUpProperties() {
        System.setProperty(PROPERTY_PORT, "");
        System.setProperty(PROPERTY_USESSL, "");
        System.setProperty(PROPERTY_KEYSTORE, "");
        System.setProperty(PROPERTY_KEYSTORE_PASS, "");
    }

    /**
     * Test the default configuration
     */
    @Test
    public void testDefaultHttp() {
        assertConfiguration(new TraceServerConfiguration(8080, false, null, null), TraceServerConfiguration.create());
    }

    /**
     * Test setting the http port
     */
    @Test
    public void testHttp() {
        int port = 8088;
        System.setProperty(PROPERTY_PORT, String.valueOf(port));
        assertConfiguration(new TraceServerConfiguration(port, false, null, null), TraceServerConfiguration.create());
    }

    /**
     * Test valid https configurations
     */
    @Test
    public void testHttps() {
        // Set a keystore path, should use ssl on default port
        String keystorePath = "/path/to/myfile";
        System.setProperty(PROPERTY_KEYSTORE, keystorePath);
        assertConfiguration(new TraceServerConfiguration(8443, true, keystorePath, null), TraceServerConfiguration.create());

        // Set a different ssl port
        int port = 8444;
        System.setProperty(PROPERTY_PORT, String.valueOf(port));
        assertConfiguration(new TraceServerConfiguration(port, true, keystorePath, null), TraceServerConfiguration.create());

        // Add a keystore password
        String keystorePass = "my password";
        System.setProperty(PROPERTY_KEYSTORE_PASS, keystorePass);
        assertConfiguration(new TraceServerConfiguration(port, true, keystorePath, keystorePass), TraceServerConfiguration.create());
    }

    /**
     * Test forcing http even with https properties
     */
    @Test
    public void testForceHttp() {
        // Set a keystore path, should use ssl on default port
        String keystorePath = "/path/to/myfile";
        String keystorePass = "my password";
        int port = 8444;
        System.setProperty(PROPERTY_KEYSTORE, keystorePath);
        System.setProperty(PROPERTY_PORT, String.valueOf(port));
        System.setProperty(PROPERTY_KEYSTORE_PASS, keystorePass);
        System.setProperty(PROPERTY_USESSL, String.valueOf(false));

        assertConfiguration(new TraceServerConfiguration(port, false, keystorePath, keystorePass), TraceServerConfiguration.create());
    }

    /**
     * Test ssl no keystore defaults to http
     */
    @Test
    public void testHttpsNoKeystore() {
        // Set use SSL, but without a keystore
        System.setProperty(PROPERTY_USESSL, String.valueOf(true));

        assertConfiguration(new TraceServerConfiguration(8080, false, null, null), TraceServerConfiguration.create());
    }

    /**
     * Test invalid port
     */
    @Test
    public void testInvalidPort() {
        System.setProperty(PROPERTY_PORT, "not a number");
        assertConfiguration(new TraceServerConfiguration(8080, false, null, null), TraceServerConfiguration.create());
    }

    private static void assertConfiguration(TraceServerConfiguration expected, TraceServerConfiguration actual) {
        assertEquals(expected.getPort(), actual.getPort());
        assertEquals(expected.getKeystore(), actual.getKeystore());
        assertEquals(expected.getKeystorePass(), actual.getKeystorePass());
        assertEquals(expected.useSSL(), actual.useSSL());

    }

}
