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

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.tracecompass.incubator.internal.filters.core.shared.LspObservable;
import org.eclipse.tracecompass.incubator.internal.filters.core.shared.LspObserver;

/**
 *
 * Implements a fake LanguageClient to handle the messages send by a real
 * LanguageFilterServer. Use this class to probe/forward the messages.
 *
 * DON'T FORGET TO COUNT THE TRANSACTIONS SO THE TESTS CAN BE SYNCHRONIZED BASED
 * ON THE NUMBER OF EXPECTED TRANSACTIONS THIS COUNT CAN BE FOUND/INSERTED
 * WITHIN: this, {@link FilterBoxServiceStub}, {@link LSPClientStub} and {@link FakeClientStub}
 *
 * {@link TestEnvironment}
 *
 * @author Maxime Thibault
 *
 */
public class LSPClientStub implements LanguageClient, LspObservable {

    // Mockup in wich store the probed messages/signals
    public LSPClientMockup fMockup = new LSPClientMockup();

    // Reference to the TestConnector that create this LSPServerStub.
    private final TestConnector fStub;

    // Observer to send updates
    public LspObserver fObserver;

    public LSPClientStub(TestConnector stub) {
        fStub = stub;
    }

    @Override
    public void telemetryEvent(Object object) {
        // Not implemented
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
        // Store data into mockup
        fMockup.fDiagnosticsReceived = diagnostics.getDiagnostics();
        // Forward the call to the real Client implementation
        fStub.getProxyClient().publishDiagnostics(diagnostics);
        // Count this transaction
        fStub.count();
    }

    @Override
    public void showMessage(MessageParams messageParams) {
        // Not implemented
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
        return null;
    }

    @Override
    public void logMessage(MessageParams message) {
        // Not implemented
    }

    @Override
    public void register(@NonNull LspObserver obs) {
        fObserver = obs;
    }

}