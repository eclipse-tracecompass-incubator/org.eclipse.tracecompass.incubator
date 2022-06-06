/**********************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.webapp;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.ExperimentManagerService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.FilterService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.HealthService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.XmlManagerService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.CORSFilter;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.JacksonObjectMapperProvider;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.TraceServerConfiguration;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.WebApplication;
import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

/**
 * Test WebApplication to create custom replies to for serializer testing
 */
public class TestWebApplication extends WebApplication {

    /**
     * Constructor to provide a configuration to the server
     *
     * @param config
     *            Server configuration
     */
    public TestWebApplication(TraceServerConfiguration config) {
        super(config);
    }

    @Override
    protected void registerResourcesAndMappers(ResourceConfig rc) {
        rc.register(TraceManagerService.class);
        rc.register(ExperimentManagerService.class);
        rc.register(TestDataProviderService.class);
        rc.register(FilterService.class);
        rc.register(HealthService.class);
        rc.register(XmlManagerService.class);
        rc.register(CORSFilter.class);
        rc.register(JacksonObjectMapperProvider.class);
        rc.register(OpenApiResource.class);
    }
}
