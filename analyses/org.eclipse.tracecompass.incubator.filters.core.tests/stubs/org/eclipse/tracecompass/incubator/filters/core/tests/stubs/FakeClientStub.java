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

import java.util.List;

import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.tracecompass.incubator.internal.filters.core.shared.LspObserver;

/**
 * This fake client stub is used to handle function call from the LSPClient
 * bound in the TestEnvironment Use this class to store the data into the
 * FakeClientMockup
 *
 * @author Maxime Thibault
 */
public class FakeClientStub implements LspObserver {

    public FakeClientMockup fMockup = new FakeClientMockup();

    @Override
    public void diagnostic(String uri, List<Diagnostic> diagnostics) {
        // Nothing to do
    }

    @Override
    public void completion(String uri, Either<List<CompletionItem>, CompletionList> completion) {
        // Nothing to do

    }

    @Override
    public void syntaxHighlighting(String uri, List<ColorInformation> colors) {
        // Nothing to do

    }
}
