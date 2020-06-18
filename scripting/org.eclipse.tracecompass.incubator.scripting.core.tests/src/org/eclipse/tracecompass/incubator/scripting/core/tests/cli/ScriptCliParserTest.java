/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.tests.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.cli.core.parser.help.test.HelpCliParserTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This class tests the parsing of the arguments related to scripting, and
 * executes the early options, but it does not test the behavior of the
 * individual arguments.
 *
 * @author David Piché
 */
@RunWith(Parameterized.class)
public class ScriptCliParserTest extends HelpCliParserTest {

    private static final String TESTFILES = "testfiles/cli/";
    private static final String HELP_FILE = "helpText.txt";
    private static final String TEST_CLI = "cli/";

    /**
     * @return The arrays of parameters
     */
    @Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
                // Help arguments
                { "--cli --help", false, TEST_CLI + HELP_FILE },
                { "--cli -h", false, TEST_CLI + HELP_FILE },
                // Script
                { "--cli --script myscript", false, null },
                { "--cli -s myscript", false, null },
                { "--cli --script myscript1 myscript2 myscript3", false, null },
                { "--cli -s myscript1 myscript2 myscript3", false, null },
                { "--cli --script", true, TEST_CLI + "missingScriptArgument.txt" },
                { "--cli -s", true, TEST_CLI + "missingScriptArgument.txt" },
                // Engine
                { "--cli --engine jython", false, null },
                { "--cli -e jython", false, null },
                { "--cli --engine", true, TEST_CLI + "missingEngineArgument.txt" },
                { "--cli -e", true, TEST_CLI + "missingEngineArgument.txt" },
                // Arguments
                { "--cli --args arg1", false, null },
                { "--cli -e jython ", false, null },
                { "--cli --args arg1 arg2 arg3", false, null },
                { "--cli -a arg1 arg2 arg3", false, null },
                { "--cli --args", true, TEST_CLI + "missingArgsArgument.txt" },
                { "--cli -a", true, TEST_CLI + "missingArgsArgument.txt" },

        });
    }

    /**
     * Constructor
     *
     * @param cmdLine
     *            The command line arguments to parse
     * @param exception
     *            Whether these arguments should throw an exception
     * @param fileText
     *            The file containing the expected output. Set to
     *            <code>null</code> if there is no output.
     */
    public ScriptCliParserTest(String cmdLine, boolean exception, @Nullable String fileText) {
        super(cmdLine, exception, fileText);
    }

    @Override
    protected String getHelpText() throws IOException {
        byte[] helpBytes = Files.readAllBytes(Paths.get(TESTFILES + HELP_FILE));
        return new String(helpBytes);
    }

}
