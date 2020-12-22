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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentColorParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

/**
 * Implements the fake FilterBoxService stub to probe/forward messages/signals
 * from the real LSP client/server implementations.
 *
 * This class uses the TestConnector to forward signals/messages to the real
 * implementations.
 *
 * Implements this class methods as needed to probe the messages/signals into
 * the mockup.
 *
 * DON'T FORGET TO COUNT THE TRANSACTIONS SO THE TESTS CAN BE SYNCHRONIZE BASED
 * ON THE NUMBER OF EXPECTED TRANSACTIONS THIS COUNT CAN BE FOUND/INSERTED
 * WITHIN: {@link LSPServerStub}, this, {@link LSPClientStub} and {@link FakeClientStub}
 *
 * {@link LSPServerStub}, {@link TestConnector}
 *
 * @author Maxime Thibault
 *
 */
public class FilterBoxServiceStub implements TextDocumentService {

    // Mockup in wich store the probed messages/signals
    public FilterBoxServiceMockup fMockup = new FilterBoxServiceMockup();

    /**
     * Reference to the TestConnector that create the LSPServerStub.
     */
    private final TestConnector fStub;

    public FilterBoxServiceStub(TestConnector stub) {
        fStub = stub;
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        // Count the transaction
        fStub.count();

        // Forward call to the real server
        fMockup.fCursor = position.getPosition().getCharacter();
        fMockup.fCompletionsReceived = fStub.getProxyServer().getTextDocumentService().completion(position);

        // Forward back the response to the real client
        return fMockup.fCompletionsReceived;
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        // Not implemented
        return null;
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams position) {
        // Not implemented
        return null;
    }
    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams position) {
        // Not implemented
        return null;
    }
    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams position) {
        // Not implemented
        return null;
    }
    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        // Not implemented
        return null;
    }
    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams position) {
        // Not implemented
        return null;
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        // Not implemented
        return null;
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        // Not implemented
        return null;
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        // Not implemented
        return null;
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
        // Not implemented
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        // Not implemented
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
        // Not implemented
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
        // Not implemented
        return null;
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        // Not implemented
        return null;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        // Forward call the real server (note: the server will reponse back
        // through the LSPClientStub)
        fStub.getProxyServer().getTextDocumentService().didOpen(params);
        // Count this transaction
        fStub.count();
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        // Store data in mockup
        fMockup.fInputReceived = params.getContentChanges().get(0).getText();
        // Forward call the function on the real server (note: the server will
        // reponse back through the LSPClientStub)
        fStub.getProxyServer().getTextDocumentService().didChange(params);
        // Count this transaction
        fStub.count();
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        // Not implemented
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // Not implemented
    }

    @Override
    public CompletableFuture<List<ColorInformation>> documentColor(DocumentColorParams params) {
        // Count transaction
        fStub.count();

        // Forward call the real implementation and wait for response
        fMockup.fColorsReceived = fStub.getProxyServer().getTextDocumentService().documentColor(params);
        // Forward back the response to the real client implementation
        return fMockup.fColorsReceived;

    }

}
