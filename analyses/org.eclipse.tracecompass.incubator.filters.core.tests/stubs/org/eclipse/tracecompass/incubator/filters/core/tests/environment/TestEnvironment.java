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
package org.eclipse.tracecompass.incubator.filters.core.tests.environment;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import org.eclipse.tracecompass.incubator.filters.core.tests.stubs.TestConnector;
import org.eclipse.tracecompass.incubator.internal.filters.core.client.wrapper.LanguageFilterClientWrapper;
import org.eclipse.tracecompass.incubator.internal.filters.core.server.FilterServerFactory;

/**
 * Create a test environment for testing LSP implementations Use this object in
 * your test case to synchronize and probe transactions
 *
 * This class contains everything to test the real LSP client implementation and
 * the real LSP server implementation.
 *
 * Stubs will need to be enhanced in order to support more tests see
 * {@link TestConnector} for more information on how to use the Stub
 *
 * Access stubs and real implementations from this class methods.
 *
 * 1.Use Server/Client to call methods invoke real implementations
 *
 * 2.Use stubs to check if value passed between real implementations are
 * correct.
 *
 * Here's a class diagram of the TestEnvironment
 *
 * <pre>
 *  TestEnvironment ⟵ TestConnector ⟵ [LanguageFilterClient, LanguageFilterServer, [LSPServerStub,LSPClientStub] ⟵ [Mockup]] ⟵ Stream
 *
 *  X ⟵ Y means X is composed of Y
 * </pre>
 *
 *
 *
 * Here's how communicates the stubs and the real implementation inside the
 * TestEnvironment. The left part is the real implementations and the right part
 * the stubs
 *
 * <pre>
 *  LanguageFilterClient ⟵⟶ LSPServerStub
 *  LanguageFilterServer ⟵⟶ LSPClientStub
 *
 *  X ⟵⟶ Y means X and Y communicates both way
 * </pre>
 *
 * Note that the real implementations communicates with the stubs using Stream.
 * When a stub receives a message, it can invoke any methods on the real
 * implementations. For example, LSPServerStub can invoke methods on
 * LanguageFilterServer. See {@link TestConnector}. Also, the streams has to be
 * carefully connected between the stubs and the real implementations to allow 2
 * way communication. It is necessary because the LanguageServer and
 * LanguageClient sometimes return value from a CompletableFuture. So the stubs
 * needs to be able to wait for responses and also send it back to the real
 * implementation through CompletableFuture.
 *
 * @author Maxime Thibault
 */
public class TestEnvironment {

    // The real LSP client implementation
    private FilterServerFactory fServer = null;
    private LanguageFilterClientWrapper fClient = null;

    // Stub that contains the fake server and the face client implementation
    // see {@link TestConnector} for more information on how to use the
    // connector
    private TestConnector fTestConnector;

    // The number of expected transactions between the server and the client
    // The transactions are counted in the stubs
    private int fExepectedTransaction;

    // Semaphore to wait the end of expected transactions
    private Semaphore fTransactionsLock;

    /**
     * Create a test environment
     *
     * @param expectedTransaction
     *            The number of transaction expected during the test
     * @throws InterruptedException
     *             Exception thrown by environment initialization
     * @throws IOException
     *             Exception thrown by environment initialization
     */
    public TestEnvironment(int expectedTransaction) throws IOException, InterruptedException {
        initialize(expectedTransaction);
    }

    /**
     * Reset the test environment
     *
     * @param expectedTransaction
     *            The number of transaction expected during the test
     * @throws InterruptedException
     *             Exception thrown by environment initialization
     * @throws IOException
     *             Exception thrown by environment initialization
     */
    public void reset(int expectedTransaction) throws IOException, InterruptedException {
        initialize(expectedTransaction);
    }

    /**
     * Initialize the test environment
     *
     * @param expectedTransaction
     *            The number of transaction expected before completion This
     *            variable is use to create a semaphore that block until the
     *            number of transactions between the LSP Implementations has
     *            been observed. @see TestConnector.count()
     * @throws IOException
     *             Exception thrown by the streams
     * @throws InterruptedException
     *             Exception thrown by the lock
     */
    private void initialize(int expectedTransaction) throws IOException, InterruptedException {

        fExepectedTransaction = expectedTransaction;
        fTransactionsLock = new Semaphore(expectedTransaction);
        // Empty the semaphore
        fTransactionsLock.acquire(fExepectedTransaction);

        // Connect stubs and real implementations

        Stream clientStream = new Stream();
        Stream serverStream = new Stream();
        Stream clientStubStream = new Stream();
        Stream serverStubStream = new Stream();

        // Init stub
        fTestConnector = new TestConnector(fTransactionsLock);

        // Server read from client stub, write its own stream back to it
        fServer = new FilterServerFactory(clientStubStream.read, serverStream.write);

        // Init clientStub: stub read from server and write its own stream back
        // to it
        fTestConnector.initClient(serverStream.read, clientStubStream.write);

        // Init serverStub: stub read from client and write its own stream back
        // to it
        fTestConnector.initServer(clientStream.read, serverStubStream.write);

        // Client read from server stub, write its own stream back to it
        fClient = new LanguageFilterClientWrapper(serverStubStream.read, clientStream.write, Objects.requireNonNull(fTestConnector.getObserver()));

    }

    /**
     * Wait for the transactions to be done
     *
     * @throws InterruptedException
     *             Exception thrown by lock
     */
    public void waitForTransactionToTerminate() throws InterruptedException {
        // @see TestConnector.count()
        fTransactionsLock.acquire(fExepectedTransaction);
        // Do one more for exit call -> This ensure that the last transaction
        // we've expected has finished
        fClient.dispose();
        fTransactionsLock.acquire();
    }

    /**
     * Return the test connector
     *
     * Use this to check value passed between the real implementations
     *
     * @return The connector
     */
    public TestConnector getTestConnector() {
        return fTestConnector;
    }

    /**
     * Return the real client implementation
     *
     * @return The real client implementation
     */
    public LanguageFilterClientWrapper getClient() {
        return fClient;
    }

    /**
     * Return the real server implementation
     *
     * @return The real server implementation
     */
    public FilterServerFactory getServer() {
        return fServer;
    }
}
