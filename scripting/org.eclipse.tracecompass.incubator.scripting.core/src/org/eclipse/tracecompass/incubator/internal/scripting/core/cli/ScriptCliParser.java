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

package org.eclipse.tracecompass.incubator.internal.scripting.core.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ease.service.EngineDescription;
import org.eclipse.ease.service.IScriptService;
import org.eclipse.ease.service.ScriptService;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.scripting.core.Activator;
import org.eclipse.tracecompass.incubator.internal.scripting.core.ScriptExecutionHelper;
import org.eclipse.tracecompass.internal.provisional.tmf.cli.core.parser.CliCommandLine;
import org.eclipse.tracecompass.internal.provisional.tmf.cli.core.parser.CliOption;
import org.eclipse.tracecompass.internal.provisional.tmf.cli.core.parser.ICliParser;

/**
 * Command line parser for scripts
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class ScriptCliParser implements ICliParser {

    private static final String OPTION_COMMAND_LINE_RUN_SCRIPT_SHORT = "s"; //$NON-NLS-1$
    private static final String OPTION_COMMAND_LINE_RUN_SCRIPT_LONG = "script"; //$NON-NLS-1$
    private static final String OPTION_COMMAND_LINE_RUN_SCRIPT_DESCRIPTION = Objects.requireNonNull(Messages.CliParser_RunScriptDescription);

    private static final String OPTION_COMMAND_LINE_SCRIPT_ENGINE_SHORT = "e"; //$NON-NLS-1$
    private static final String OPTION_COMMAND_LINE_SCRIPT_ENGINE_LONG = "engine"; //$NON-NLS-1$
    private static final String OPTION_COMMAND_LINE_SCRIPT_ENGINE_DESCRIPTION = Objects.requireNonNull(Messages.CliParser_ScriptEngineDescription);

    private static final String OPTION_COMMAND_LINE_SCRIPT_ARGUMENT_SHORT = "a"; //$NON-NLS-1$
    private static final String OPTION_COMMAND_LINE_SCRIPT_ARGUMENT_LONG = "args"; //$NON-NLS-1$
    private static final String OPTION_COMMAND_LINE_SCRIPT_ARGUMENT_DESCRIPTION = Objects.requireNonNull(Messages.CliParser_ScriptArgumentDescription);

    private final HashMap<String, String> fEngineNameToId;
    private final ArrayList<CliOption> fOptions;

    /**
     * Constructor
     */
    public ScriptCliParser() {
        fOptions = new ArrayList<>();
        fOptions.add(CliOption.createOptionWithArgs(OPTION_COMMAND_LINE_RUN_SCRIPT_SHORT, OPTION_COMMAND_LINE_RUN_SCRIPT_LONG, OPTION_COMMAND_LINE_RUN_SCRIPT_DESCRIPTION, true, true, "script")); //$NON-NLS-1$
        fOptions.add(CliOption.createOptionWithArgs(OPTION_COMMAND_LINE_SCRIPT_ENGINE_SHORT, OPTION_COMMAND_LINE_SCRIPT_ENGINE_LONG, OPTION_COMMAND_LINE_SCRIPT_ENGINE_DESCRIPTION, true, false, "engine name")); //$NON-NLS-1$
        fOptions.add(CliOption.createOptionWithArgs(OPTION_COMMAND_LINE_SCRIPT_ARGUMENT_SHORT, OPTION_COMMAND_LINE_SCRIPT_ARGUMENT_LONG, OPTION_COMMAND_LINE_SCRIPT_ARGUMENT_DESCRIPTION, true, true, "argument")); //$NON-NLS-1$

        fEngineNameToId = new HashMap<>();
        // Get all available EASE engines
        final IScriptService scriptService = ScriptService.getInstance();
        Collection<EngineDescription> engines = scriptService.getEngines();
        for (EngineDescription engine : engines) {
            // Do not include debugger engines
            if (engine.getName().contains("Debugger")) { //$NON-NLS-1$
                continue;
            }
            fEngineNameToId.put(engine.getName(), engine.getID());
        }
    }

    @Override
    public List<CliOption> getCmdLineOptions() {
        return fOptions;
    }

    @Override
    public @NonNull IStatus workspaceLoading(@NonNull CliCommandLine commandLine, @NonNull IProgressMonitor monitor) {
        if (commandLine.hasOption(OPTION_COMMAND_LINE_RUN_SCRIPT_SHORT)) {
            // The script option should be handled once the workspace is ready
            String[] scripts = commandLine.getOptionValues(OPTION_COMMAND_LINE_RUN_SCRIPT_SHORT);
            String scriptEngine = null;
            List<String> scriptArguments = null;

            if (commandLine.hasOption(OPTION_COMMAND_LINE_SCRIPT_ENGINE_SHORT)) {
                String scriptEngineName = commandLine.getOptionValues(OPTION_COMMAND_LINE_SCRIPT_ENGINE_SHORT)[0];
                // Search the list of available engines for a match
                for (Entry<String, String> entry : fEngineNameToId.entrySet()) {
                    String entryEngineName = Objects.requireNonNull(entry.getKey());
                    if (entryEngineName.toLowerCase().contains(scriptEngineName.toLowerCase())) {
                        scriptEngine = Objects.requireNonNull(entry.getValue());
                    }
                }
                // No available engine found
                if (scriptEngine == null) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.CliParser_ScriptEngineMapError);
                }

            }
            if (commandLine.hasOption(OPTION_COMMAND_LINE_SCRIPT_ARGUMENT_SHORT)) {
                scriptArguments = Arrays.asList(commandLine.getOptionValues(OPTION_COMMAND_LINE_SCRIPT_ARGUMENT_SHORT));
            }
            for (String script : scripts) {
                Object scriptRet = ScriptExecutionHelper.executeScript(String.valueOf(script), scriptEngine, scriptArguments);

                if (scriptRet == null) {
                    // Return after script execution failure
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.CliParser_ScriptExecutionError);
                }
            }
        }
        return Status.OK_STATUS;
    }

}
