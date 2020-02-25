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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.CommonTokenStream;
import org.eclipse.lsp4j.Color;
import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.tracecompass.tmf.filter.parser.FilterParserLexer;

/**
 * Provides information on the colors for each token in the string
 *
 * @author Maxime Thibault
 * @author David-Alexandre Beaupre
 *
 */
public class SyntaxHighlighting {

    /**
     * RGB color for the operators (float values)
     */
    public static final Color OPERATION_COLOR = new Color(0.3, 0.3, 1, 1);
    /**
     * RGB color for the text (float values)
     */
    public static final Color TEXT_COLOR = new Color(0, 0, 0, 1);
    /**
     * RGB color for the separators (float values)
     */
    public static final Color SEPARATOR_COLOR = new Color(0.9, 0.5, 0.1, 1);

    /**
     * Assigns a color information for all tokens based on their type
     *
     * @param str
     *            input of the filter box
     *
     * @return colorInformation
     * @throws IOException
     *             from antlr
     */
    @SuppressWarnings("restriction")
    static public List<ColorInformation> getColorInformationList(String str) throws IOException {

        // Initialise the lexerParser, parse str and return list of CommonToken
        ByteArrayInputStream input = new ByteArrayInputStream(str.getBytes());
        ANTLRInputStream antlrStream = new ANTLRInputStream(input);
        FilterParserLexer lexer = new FilterParserLexer(antlrStream);
        lexer.setErrorListener(e -> {
            // do nothing
        });
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        List<CommonToken> commonTokenList = tokenStream.getTokens();

        // From commonTokens
        List<ColorInformation> colorInformations = new LinkedList<>();
        commonTokenList.forEach(commonToken -> {
            Position start = new Position(commonToken.getLine(), commonToken.getStartIndex());
            Position end = new Position(commonToken.getLine(), commonToken.getStopIndex());
            Range range = new Range(start, end);
            Color color = SyntaxHighlighting.getColor(commonToken.getType());
            ColorInformation colorInformation = new ColorInformation(range, color);
            colorInformations.add(colorInformation);
        });
        return colorInformations;
    }

    /**
     * Returns RGB color that matches type
     *
     * @param index
     * @return Color
     */
    private static Color getColor(int type) {
        switch (type) {
        case FilterParserLexer.OP:
            return OPERATION_COLOR;
        case FilterParserLexer.TEXT:
            return TEXT_COLOR;
        case FilterParserLexer.SEPARATOR:
            return SEPARATOR_COLOR;
        default:
            return TEXT_COLOR;
        }
    }

}
