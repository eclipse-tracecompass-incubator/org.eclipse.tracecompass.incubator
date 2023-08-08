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
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.Activator;

import com.google.common.annotations.VisibleForTesting;

/**
 * Class that centralizes getting the configuration for the Trace Compass server
 * and default values
 *
 * @author Geneviève Bastien
 */
public class TraceServerConfiguration {

    /**
     * Port value which boots the server in testing mode.
     */
    public static final int TEST_PORT = 8378;

    /**
     * This is protected so it may be linked from other JavaDoc in this class.
     */
    protected static final String PROPERTY_HOST = "traceserver.host"; //$NON-NLS-1$
    /**
     * This is protected so it may be linked from other JavaDoc in this class.
     */
    protected static final String PROPERTY_PORT = "traceserver.port"; //$NON-NLS-1$

    private static final String PROPERTY_USESSL = "traceserver.useSSL"; //$NON-NLS-1$
    private static final String PROPERTY_KEYSTORE = "traceserver.keystore"; //$NON-NLS-1$
    private static final String PROPERTY_KEYSTORE_PASS = "traceserver.keystorepass"; //$NON-NLS-1$

    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final int DEFAULT_SSL_PORT = 8443;

    private final int fPort;
    private final boolean fUseSSL;
    private final @Nullable String fKeystore;
    private final @Nullable String fKeystorePass;
    private final @Nullable String fHost;

    /**
     * Create the trace server configuration
     *
     * @return The trace server configuration
     */
    public static TraceServerConfiguration create() {
        String portStr = System.getProperty(PROPERTY_PORT);
        String usesslStr = System.getProperty(PROPERTY_USESSL);
        usesslStr = usesslStr == null || usesslStr.isEmpty() ? null : usesslStr;
        String keystore = System.getProperty(PROPERTY_KEYSTORE);
        keystore = keystore == null || keystore.isEmpty() ? null : keystore;
        String keystorePass = System.getProperty(PROPERTY_KEYSTORE_PASS);
        keystorePass = keystorePass == null || keystorePass.isEmpty() ? null : keystorePass;
        boolean useSSL = (usesslStr != null && !"false".equals(usesslStr)) ? true : false; //$NON-NLS-1$
        if (keystore != null && usesslStr == null) {
            useSSL = true;
        } else if (useSSL) {
            Activator.getInstance().logError(String.format("Server requested to use SSL, but no keystore specified. You must specify the keystore using the '%s' system property. Will use plain http instead.", PROPERTY_KEYSTORE)); //$NON-NLS-1$
            useSSL = false;
        }
        int port = useSSL ? DEFAULT_SSL_PORT : DEFAULT_HTTP_PORT;
        if (portStr != null && !portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                // Log an exception
                Activator.getInstance().logWarning(String.format("Invalid port specified: %s. Will use default port %d", portStr, port)); //$NON-NLS-1$
            }
        }
        String host = System.getProperty(PROPERTY_HOST);
        if (host != null && !host.isEmpty()) {
            return new TraceServerConfiguration(host, port, useSSL, keystore, keystorePass);
        }
        // Otherwise host already assumed as null, meaning 0.0.0.0 or wild-card.
        return new TraceServerConfiguration(port, useSSL, keystore, keystorePass);
    }

    /**
     * Constructor. Use only for unit tests, otherwise use {@link #create()} to
     * automatically get the configuration parameters
     *
     * @param keystorePass
     *            The keystore password
     * @param keystore
     *            The path to the SSL keystore
     * @param useSSL
     *            Whether to use SSL
     * @param port
     *            The port to use
     */
    @VisibleForTesting
    public TraceServerConfiguration(int port, boolean useSSL, @Nullable String keystore, @Nullable String keystorePass) {
        this(null, port, useSSL, keystore, keystorePass);
    }

    /**
     * Constructor. Use only for unit tests, otherwise use {@link #create()} to
     * automatically get the configuration parameters
     *
     * @param keystorePass
     *            The keystore password
     * @param keystore
     *            The path to the SSL keystore
     * @param useSSL
     *            Whether to use SSL
     * @param port
     *            The port to use
     * @param host
     *            The host to use
     */
    @VisibleForTesting
    public TraceServerConfiguration(String host, int port, boolean useSSL, @Nullable String keystore, @Nullable String keystorePass) {
        fHost = host;
        fPort = port;
        fUseSSL = useSSL;
        fKeystore = keystore;
        fKeystorePass = keystorePass;
    }

    /**
     * Get the host on which the server will run. The host can be specified
     * using the system property {@link #PROPERTY_HOST}
     *
     * @return The host string
     */
    public @Nullable String getHost() {
        return fHost;
    }

    /**
     * Get the port on which the server will run. The port can be specified
     * using the system property {@link #PROPERTY_PORT}
     *
     * @return The port number
     */
    public int getPort() {
        return fPort;
    }

    /**
     * Get whether the server should use SSL
     *
     * @return if <code>true</code>, the server will use SSL
     */
    public boolean useSSL() {
        return fUseSSL;
    }

    /**
     * Get the keystore for SSL configuration. SSL keystore and certificates can
     * be configured as per the jetty documentation
     *
     * @see <a href=
     *      "https://www.eclipse.org/jetty/documentation/current/configuring-ssl.html">https://www.eclipse.org/jetty/documentation/current/configuring-ssl.html</a>
     *
     * @return The path to the keystore
     */
    public @Nullable String getKeystore() {
        return fKeystore;
    }

    /**
     * Get the keystore password
     *
     * @return The keystore password
     */
    public @Nullable String getKeystorePass() {
        return fKeystorePass;
    }
}
