/**********************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

/**
 * This filter enables cross-origin requests. AJAX requests are forbidden by
 * default by the same-origin security policy. By enabling cross-origin
 * requests, a client of a different domain than the server could make HTTP
 * requests.
 *
 * @author Yonni Chen
 */
@Provider
public class CORSFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        response.getHeaders().add("Access-Control-Allow-Origin", "*"); //$NON-NLS-1$ //$NON-NLS-2$
        response.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization"); //$NON-NLS-1$ //$NON-NLS-2$
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}