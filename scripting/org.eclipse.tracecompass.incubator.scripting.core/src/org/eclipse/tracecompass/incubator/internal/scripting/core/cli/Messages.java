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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for the command line parser
 *
 * @author Geneviève Bastien
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$

    /** Run script command description */
    public static @Nullable String CliParser_RunScriptDescription;
    /** Script engine command description */
    public static @Nullable String CliParser_ScriptEngineDescription;
    /** Message when the engine name mapping does not find any engineID **/
    public static @Nullable String CliParser_ScriptEngineMapError;
    /** Script argument command description */
    public static @Nullable String CliParser_ScriptArgumentDescription;
    /** Message when script does not end properly */
    public static @Nullable String CliParser_ScriptExecutionError;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
