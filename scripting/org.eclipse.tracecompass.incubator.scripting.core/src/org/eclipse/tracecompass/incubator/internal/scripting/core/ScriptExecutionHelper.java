/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.ease.IScriptEngine;
import org.eclipse.ease.ScriptResult;
import org.eclipse.ease.service.EngineDescription;
import org.eclipse.ease.service.IScriptService;
import org.eclipse.ease.service.ScriptService;
import org.eclipse.ease.service.ScriptType;
import org.eclipse.ease.tools.ResourceTools;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class is responsible to execute scripts in a headless mode.
 *
 * @author Geneviève Bastien
 */
public class ScriptExecutionHelper {

    /**
     * Execute the script file pointed to by the path
     *
     * @param filePath
     *            The absolute path of the file containing the script to execute
     * @return The return value of the script, or <code>null</code> if the
     *         script did not execute properly
     */
    public static @Nullable Object executeScript(String filePath) {
        return executeScript(filePath, null, null);
    }

    /**
     * Execute the script file pointed to by the path
     *
     * @param filePath
     *            The absolute path of the file containing the script to execute
     * @param engineID
     *            The ID of the engine to run this script with. If
     *            <code>null</code>, the method will try to find the right
     *            engine.
     * @param arguments
     *            The arguments to the script, or <code>null</code> if there are
     *            no arguments
     * @return The return value of the script, or <code>null</code> if the
     *         script did not execute properly
     */
    public static @Nullable Object executeScript(String filePath, @Nullable String engineID, @Nullable List<String> arguments) {
        // Does the file exists
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            Activator.getInstance().logWarning(String.format("Script file does not exist %s", filePath)); //$NON-NLS-1$
            return null;
        }
        EngineDescription engineDescription = getEngine(filePath, engineID);
        if (engineDescription == null) {
            Activator.getInstance().logWarning(String.format("No engine was found to execute script %s", filePath)); //$NON-NLS-1$
            return null;
        }

        // create engine
        final IScriptEngine engine = engineDescription.createEngine();

        // Add the arguments to the script
        if (arguments != null) {
            engine.setVariable("argv", arguments.toArray(new String[arguments.size()])); //$NON-NLS-1$
        }

        Object scriptObject = ResourceTools.resolve(filePath);
        if (scriptObject == null) {
            Activator.getInstance().logWarning(String.format("The script cannot be resolved %s", filePath)); //$NON-NLS-1$
            return null;
        }

        try {
            ScriptResult scriptResult = engine.executeSync(scriptObject);

            if (scriptResult.hasException()) {
                return null;
            }

            final Object result = scriptResult.getResult();

            if (result != null) {
                if (ScriptResult.VOID.equals(result)) {
                    return 0;
                }

                try {
                    return Integer.parseInt(result.toString());
                } catch (final Exception e) {
                    // no integer
                }

                try {
                    return (int)(Double.parseDouble(result.toString()));
                } catch (final Exception e) {
                    // no double
                }

                try {
                    return Boolean.parseBoolean(result.toString()) ? 0 : -1;
                } catch (final Exception e) {
                    // no boolean
                }

                // we do not know the return type, but typically parseBoolean()
                // will
                // deal with anything you throw at it
            } else {
                return 0;
            }
        } catch (InterruptedException e1) {
            Activator.getInstance().logWarning(String.format("Script execution was interrupted for %s", filePath)); //$NON-NLS-1$
            return null;
        }
        return null;

    }

    private static @Nullable EngineDescription getEngine(@Nullable String filePath, @Nullable String engineID) {
        final IScriptService scriptService = ScriptService.getInstance();

        if (engineID != null) {
            EngineDescription engineDescription = scriptService.getEngineByID(engineID);
            if (engineDescription == null) {
                throw new NullPointerException("Script engine not found " + engineID); //$NON-NLS-1$
            }
            return engineDescription;
        }

        // locate engine by file extension
        final ScriptType scriptType = scriptService.getScriptType(filePath);
        if (scriptType == null) {
            Activator.getInstance().logWarning(String.format("No script type was found for script %s", filePath)); //$NON-NLS-1$
            return null;
        }

        EngineDescription engineDescription = scriptService.getEngine(scriptType.getName());
        return engineDescription;

    }

}
