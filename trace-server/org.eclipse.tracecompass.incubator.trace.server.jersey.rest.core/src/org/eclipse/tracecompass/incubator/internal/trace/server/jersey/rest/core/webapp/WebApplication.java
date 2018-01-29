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
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.data.TraceManager;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DiskActivityViewService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EventTableService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

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

    private final TraceManager fTraceManager = new TraceManager();

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

        TraceManagerService resource = new TraceManagerService();
        ResourceConfig rc = new ResourceConfig();
        rc.register(JacksonFeature.class);
        rc.register(resource);
        EventTableService table = new EventTableService();
        rc.register(table);
        DiskActivityViewService diskService = new DiskActivityViewService();
        rc.register(diskService);
        rc.register(new CORSFilter());

        /**
         * register a TraceManager, this allows it to be swappable with another
         * implementation.
         */
        rc.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(fTraceManager).to(TraceManager.class);
            }
        });

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
