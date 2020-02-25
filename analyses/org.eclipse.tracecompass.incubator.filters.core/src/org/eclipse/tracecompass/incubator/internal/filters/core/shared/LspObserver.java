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

package org.eclipse.tracecompass.incubator.internal.filters.core.shared;

import java.util.List;

import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Interface to apply observable pattern between the lsp api and the filter box
 *
 * @author Jeremy Dube
 *
 */
public interface LspObserver {
    /**
     * Method to notify the observer of diagnostic changes
     *
     * @param uri
     *            The identifier for the document diagnosed
     *
     * @param diagnostics
     *            the diagnostics to go through
     */
    void diagnostic(String uri, List<Diagnostic> diagnostics);

    /**
     * Method to notify the observer of completion changes
     *
     * @param uri
     *            The identifier for the document to complete
     *
     * @param completion
     *            the completion items to render
     */
    void completion(String uri, Either<List<CompletionItem>, CompletionList> completion);

    /**
     * Method to notify the observer of color changes
     *
     * @param uri
     *            The identifier for the document to highlight
     *
     * @param colors
     *            the colors to render
     */
    void syntaxHighlighting(String uri, List<ColorInformation> colors);
}
