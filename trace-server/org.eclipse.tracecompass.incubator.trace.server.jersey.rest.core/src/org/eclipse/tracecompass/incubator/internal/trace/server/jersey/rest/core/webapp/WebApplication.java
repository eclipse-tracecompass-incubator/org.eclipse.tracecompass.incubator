/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EventTableService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.ExperimentManagerService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.XmlManagerService;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

    private static final String CONTEXT_PATH = "/tracecompass"; //$NON-NLS-1$
    private static final String PATH_SPEC = "/*"; //$NON-NLS-1$
    /**
     * Port value which boots the server in testing mode.
     */
    public static final int TEST_PORT = 8378;

    private int fPort;

    private Server fServer;

    /**
     * Default Constructor
     */
    public WebApplication() {
        this(8080);
    }

    /**
     * Constructor to to provide different port for server
     *
     * @param port
     *            the port to use
     */
    public WebApplication(int port) {
        fPort = port;
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
        rc.register(EventTableService.class);
        rc.register(DataProviderService.class);
        rc.register(XmlManagerService.class);
        rc.register(CORSFilter.class);
        rc.register(registerCustomMappers());

        ServletContainer sc = new ServletContainer(rc);
        ServletHolder holder = new ServletHolder(sc);
        sch.addServlet(holder, PATH_SPEC);

        fServer = new Server(fPort);
        fServer.setHandler(sch);

        fServer.start();
        if (fPort != TEST_PORT) {
            fServer.join();
        }
    }

    private static JacksonJaxbJsonProvider registerCustomMappers() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // create JsonProvider to provide custom ObjectMapper
        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(mapper);

        SimpleModule module = new SimpleModule();
        module.addSerializer(ITmfTrace.class, new TraceSerializer());
        module.addSerializer(TmfExperiment.class, new ExperimentSerializer());
        mapper.registerModule(module);
        return provider;
    }

    /**
     * Method to stop the web application
     */
    public void stop() {
        try {
            fServer.stop();
        } catch (Exception e) {
            // ignore
        }
    }

}
