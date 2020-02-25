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

package org.eclipse.tracecompass.incubator.filters.core.tests.stubs;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Used in the TestEnvironment to handle communications between
 * LanguageFilterServer and LanguageFilterClient.
 *
 * This class centralizes communications between these two classes. Doing so we
 * can probe the values passed between them and check if they are valid and/or
 * expected.
 *
 * To do so, we use a client stub and a server stub connected to the real
 * implementations in which we forward back the signals to the other end.
 *
 * This class knows everything (stubs and real implementations) and can
 * communicate with each end. We pass this class to the stubs so those can
 * forward the signals to any end.
 *
 * @author Maxime Thibault
 *
 */
public class TestConnector {

    // The real server implementation
    private static LanguageServer fProxyServer;
    // The real client implementation
    private static LanguageClient fProxyClient;

    private LSPClientStub fClientStub;
    private LSPServerStub fServerStub;

    // Client stub, usually the far end of the client
    private final @NonNull FakeClientStub fFakeClientStub;

    // Passed by the TestEnvironment to count the transactions
    private final Semaphore fTransactionsLock;

    /**
     * Setup the TestConnector. FIXME: Create a fake client with no purpose
     * yet..
     *
     * @param transactionLock
     */
    public TestConnector(Semaphore transactionLock) {
        fTransactionsLock = transactionLock;
        fFakeClientStub = new FakeClientStub();
    }

    /**
     * Initialize the serverStub from the TestEnvironment
     *
     * See the LSPServerStub to implements/modify signal probing/forwarding
     *
     * {@link LSPServerStub}
     *
     * @param streamFromClient
     *            Stream to read from client
     * @param streamToClient
     *            Stream to write to client
     */
    public void initServer(InputStream streamFromClient, OutputStream streamToClient) {
        // Start the stub server
        fServerStub = new LSPServerStub(this);
        Launcher<LanguageClient> serverLauncher = LSPLauncher.createServerLauncher(fServerStub, streamFromClient, streamToClient);
        fProxyClient = serverLauncher.getRemoteProxy();
        serverLauncher.startListening();

    }

    /**
     * Initialize the clientStub from the TestEnvironment
     *
     * See the LSPClientStub to implements/modify signal probing/forwarding
     *
     * {@link LSPClientStub}
     *
     * @param streamFromServer
     *            Stream to read from server
     * @param streamToServer
     *            Stream to write to server
     */
    public void initClient(InputStream streamFromServer, OutputStream streamToServer) {
        // Start the stub client
        fClientStub = new LSPClientStub(this);
        Launcher<LanguageServer> clientLauncher = LSPLauncher.createClientLauncher(fClientStub, streamFromServer, streamToServer);
        fProxyServer = clientLauncher.getRemoteProxy();
        fClientStub.register(fFakeClientStub);
        clientLauncher.startListening();
    }

    /**
     * Return the stub observer
     *
     * FIXME: Not used yet
     *
     * @return
     */
    public FakeClientStub getObserver() {
        return fFakeClientStub;
    }

    /**
     * Return the clientStub
     *
     * @return
     */
    public LSPClientStub getClientStub() {
        return fClientStub;
    }

    /**
     * Return the serverStub
     *
     * @return
     */
    public LSPServerStub getServerStub() {
        return fServerStub;
    }

    /**
     * Return the proxy of the real server implementation
     *
     * @return
     */
    public LanguageServer getProxyServer() {
        return fProxyServer;
    }

    /**
     * Return the proxy of the real client implementation
     *
     * @return
     */
    public LanguageClient getProxyClient() {
        return fProxyClient;
    }

    /**
     * Increment semaphore (used for synchronization based on the number of
     * expected transaction)
     *
     * Use this inside stubs to count the transactions
     *
     * @see LSPClientStub
     * @see LSPServerStub
     * @see FilterBoxServiceStub
     *
     */
    public void count() {
        fTransactionsLock.release();
    }

}
