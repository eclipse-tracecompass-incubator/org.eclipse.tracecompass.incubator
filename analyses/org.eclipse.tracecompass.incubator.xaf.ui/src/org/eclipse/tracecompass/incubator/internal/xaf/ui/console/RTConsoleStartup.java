/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.xaf.ui.console;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineReport;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * Startup class that will add a console to display the results of the XaF
 * analysis
 *
 * @author Geneviève Bastien
 */
public class RTConsoleStartup implements IStartup {

    private static final @NonNull String CONSOLE_NAME = "org.eclipse.tracecompass.incubator.xaf.ui.rtanalysis.console"; //$NON-NLS-1$

    @Override
    public void earlyStartup() {
        StateMachineReport.R.setOutput(findConsole());
    }

    private static MessageConsoleStream findConsole() {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++) {
            if (CONSOLE_NAME.equals(existing[i].getName())) {
                return ((MessageConsole) existing[i]).newMessageStream();
            }
        }
        // no console found, so create a new one
        MessageConsole myConsole = new MessageConsole(CONSOLE_NAME, null);
        conMan.addConsoles(new IConsole[] { myConsole });
        return myConsole.newMessageStream();
    }

}
