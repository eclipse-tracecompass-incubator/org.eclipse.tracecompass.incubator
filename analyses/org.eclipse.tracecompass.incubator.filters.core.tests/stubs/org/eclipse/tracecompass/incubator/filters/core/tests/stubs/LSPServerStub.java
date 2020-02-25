/*******************************************************************************
 * Copyright (c) 2019 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.filters.core.tests.stubs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.eclipse.tracecompass.incubator.filters.core.tests.environment.TestEnvironment;
import org.eclipse.tracecompass.incubator.internal.filters.core.server.FilterWorkspaceService;

/**
 *
 * Implements a fake LanguageServer to handle the messages passed by a real
 * LanguageFilterClient. Use this class to probe/forward the messages.
 *
 * DON'T FORGET TO COUNT THE TRANSACTIONS SO THE TESTS CAN BE SYNCHRONIZE BASED
 * ON THE NUMBER OF EXPECTED TRANSACTIONS THIS COUNT CAN BE FOUND/INSERTED
 * WITHIN: this, {@link FilterBoxServiceStub}, {@link LSPClientStub} and {@link FakeClientStub}
 *
 * @see TestConnector
 * @see TestEnvironment
 *
 * @author Maxime Thibault
 *
 */
public class LSPServerStub implements LanguageServer {

    // Mockup in wich store the probed messages/signals
    public LSPServerMockup fMockup = new LSPServerMockup();

    /**
     * A fake FilterBoxServer
     *
     * {@link FilterBoxService}
     */
    private final FilterBoxServiceStub fFilterBoxService;

    /**
     * Don't do anything right now. Implement a fakeFilterWorkspaceServiceStub
     * if necessary
     */
    private final WorkspaceService fFilterWorkspaceService;

    // Reference to the TestConnector that create this LSPServerStub.
    private final TestConnector fStub;

    /**
     *
     * Create the serverStub. Also initialize the services stubs.
     *
     * {@link FilterBoxServiceStub}
     *
     * @param languageServer:
     *            The real LanguageServer implementation
     * @param transactionsLock
     *            use this semaphore to count the transactions and use it in the
     *            TestEnvironment
     */
    LSPServerStub(TestConnector stub) {
        fStub = stub;
        fFilterBoxService = new FilterBoxServiceStub(stub);
        fFilterWorkspaceService = new FilterWorkspaceService();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        return CompletableFuture.completedFuture(new InitializeResult());
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        Object returnVal = null;
        try {
            // Get the value from the real server
            returnVal = fStub.getProxyServer().shutdown().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        // Pass it back to the real client
        return CompletableFuture.completedFuture(returnVal);
    }

    @Override
    public void exit() {
        // Ask the server to exits
        fStub.getProxyServer().exit();
        // Count this transaction so that we can terminate the tests
        // @see TestEnvironment.waitForTransactionToTerminate
        // This is the last transaction we expect, but should not be considered
        // in the testEnvironment transactionExpected value
        fStub.count();
    }

    @Override
    public FilterBoxServiceStub getTextDocumentService() {
        // Return the FilterBoxServiceStub
        return fFilterBoxService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return fFilterWorkspaceService;
    }

}