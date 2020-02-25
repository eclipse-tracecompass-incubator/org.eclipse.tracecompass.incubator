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

import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.eclipse.lsp4j.Position;
import org.eclipse.tracecompass.tmf.filter.parser.FilterParserLexer;

/**
 * Class that offer autocompletion parameters based on antlr
 *
 * @author Maxime Thibault
 * @author David-Alexandre Beaupre
 * @author Remi Croteau
 *
 */
public class AutoCompletion {

    // theree seems to be no way of getting those directly from the grammar
    static private String[] OPERATORS = { "==", "!=", "<", ">", "matches", "contains", "present" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
    static private String[] SEPARATORS = { "&&", "||" }; //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Proposes suggestions based on the cursor position
     *
     * @param str
     *            is the content of the filter box
     * @param cursor
     *            is the current position in the string
     * @return List of suggestions as string
     * @throws IOException
     *             can be thrown by ByteArrayInputStream
     */
    @SuppressWarnings("restriction") // Suppress restriction on ANTLR
                                     // FilterParser*
    static public List<String> autoCompletion(String str, Position cursor) throws IOException {

        String subString = str.substring(0, cursor.getCharacter());
        String endString = str.substring(cursor.getCharacter(), str.length());
        List<String> suggestions = new ArrayList<>();

        // Initialize the lexerParser, parse str and return list of CommonToken
        ByteArrayInputStream input = new ByteArrayInputStream(subString.getBytes());
        ANTLRInputStream antlrStream = new ANTLRInputStream(input);
        FilterParserLexer lexer = new FilterParserLexer(antlrStream);
        ArrayList<RecognitionException> lexerExceptions = new ArrayList<>();
        lexer.setErrorListener(e -> {
            lexerExceptions.add((RecognitionException) e);
        });
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        List<CommonToken> commonTokens = tokenStream.getTokens();
        if (commonTokens.isEmpty()) {
            return suggestions;
        }

        CommonToken lastToken = null;
        int lastType = -1;
        if (!commonTokens.isEmpty()) {
            lastToken = commonTokens.get(commonTokens.size() - 1);
            lastType = lastToken.getType();
        }

        CommonToken beforeLastToken = null;
        int beforeLastType = -1;
        if (commonTokens.size() > 1) {
            beforeLastToken = commonTokens.get(commonTokens.size() - 2);
            beforeLastType = beforeLastToken.getType();
        }

        if (lastToken != null && lastType == FilterParserLexer.TEXT) {
            // separator
            for (int i = 0; i < SEPARATORS.length; i++) {
                suggestions.add(new String(subString + " " + SEPARATORS[i] + " " + endString)); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (beforeLastToken == null || beforeLastType != FilterParserLexer.OP) {
                // operators
                for (int i = 0; i < OPERATORS.length; i++) {
                    suggestions.add(new String(subString + " " + OPERATORS[i] + " " + endString)); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }
        if (lastToken != null && lastType == FilterParserLexer.T__23) {
            // separators
            for (int i = 0; i < SEPARATORS.length; i++) {
                suggestions.add(new String(subString + " " + SEPARATORS[i] + " " + endString)); //$NON-NLS-1$//$NON-NLS-2$
            }
        }

        // format output so there is one space between each token
        for (int i = 0; i < suggestions.size(); i++) {
            input = new ByteArrayInputStream(suggestions.get(i).getBytes());
            antlrStream = new ANTLRInputStream(input);
            lexer = new FilterParserLexer(antlrStream);
            tokenStream = new CommonTokenStream(lexer);
            lexer.setErrorListener(e -> {
                lexerExceptions.add((RecognitionException) e);
            });

            commonTokens = tokenStream.getTokens();

            StringBuilder suggestion = new StringBuilder();

            for (int j = 0; j < commonTokens.size(); j++) {
                suggestion.append(commonTokens.get(j).getText() + " "); //$NON-NLS-1$
            }
            suggestions.set(i, suggestion.toString());
        }

        return suggestions;
    }

}
