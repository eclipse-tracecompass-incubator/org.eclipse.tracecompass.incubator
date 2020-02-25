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

package org.eclipse.tracecompass.incubator.filters.core.tests.client;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.eclipse.tracecompass.incubator.filters.core.tests.environment.TestEnvironment;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

/**
 * Test the filter language client
 *
 * @author Maxime Thibault
 */
public class LspClientTest {

    /** Timeout the tests after 2 minutes */
    @Rule
    public TestRule timeoutRule = new Timeout(2, TimeUnit.MINUTES);

    /**
     * Simple hello world tests. LSPClient send 'hello' to LSPServer
     *
     * @throws InterruptedException
     *             Exceptions thrown by test
     * @throws IOException
     *             Exceptions thrown by test
     */
    @Test
    public void hello() throws InterruptedException, IOException {
        String input = "Hello";
        String uri = "Mamma mia";
        /**
         * We expect 5 transactions: 1.DidOpen: client -> Server 2.DidChange:
         * client -> server 3.publishDiagnostics: server -> client
         * 4.syntaxHighlight: client <-> server 5.documentColor: client
         * <->server
         */
        TestEnvironment te = new TestEnvironment(5);
        te.getClient().getLanguageClient().tellDidOpen(uri);
        te.getClient().notify(uri, input, input.length());

        // Lock till the transactions we're expecting is not over
        te.waitForTransactionToTerminate();

        // Check mockup for stored values
        assertEquals(input, te.getTestConnector().getServerStub().getTextDocumentService().fMockup.fInputReceived);
        assertEquals(0, te.getTestConnector().getClientStub().fMockup.fDiagnosticsReceived.size());
    }

    /**
     * TODO: ADD MORE TESTS!!
     */

}
