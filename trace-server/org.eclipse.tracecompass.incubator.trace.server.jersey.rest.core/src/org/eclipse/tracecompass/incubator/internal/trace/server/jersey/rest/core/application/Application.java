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

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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
    private IApplicationContext fAppContext = null;
    private Bundle fBundle;
    @SuppressWarnings("null")
    @NonNull private Object fResult = EXIT_OK;

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
        fAppContext = context;
        // fetch the Location that we will be modifying
        fInstanceLoc = Platform.getInstanceLocation();
        fBundle = FrameworkUtil.getBundle(this.getClass());
        Bundle bundle = fBundle;
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

        // Add shutdown hook for clean-up
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                Application.this.stop();
            }
        });

        try {
            fWebAppl.start();
        } catch (Exception e) {
            ExitCode exitCode = ExitCode.ERROR_RUNNING_TRACE_SERVER;
            fResult = Integer.valueOf(exitCode.ordinal());
            Platform.getLog(bundle).log(new Status(IStatus.ERROR, bundle.getSymbolicName(), exitCode.getErrorText(), e));
            return fResult;
        }
        return IApplicationContext.EXIT_ASYNC_RESULT;
    }

    @Override
    public void stop() {
        WebApplication webAppl = fWebAppl;
        Bundle bundle = fBundle;
        IApplicationContext appContext = fAppContext;
        Location instanceLoc = fInstanceLoc;
        // Stop the web application
        if (fResult.equals(EXIT_OK)) {
            Platform.getLog(bundle).log(new Status(IStatus.INFO, bundle.getSymbolicName(), "Shutting down trace server.")); //$NON-NLS-1$
            if (webAppl != null) {
                webAppl.dispose();
                webAppl.stop();
            }
            // Save workspace
            try {
                ResourcesPlugin.getWorkspace().save(true, null);
            } catch (CoreException e) {
                Platform.getLog(bundle).log(new Status(IStatus.ERROR, bundle.getSymbolicName(), "Error saving workspace", e)); //$NON-NLS-1$
            }
            // Set the asynchronous result
            if (appContext != null) {
                appContext.setResult(fResult, this);
            }
        }
        // Release workspace
        if (instanceLoc!= null) {
            instanceLoc.release();
        }
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
