/*******************************************************************************
 * Copyright (c) 2017, 2020 Ericsson
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
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.Experiment;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.ExperimentManagerService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.FilterService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.Trace;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.XmlManagerService;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.IVirtualTableLine;
import org.eclipse.tracecompass.internal.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.TmfProjectNature;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

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

        rc.register(TraceManagerService.class);
        rc.register(ExperimentManagerService.class);
        rc.register(DataProviderService.class);
        rc.register(FilterService.class);
        rc.register(XmlManagerService.class);
        rc.register(CORSFilter.class);
        rc.register(registerCustomMappers());

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

        IFolder tracesFolder = project.getFolder("Traces");
        if (!tracesFolder.exists()) {
            tracesFolder.create(true, true, null);
        }

        fServer.start();
        if (fConfig.getPort() != TraceServerConfiguration.TEST_PORT) {
            fServer.join();
        }
    }

    private static ServerConnector getConnector(Server server, TraceServerConfiguration config) {
        ServerConnector serverConnector = null;
        if (config.useSSL()) {

            SslContextFactory contextFactory = new SslContextFactory.Server();
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
        serverConnector.setPort(config.getPort());
        return serverConnector;
    }

    private static JacksonJaxbJsonProvider registerCustomMappers() {
        ObjectMapper mapper = new ObjectMapper();

        // create JsonProvider to provide custom ObjectMapper
        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(mapper);

        SimpleModule module = new SimpleModule();
        module.addSerializer(Trace.class, new TraceSerializer());
        module.addSerializer(Experiment.class, new ExperimentSerializer());
        module.addSerializer(DataProviderDescriptor.class, new DataProviderDescriptorSerializer());
        module.addSerializer(ITmfXyModel.class, new XYModelSerializer());
        module.addSerializer(ISeriesModel.class, new SeriesModelSerializer());
        module.addSerializer(TimeGraphState.class, new TimeGraphStateSerializer());
        module.addSerializer(TimeGraphRowModel.class, new TimeGraphRowModelSerializer());
        module.addSerializer(TimeGraphEntryModel.class, new TimeGraphEntryModelSerializer());
        module.addSerializer(TmfTreeDataModel.class, new TmfTreeModelSerializer());
        module.addSerializer(OutputElementStyle.class, new OutputElementStyleSerializer());
        module.addSerializer(IVirtualTableLine.class, new VirtualTableLineSerializer());
        mapper.registerModule(module);
        return provider;
    }

    /**
     * Method to stop the web application
     */
    public void stop() {
        try {
            fServer.stop();
            ResourcesPlugin.getWorkspace().getRoot()
                    .getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME)
                    .close(null);
        } catch (Exception e) {
            // ignore
        }
    }

}
