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

package org.eclipse.tracecompass.incubator.internal.filters.core.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.antlr.runtime.RecognitionException;
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
import org.eclipse.lsp4j.Diagnostic;
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
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.tracecompass.incubator.internal.filters.core.Activator;

/**
 * FilterBoxService offers the interface to the client in order to notify the
 * server of the changes and to ask for completions, validations and syntax tips
 * for the filter string.
 *
 * @author Remi Croteau
 * @author David-Alexandre Beaupre
 *
 */
public class FilterBoxService implements TextDocumentService {

    private Map<String, String> fFiltersInputs;
    private final LanguageFilterServer fLSPServer;

    /**
     * Constructor for the filterBoxService
     *
     * @param server
     *            is a language filter server
     */
    protected FilterBoxService(LanguageFilterServer server) {
        fFiltersInputs = new HashMap<>();
        fLSPServer = server;
    }

    /**
     * Offers completion suggestions based on the user input
     *
     * @param completionParams
     *            Contains the current cursor position and uri
     */
    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams completionParams) {

        List<CompletionItem> completions = new ArrayList<>();
        try {
            String uri = completionParams.getTextDocument().getUri();
            String input = fFiltersInputs.get(uri);
            if (input == null) {
                return CompletableFuture.completedFuture(Either.forLeft(completions));
            }
            Position cursor = completionParams.getPosition();
            List<String> suggestions = AutoCompletion.autoCompletion(input, cursor);
            for (int i = 0; i < suggestions.size(); i++) {
                Position start = new Position(0, 0);
                Position end = new Position(0, input.length());
                CompletionItem item = new CompletionItem();
                TextEdit textEdit = new TextEdit(new Range(start, end), suggestions.get(i));
                item.setTextEdit(textEdit);
                completions.add(item);
            }
        } catch (IOException error) {
            Activator.getInstance().logError(error.getMessage());
        }
        return CompletableFuture.completedFuture(Either.forLeft(completions));
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        // May eventually be used
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams position) {
        // Not used.
        throw new UnsupportedOperationException();
    }
    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams position) {
        // Does not apply to filter box
        throw new UnsupportedOperationException();
    }
    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams position) {
        // Does not apply to filter box
        throw new UnsupportedOperationException();
    }
    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        // Does not apply to filter box
        throw new UnsupportedOperationException();
    }
    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams position) {
        // May be implemented eventually
        throw new UnsupportedOperationException();
    }

    /**
     * Gives colors to each token in the user input
     *
     * @param params
     *            is the for the document (i.e. the input)
     * @return List of colors for each token as a CompletableFuture
     */
    @Override
    public CompletableFuture<List<ColorInformation>> documentColor(DocumentColorParams params) {
        try {
            String uri = params.getTextDocument().getUri();
            String input = fFiltersInputs.get(uri);
            List<ColorInformation> colorInformation = SyntaxHighlighting.getColorInformationList(input);
            return CompletableFuture.completedFuture(colorInformation);
        } catch (IOException error) {
            Activator.getInstance().logError(error.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        // May be implemented eventually
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        // Does not apply to filter box
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        // May be implemented eventually
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
        // May be implemented eventually
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        // Should be implemented eventually
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
        // Should be implemented eventually
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
        // Should be implemented eventually
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        // Does not apply to filter box
        throw new UnsupportedOperationException();
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        fFiltersInputs.put(params.getTextDocument().getUri(), ""); //$NON-NLS-1$
    }

    /**
     * Check the string validity and sends a diagnostic to the client
     *
     * @param params
     *            contains the changes to the string input
     */
    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        TextDocumentContentChangeEvent contentChange = params.getContentChanges().get(0);
        if (contentChange == null) {
            throw new NullPointerException("Event change param cannot be null"); //$NON-NLS-1$
        }
        String input = params.getContentChanges().get(0).getText();
        fFiltersInputs.put(uri, input);
        try {
            List<Diagnostic> diagnostics = FilterValidation.validate(input);
            PublishDiagnosticsParams pd = new PublishDiagnosticsParams(uri, diagnostics);
            pd.setDiagnostics(diagnostics);
            LanguageClient client = fLSPServer.getClient();
            if (client != null) {
                client.publishDiagnostics(pd);
            }
        } catch (RecognitionException | IOException error) {
            Activator.getInstance().logError(error.getMessage());
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        // Does not apply to filter box
        throw new UnsupportedOperationException();
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // Does not apply to filter box
        throw new UnsupportedOperationException();
    }

}
