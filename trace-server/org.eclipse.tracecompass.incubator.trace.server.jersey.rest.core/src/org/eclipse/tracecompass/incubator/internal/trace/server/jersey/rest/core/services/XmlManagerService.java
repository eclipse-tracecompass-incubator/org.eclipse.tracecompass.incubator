/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.io.File;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlAnalysisModuleSource;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils;

import com.google.common.collect.Maps;

/**
 * XML analysis and provider management
 *
 * @author Loic Prieur-Drevon
 */
@Path("/xml")
@SuppressWarnings("restriction")
public class XmlManagerService {

    /**
     * Getter for the list of available XML files
     *
     * @return list of available XML files, encapsulated in a response.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getXml() {
        return Response.ok(Maps.transformValues(XmlUtils.listFiles(), File::getAbsolutePath)).build();
    }

    /**
     * POST a new XML file to the server
     *
     * @param path
     *            path to the file
     * @return status for adding new XML
     */
    @POST
    public Response postXml(@FormParam("path") String path) {
        return updateXml(path, true);
    }

    /**
     * PUT an XML file to the server
     *
     * @param path
     *            path to the file
     * @return status for adding new XML
     */
    @PUT
    public Response putXml(@FormParam("path") String path) {
        return updateXml(path, false);
    }

    /**
     * End point to delete an XML file by name
     *
     * @param name
     *            XML file name
     * @return OK
     */
    @DELETE
    @Path("/{name}")
    public Response deleteXml(@PathParam("name") String name) {
        if (!XmlUtils.listFiles().containsKey(name)) {
            return Response.status(Status.NOT_FOUND).build();
        }
        XmlUtils.deleteFile(name);
        return Response.ok().build();
    }

    private static Response updateXml(String path, boolean addFile) {
        File file = new File(path);

        IStatus status = XmlUtils.xmlValidate(file);
        if (status.isOK()) {
            if (addFile) {
                status = XmlUtils.addXmlFile(file);
            } else {
                XmlUtils.updateXmlFile(file);
            }
            if (status.isOK()) {
                XmlAnalysisModuleSource.notifyModuleChange();
                return Response.ok().build();
            }
        }
        return Response.serverError().build();
    }

}
