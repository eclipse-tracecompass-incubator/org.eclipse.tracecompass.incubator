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

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.ANN;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CFG;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.DESC;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.DIA;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.DT;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.EMAIL;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.EXP;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.IDF;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.LICENSE;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.LICENSE_URL;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.OCG;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.SERVER;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.STY;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TERMS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TGR;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TITLE;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TRA;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.VERSION;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.VTB;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.X_Y;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Class extending {@link OpenApiResource} and providing application level open
 * API definitions for the Trace Compass trace server.
 */
@OpenAPIDefinition(info = @Info(title = TITLE, description = DESC, termsOfService = TERMS, contact = @Contact(email = EMAIL), license = @License(name = LICENSE, url = LICENSE_URL), version = VERSION), servers = {
        @Server(url = SERVER)
}, tags = {
        @Tag(name = ANN, description = "Retrieve annotations for different outputs."),
        @Tag(name = CFG, description = "Manage configuration source types and configurations."),
        @Tag(name = DIA, description = "Retrieve the server's status."),
        @Tag(name = DT,  description = "Query data tree models (e.g. for statistics)."),
        @Tag(name = EXP, description = "Manage experiments on your server; an experiment represents a collection of traces, which can produce output models."),
        @Tag(name = IDF, description = "Retrieve information about the server and the system it is running on."),
        @Tag(name = OCG, description = "Manage configuration source types and configurations for given outputs."),
        @Tag(name = STY, description = "Retrieve styles for different outputs."),
        @Tag(name = TGR, description = "Query Time Graph models."),
        @Tag(name = TRA, description = "Manage physical traces on your server."),
        @Tag(name = VTB, description = "Query virtual table models (e.g. Events Table)."),
        @Tag(name = X_Y, description = "Query XY chart models.")
})
public class TraceServerOpenApiResource extends OpenApiResource {
}
