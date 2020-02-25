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

package org.eclipse.tracecompass.incubator.internal.filters.core.server;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.tracecompass.incubator.internal.filters.core.Activator;
import org.eclipse.tracecompass.incubator.internal.filters.core.shared.FilterLspConfiguration;

/**
 * It's the class responsible for listening on the server socket and instantiate
 * a language server instance to associate with a client
 *
 * @author Maxime Thibault
 */
public class FilterServerFactory {

    private LanguageFilterServer fLSPServer;
    private ServerSocket fServerSocket;
    private Thread fMainThread;
    private List<Thread> fClientThreads = new ArrayList<>();
    private Boolean fCanRun = true;

    /**
     * Create serverSocket then wait for a client socket to connect When a
     * client socket is connected, create the lspLauncher to listen to incoming
     * requests
     *
     * @throws IOException
     *             can be thrown by the socket
     */
    public FilterServerFactory() throws IOException {
        fServerSocket = new ServerSocket(FilterLspConfiguration.PORT);
        fMainThread = new Thread(new ServerLoop());
        fMainThread.start();
    }

    /**
     * Create server from InputStream and OutputStream FIXME: This class is used
     * by the tests case only. This needs to be fixed.
     *
     * @param in
     *            InputStream (data in)
     * @param out
     *            OutputStream (data out)
     */
    @VisibleForTesting
    public FilterServerFactory(InputStream in, OutputStream out) {
        fLSPServer = new LanguageFilterServer();
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(fLSPServer, in, out);
        fLSPServer.connect(launcher.getRemoteProxy());
        launcher.startListening();
    }

    /**
     * Thread target to be run each time a new connection is accepted
     *
     * @author Maxime Thibault
     * @author Remi Croteau
     *
     */
    class ConnectionInitializer implements Runnable {
        private Socket fClientSocket;

        ConnectionInitializer(Socket clientSocket) {
            fClientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {

                // Instantiate LSP client
                InputStream in = fClientSocket.getInputStream();
                OutputStream out = fClientSocket.getOutputStream();

                fLSPServer = new LanguageFilterServer();
                Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(fLSPServer, in, out);
                fLSPServer.connect(launcher.getRemoteProxy());
                launcher.startListening();
            } catch (IOException e) {
                try {
                    fClientSocket.close();
                } catch (IOException e1) {
                    // Nothing to do, closing the socket
                }
                Activator.getInstance().logError(e.getMessage());
            }
        }
    }

    /**
     * Main server loop to accept new connections.
     *
     * @author Remi Croteau
     *
     */
    class ServerLoop implements Runnable {
        @Override
        public void run() {
            while (fCanRun) {
                try {
                    Socket clientSocket = fServerSocket.accept();
                    Thread thread = new Thread(new ConnectionInitializer(clientSocket));
                    fClientThreads.add(thread);
                    thread.start();
                } catch (IOException e) {
                    Activator.getInstance().logError(e.getMessage());
                }
            }
            try {
                fServerSocket.close();
            } catch (IOException e) {
                // Nothing to do, just closing the socket
            }
        }
    }

    /**
     * Close client-end and server socket
     *
     */
    public void dispose() {
        fClientThreads.forEach((Thread t) -> {
            t.interrupt();
        });
        fCanRun = false;
        fMainThread.interrupt();
    }
}
