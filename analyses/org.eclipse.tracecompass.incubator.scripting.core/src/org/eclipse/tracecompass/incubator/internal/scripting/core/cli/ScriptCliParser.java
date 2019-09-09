/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.core.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
    private final ArrayList<CliOption> fOptions;

    /**
     * Constructor
     */
    public ScriptCliParser() {
        fOptions = new ArrayList<>();
        fOptions.add(CliOption.createOptionWithArgs(OPTION_COMMAND_LINE_RUN_SCRIPT_SHORT, OPTION_COMMAND_LINE_RUN_SCRIPT_LONG, OPTION_COMMAND_LINE_RUN_SCRIPT_DESCRIPTION, true, true, "script")); //$NON-NLS-1$
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
            for (String script : scripts) {
                Object scriptRet = ScriptExecutionHelper.executeScript(String.valueOf(script));
                if (scriptRet == null) {
                    // Return after script execution failure
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.CliParser_ScriptExecutionError);
                }
            }
        }
        return Status.OK_STATUS;
    }

}
