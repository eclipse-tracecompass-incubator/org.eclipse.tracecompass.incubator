/*******************************************************************************
 * Copyright (c) 2017, 2021 Ericsson and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.ExperimentManagerService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.FilterService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.HealthService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.XmlManagerService;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.TmfProjectNature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

/**
 *
 * Simple web application using Jetty
 *
 * @author Bernd Hufmann
 *
 */
public class WebApplication {

    private static final String CONTEXT_PATH = "/tsp/api"; //$NON-NLS-1$
    private static final String PATH_SPEC = "/*"; //$NON-NLS-1$

    private Server fServer;
    private final TraceServerConfiguration fConfig;

    /**
     * Default Constructor
     */
    public WebApplication() {
        this(TraceServerConfiguration.create());
    }

    /**
     * Constructor to provide a configuration to the server
     *
     * @param config
     *            Server configuration
     */
    public WebApplication(TraceServerConfiguration config) {
        fConfig = config;
    }

    /**
     * Method to start the web application
     *
     * @throws Exception
     *             if server cannot be started
     */
    public void start() throws Exception {
        ServletContextHandler sch = new ServletContextHandler();
        sch.setContextPath(CONTEXT_PATH);

        ResourceConfig rc = new ResourceConfig();
        registerResourcesAndMappers(rc);
        ServletContainer sc = new ServletContainer(rc);
        ServletHolder holder = new ServletHolder(sc);
        sch.addServlet(holder, PATH_SPEC);

        fServer = new Server();
        // https://www.programcreek.com/java-api-examples/?api=org.eclipse.jetty.server.SslConnectionFactory

        @SuppressWarnings("resource")
        ServerConnector connector = getConnector(fServer, fConfig);
        fServer.addConnector(connector);
        fServer.setHandler(sch);

        // create and open a default eclipse project.
        IProject project = ResourcesPlugin.getWorkspace().getRoot()
                .getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
        if (!project.exists()) {
            project.create(null);
            if (!project.isOpen()) {
                project.open(null);
            }
            IProjectDescription description = project.getDescription();
            description.setNatureIds(new String[] { TmfProjectNature.ID });
            project.setDescription(description, null);
        }
        if (!project.isOpen()) {
            project.open(null);
        }

        IFolder tracesFolder = project.getFolder("Traces"); //$NON-NLS-1$
        if (!tracesFolder.exists()) {
            tracesFolder.create(true, true, null);
        }

        fServer.start();
    }

    /**
     * Given a {@link ResourceConfig}, register resources (i.e. endpoints) and
     * custom mappers (i.e. serializers) for this web application.
     *
     * @param rc
     *            the {@link ResourceConfig} instance to be configured with
     *            resources and mappers
     */
    protected void registerResourcesAndMappers(ResourceConfig rc) {
        rc.register(TraceManagerService.class);
        rc.register(ExperimentManagerService.class);
        rc.register(DataProviderService.class);
        rc.register(FilterService.class);
        rc.register(HealthService.class);
        rc.register(XmlManagerService.class);
        rc.register(CORSFilter.class);
        rc.register(JacksonObjectMapperProvider.class);
        EncodingFilter.enableFor(rc, GZipEncoder.class);
        rc.register(OpenApiResource.class);
    }

    /**
     * Given a server instance and its preferred configuration, a properly
     * configured ServerConnector for the Jetty server is returned.
     *
     * @param server
     *            the server instance
     * @param config
     *            a class describing the desired server configuration
     * @return a configured ServerConnector instance
     */
    protected static ServerConnector getConnector(Server server, TraceServerConfiguration config) {
        ServerConnector serverConnector = null;
        if (config.useSSL()) {

            org.eclipse.jetty.util.ssl.SslContextFactory.Server contextFactory = new SslContextFactory.Server();
            contextFactory.setKeyStorePath(config.getKeystore());
            contextFactory.setKeyStorePassword(config.getKeystorePass());
            contextFactory.setTrustAll(true);

            HttpConfiguration httpsConfig = new HttpConfiguration();
            httpsConfig.setSecureScheme("https"); //$NON-NLS-1$
            httpsConfig.setOutputBufferSize(32768);
            httpsConfig.setRequestHeaderSize(8192 * 2);
            httpsConfig.setResponseHeaderSize(8192 * 2);
            httpsConfig.setSendServerVersion(true);
            httpsConfig.setSendDateHeader(false);

            SslConnectionFactory connector = new SslConnectionFactory(contextFactory, HttpVersion.HTTP_1_1.asString());
            serverConnector = new ServerConnector(server, connector, new HttpConnectionFactory(httpsConfig));
        } else {
            serverConnector = new ServerConnector(server);
        }
        serverConnector.setHost(config.getHost());
        serverConnector.setPort(config.getPort());
        return serverConnector;
    }

    /**
     * Method to dispose all necessary resources.
     *
     * Needs to be called before calling {@link #stop()}
     */
    public void dispose() {
        ExperimentManagerService.dispose();
        TraceManagerService.dispose();
    }

    /**
     * Method to stop the web application
     */
    public void stop() {
        try {
            fServer.stop();
        } catch (Exception ex) {
            Bundle bundle = FrameworkUtil.getBundle(this.getClass());
            Platform.getLog(bundle).log(new Status(IStatus.ERROR, bundle.getSymbolicName(), "Error stopping server", ex)); //$NON-NLS-1$
        }
    }
}
