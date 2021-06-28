/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.application;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.WebApplication;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Eclipse application entry
 *
 * @author Bernd Hufmann
 */
public class Application implements IApplication {

    private static final String WORKSPACE_NAME = ".tracecompass-webapp"; //$NON-NLS-1$

    private enum ExitCode {
        OK(""), //$NON-NLS-1$
        ERROR_NO_WORKSPACE_ROOT("Trace server error: Workspace root doesn't exist."), //$NON-NLS-1$
        ERROR_WORKSPACE_ROOT_PERMISSION("Trace server error: Workspace not writable."), //$NON-NLS-1$
        ERROR_WORKSPACE_ALREADY_IN_USE("Trace server error: Workspace already in use."), //$NON-NLS-1$
        ERROR_RUNNING_TRACE_SERVER("Trace server error: Error running trace server."); //$NON-NLS-1$
        @NonNull private String fErrorText;
        private ExitCode(@NonNull String errorText) {
            fErrorText = errorText;
        }
        @NonNull public String getErrorText() {
            return fErrorText;
        }
    }

    private WebApplication fWebAppl = null;
    private Location fInstanceLoc = null;


    /**
     * Default constructor
     */
    public Application() {
        // empty constructor
    }

    /**
     * Constructor with initialized {@link WebApplication}
     *
     * @param webApp
     *            a {@link WebApplication} instance which will be started by
     *            this Application
     */
    public Application(WebApplication webApp) {
        fWebAppl = webApp;
    }

    @Override
    public Object start(IApplicationContext context) throws Exception {
        // fetch the Location that we will be modifying
        fInstanceLoc = Platform.getInstanceLocation();
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        if (!fInstanceLoc.allowsDefault() && !fInstanceLoc.isSet()) {
            File workspaceRoot = new File(getWorkspaceRoot());

            if (!workspaceRoot.exists()) {
                ExitCode exitCode = ExitCode.ERROR_NO_WORKSPACE_ROOT;
                Platform.getLog(bundle).log(new Status(IStatus.ERROR, bundle.getSymbolicName(), exitCode.getErrorText()));
                return Integer.valueOf(exitCode.ordinal());
            }

            if (!workspaceRoot.canWrite()) {
                ExitCode exitCode = ExitCode.ERROR_WORKSPACE_ROOT_PERMISSION;
                Platform.getLog(bundle).log(new Status(IStatus.ERROR, bundle.getSymbolicName(), exitCode.getErrorText()));
                return Integer.valueOf(exitCode.ordinal());
            }

            String workspace = getWorkspaceRoot() + File.separator + WORKSPACE_NAME;
            // set location to workspace
            fInstanceLoc.set(new URL("file", null, workspace), false); //$NON-NLS-1$
        }

        if (!fInstanceLoc.lock()) {
            ExitCode exitCode = ExitCode.ERROR_WORKSPACE_ALREADY_IN_USE;
            Platform.getLog(bundle).log(new Status(IStatus.ERROR, bundle.getSymbolicName(), exitCode.getErrorText() + " (workspace: " + fInstanceLoc.getURL().getPath() + ")"));//$NON-NLS-1$ //$NON-NLS-2$
            return Integer.valueOf(exitCode.ordinal());
        }

        if (fWebAppl == null) {
            fWebAppl = new WebApplication();
        }
        try {
            fWebAppl.start();
        } catch (Exception e) {
            ExitCode exitCode = ExitCode.ERROR_RUNNING_TRACE_SERVER;
            Platform.getLog(bundle).log(new Status(IStatus.ERROR, bundle.getSymbolicName(), exitCode.getErrorText(), e));
            return Integer.valueOf(exitCode.ordinal());
        }
        return IApplication.EXIT_OK;
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
