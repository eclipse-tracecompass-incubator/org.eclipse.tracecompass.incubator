/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.application;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.WebApplication;

/**
 * Eclipse application entry
 *
 * @author Bernd Hufmann
 */
public class Application implements IApplication {

    private static final String WORKSPACE_NAME = ".tracecompass-webapp"; //$NON-NLS-1$
    private WebApplication fWebAppl;
    private Location fInstanceLoc = null;

    @Override
    public Object start(IApplicationContext context) throws Exception {
        // fetch the Location that we will be modifying
        fInstanceLoc = Platform.getInstanceLocation();
        if (!fInstanceLoc.allowsDefault() && !fInstanceLoc.isSet()) {
            File workspaceRoot = new File(getWorkspaceRoot());

            if (!workspaceRoot.exists()) {
                return IApplication.EXIT_OK;
            }

            if (!workspaceRoot.canWrite()) {
                return IApplication.EXIT_OK;
            }

            String workspace = getWorkspaceRoot() + File.separator + WORKSPACE_NAME;
            // set location to workspace
            fInstanceLoc.set(new URL("file", null, workspace), false); //$NON-NLS-1$
        }

        if (!fInstanceLoc.lock()) {
            return IApplication.EXIT_OK;
        }

        fWebAppl = new WebApplication();
        fWebAppl.start();
        return null;
    }

    @Override
    public void stop() {
        fWebAppl.stop();
        fInstanceLoc.release();
    }

    private static String getWorkspaceRoot() {
        /* Look for the environment variable in the global environment variables */
        String workspaceRoot = System.getenv().get("TRACING_SERVER_ROOT"); //$NON-NLS-1$
        if (workspaceRoot == null) {
            /* Use the user's home directory */
            workspaceRoot = System.getProperty("user.home"); //$NON-NLS-1$
        }
        return workspaceRoot;
    }
}
