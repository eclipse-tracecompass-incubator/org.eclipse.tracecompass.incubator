/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariableAnalysis;
import org.eclipse.ui.console.MessageConsoleStream;

import com.google.common.base.Strings;

/**
 * A basic printer class to manage tabulation levels
 *
 * @author Raphaël Beamonte
 */
public class StateMachineReport {

    /** Main report instance */
    public final static StateMachineReport R = new StateMachineReport();

    /** The name of the ANALYZE print */
    public final static String ANALYZE = "ANALYZE"; //$NON-NLS-1$
    /** The name of the DEBUG print */
    public final static String DEBUG = "DEBUG"; //$NON-NLS-1$
    /** The name of the BENCHMARK print */
    public final static String BENCHMARK = "BENCHMARK"; //$NON-NLS-1$

    private final static Map<String, Boolean> shouldPrintMap = new HashMap<>();

    /**
     * @param type The type of element to check the environment variable for
     * @return If we should print
     */
    public static boolean shouldPrint(String type) {
        return shouldPrint(type, true);
    }

    /**
     * @param type The type of element to check the environment variable for
     * @param defaultValue The default value if none is given
     * @return If we should print
     */
    public static boolean shouldPrint(String type, boolean defaultValue) {
        String typeToUpper = type.toUpperCase();

        Boolean print = shouldPrintMap.get(typeToUpper);
        if (print != null) {
            return print;
        }

        // Default print status ?
        print = defaultValue;

        // Environment variable ?
        String envv = System.getenv().get("PRINT" + typeToUpper); //$NON-NLS-1$
        if (envv != null) {
            if (Boolean.parseBoolean(envv)) {
                print = true;
            } else {
                print = false;
            }
        }

        shouldPrintMap.put(typeToUpper, print);
        return print;
    }

    private static void specialPrint(Object str, String cat) {
        String c = cat.toUpperCase();
        if (!shouldPrint(c, false)) {
            return;
        }

        String finalString = String.format(c + ": %s", str); //$NON-NLS-1$
        finalString = finalString.replaceAll("\n", "\n" + c + ": "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        finalString = finalString.replaceAll("\n" + c + ": \n", "\n\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        finalString = finalString.replaceAll("^" + c + ": \n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        finalString = finalString.replaceAll("\n" + c + ": $", "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        System.out.println(finalString);
    }

    /**
     * Print a debug message
     * @param str The message to print
     */
    public static void debug(Object str) {
        specialPrint(str, DEBUG);
    }

    /**
     * Print a benchmark message
     * @param str The message to print
     */
    public static void benchmark(Object str) {
        specialPrint(str, BENCHMARK);
    }

    private class RTOutputStream {
        private final OutputStream fOutput;

        public RTOutputStream(OutputStream stream) {
            fOutput = stream;
        }

        public void println(String str) {
            if (fOutput instanceof PrintStream) {
                ((PrintStream) fOutput).println(str);
            } else if (fOutput instanceof MessageConsoleStream) {
                ((MessageConsoleStream) fOutput).println(str);
            }
        }

        public void println() {
            if (fOutput instanceof PrintStream) {
                ((PrintStream) fOutput).println();
            } else if (fOutput instanceof MessageConsoleStream) {
                ((MessageConsoleStream) fOutput).println();
            }
        }

    }

    private RTOutputStream output = new RTOutputStream(System.out);
    private int level = 0;
    private String tabStr = null;
    private final static char SHIFT = '\t';

    /**
     * Default constructor
     */
    public StateMachineReport() {
    }

    /**
     * Constructor with parameter
     * @param output The stream in which to output the report
     */
    public StateMachineReport(PrintStream output) {
        this.output = new RTOutputStream(output);
    }

    /**
     * <pre>
     * Print a decorated text as section:
     *    ╔═════╗
     *    ║ str ║
     *    ╚═════╝
     * </pre>
     * @param str The text
     */
    public void println_section(String str) {
        if (!shouldPrint(ANALYZE)) {
            return;
        }

        String top = "╔══"; //$NON-NLS-1$
        String bottom = "╚══"; //$NON-NLS-1$
        for (int i = 0; i < str.length(); i++) {
            top += "═"; //$NON-NLS-1$
            bottom += "═"; //$NON-NLS-1$
        }
        top += "╗"; //$NON-NLS-1$
        bottom += "╝"; //$NON-NLS-1$
        String t = top + "\n║ " + Font.boldItalic(str) + " ║\n" + bottom; //$NON-NLS-1$ //$NON-NLS-2$
        println(t);
    }

    /**
     * <pre>
     * Print a decorated text as subsection:
     *    ┏━━━━━┓
     *    ┃ str ┃
     *    ┗━━━━━┛
     * </pre>
     * @param str The text
     */
    public void println_subsection(String str) {
        if (!shouldPrint(ANALYZE)) {
            return;
        }

        String top = "┏━━"; //$NON-NLS-1$
        String bottom = "┗━━"; //$NON-NLS-1$
        for (int i = 0; i < str.length(); i++) {
            top += "━"; //$NON-NLS-1$
            bottom += "━"; //$NON-NLS-1$
        }
        top += "┓"; //$NON-NLS-1$
        bottom += "┛"; //$NON-NLS-1$
        String t = top + "\n┃ " + Font.bold(str) + " ┃\n" + bottom; //$NON-NLS-1$ //$NON-NLS-2$
        println(t);
    }

    /**
     * <pre>
     * Print a decorated text as subsubsection:
     *    ┌─────┐
     *    │ str │
     *    └─────┘
     * </pre>
     * @param str The text
     */
    public void println_subsubsection(String str) {
        if (!shouldPrint(ANALYZE)) {
            return;
        }

        String top = "┌──"; //$NON-NLS-1$
        String bottom = "└──"; //$NON-NLS-1$
        for (int i = 0; i < str.length(); i++) {
            top += "─"; //$NON-NLS-1$
            bottom += "─"; //$NON-NLS-1$
        }
        top += "┐"; //$NON-NLS-1$
        bottom += "┘"; //$NON-NLS-1$
        String t = top + "\n│ " + str + " │\n" + bottom; //$NON-NLS-1$ //$NON-NLS-2$
        println(t);
    }

    /**
     * <pre>
     * Print a decorated text as subsubsubsection:
     *    ┌┄┄┄┄┄┐
     *    ┊ str ┊
     *    └┄┄┄┄┄┘
     * </pre>
     * @param str The text
     */
    public void println_subsubsubsection(String str) {
        if (!shouldPrint(ANALYZE)) {
            return;
        }

        String top = "┌┄┄"; //$NON-NLS-1$
        String bottom = "└┄┄"; //$NON-NLS-1$
        for (int i = 0; i < str.length(); i++) {
            top += "┄"; //$NON-NLS-1$
            bottom += "┄"; //$NON-NLS-1$
        }
        top += "┐"; //$NON-NLS-1$
        bottom += "┘"; //$NON-NLS-1$
        String t = top + "\n┊ " + Font.italic(str) + " ┊\n" + bottom; //$NON-NLS-1$ //$NON-NLS-2$
        println(t);
    }

    /**
     * <pre>
     * Print a decorated text as subsubsubsubsection:
     *    str
     *    ‾‾‾
     * </pre>
     * @param str The text
     */
    public void println_subsubsubsubsection(String str) {
        if (!shouldPrint(ANALYZE)) {
            return;
        }

        String e = ""; //$NON-NLS-1$
        for (int i = 0; i < str.length(); i++) {
            e += "‾"; //$NON-NLS-1$
        }
        String t = str + "\n" + e; //$NON-NLS-1$
        println(t);
    }

    /**
     * Print a table in the stream
     * @param header The String array containing the header for the table
     * @param strs The String array of String arrays representing the content of the table
     * @param rightAlign The collection indexes of columns that have to be right aligned
     */
    public void println_table(Object[] header, Object[][] strs, Collection<Integer> rightAlign) {
        if (!shouldPrint(ANALYZE)) {
            return;
        }

        println_table(header, strs, rightAlign, false);
    }

    /**
     * Print a table in the stream
     * @param header The String array containing the header for the table
     * @param strs The String array of String arrays representing the content of the table
     * @param rightAlign The collection indexes of columns that have to be right aligned
     * @param bottomAlign To align the rows to the bottom for multiline cases
     */
    public void println_table(Object[] header, Object[][] strs, Collection<Integer> rightAlign, boolean bottomAlign) {
        if (!shouldPrint(ANALYZE)) {
            return;
        }

        ArrayList<Integer> columnsSize = new ArrayList<>();
        ArrayList<Integer> rowsSize = new ArrayList<>();

        // Get the size of each column
        for (int i = 0; i < strs.length; i++) {
            Object[] row = strs[i];

            for (int j = 0; j < row.length; j++) {
                String data = row[j].toString();
                String[] splittedData = data.split("\n"); //$NON-NLS-1$

                if (i == rowsSize.size()) {
                    rowsSize.add(splittedData.length);
                } else if (splittedData.length > rowsSize.get(i)) {
                    rowsSize.set(i, splittedData.length);
                }

                for (String cellLine : splittedData) {
                    if (j == columnsSize.size()) {
                        columnsSize.add(cellLine.length());
                    } else if (cellLine.length() > columnsSize.get(j)) {
                        columnsSize.set(j, cellLine.length());
                    }
                }
            }
        }

        // Verify also if the header is not outside of that
        for (int j = 0; j < header.length; j++) {
            String data = header[j].toString();

            if (j == columnsSize.size()) {
                columnsSize.add(data.length());
            } else if (data.length() > columnsSize.get(j)) {
                columnsSize.set(j, data.length());
            }
        }

        // Generate the separator lines
        String topSep = "┌"; //$NON-NLS-1$
        String headerSep = "╞"; //$NON-NLS-1$
        String bottomSep = "└"; //$NON-NLS-1$
        for (int j = 0; j < columnsSize.size(); j++) {
            int size = columnsSize.get(j) + 2;
            for (int k = 0; k < size; k++) {
                topSep += "─"; //$NON-NLS-1$
                headerSep += "═"; //$NON-NLS-1$
                bottomSep += "─"; //$NON-NLS-1$
            }
            if (j < columnsSize.size() - 1) {
                topSep += "┬"; //$NON-NLS-1$
                headerSep += "╪"; //$NON-NLS-1$
                bottomSep += "┴"; //$NON-NLS-1$
            }
        }
        topSep += "┐"; //$NON-NLS-1$
        bottomSep += "┘"; //$NON-NLS-1$
        headerSep += "╡"; //$NON-NLS-1$

        // Print the top line
        println(topSep);

        // Print the header
        String headerLine = "│"; //$NON-NLS-1$
        for (int j = 0; j < header.length; j++) {
            String data = header[j].toString();
            while (data.length() < columnsSize.get(j)) {
                data += " "; //$NON-NLS-1$
            }
            headerLine += " " + data + " │"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        println(headerLine);

        // Print the header separator
        println(headerSep);

        // Start printing the content
        for (int i = 0; i < strs.length; i++) {
            Object[] row = strs[i];

            for (int k = 0; k < rowsSize.get(i); k++) {
                String line = "│"; //$NON-NLS-1$
                for (int j = 0; j < row.length; j++) {
                    String data = row[j].toString();
                    String[] splittedData = data.split("\n"); //$NON-NLS-1$
                    String cellLine = ""; //$NON-NLS-1$

                    if (bottomAlign) {
                        int idk = k - (rowsSize.get(i) - splittedData.length);
                        if (idk >= 0) {
                            cellLine = splittedData[idk];
                        }
                    } else {
                        if (k < splittedData.length) {
                            cellLine = splittedData[k];
                        }
                    }

                    while (cellLine.length() < columnsSize.get(j)) {
                        if (rightAlign != null && rightAlign.contains(j)) {
                            cellLine = " " + cellLine; //$NON-NLS-1$
                        } else {
                            cellLine += " "; //$NON-NLS-1$
                        }
                    }
                    line += " " + cellLine + " │"; //$NON-NLS-1$ //$NON-NLS-2$
                }
                println(line);
            }
        }

        // Print the bottom line
        println(bottomSep);
    }

    /**
     * Print a new line
     */
    public void println() {
        if (!shouldPrint(ANALYZE)) {
            return;
        }

        output.println();
    }

    /**
     * Print the given object with the right tabulation level
     * @param obj The object to print
     */
    public void println(Object obj) {
        if (!shouldPrint(ANALYZE)) {
            return;
        }

        String s = obj.toString().replaceAll("\n", "\n" + tabStr()); //$NON-NLS-1$ //$NON-NLS-2$
        s = tabStr() + s;
        output.println(s);
    }

    /**
     * Add to the tabulation level
     */
    public synchronized void inc() {
        level++;
        tabStr = null;
    }

    /**
     * Remove from the tabulation level
     */
    public synchronized void dec() {
        level--;
        tabStr = null;
    }

    private synchronized String tabStr() {
        if (tabStr == null) {
            tabStr = Strings.padStart("", level, SHIFT); //$NON-NLS-1$
        }
        return tabStr;
    }

    /**
     * Print a progress bar
     * @param ratio The ratio to print (in percent)
     * @param progressSize The size of the progress bar
     * @return A String representing the progress bar
     */
    public static String progressBar(double ratio, int progressSize) {
        int size = 0;
        double chunkSize = 100.0 / progressSize;
        double cratio = ratio;

        String symbol = ""; //$NON-NLS-1$
        while (cratio >= chunkSize
                || (Math.abs(chunkSize - cratio) <= cratio
                    && Math.abs(chunkSize - cratio) <= Math.abs((chunkSize / 2) - cratio))) {
            cratio -= chunkSize;
            symbol += "█"; //$NON-NLS-1$
            size++;
        }

        if (cratio >= (7 * chunkSize / 8)
                || Math.abs((7 * chunkSize / 8) - cratio) <= cratio) {
            symbol += "▉"; //$NON-NLS-1$
            size++;
        } else if (cratio >= (3 * chunkSize / 4)
                || Math.abs((3 * chunkSize / 4) - cratio) <= cratio) {
            symbol += "▊"; //$NON-NLS-1$
            size++;
        } else if (cratio >= (5 * chunkSize / 8)
                || Math.abs((5 * chunkSize / 8) - cratio) <= cratio) {
            symbol += "▋"; //$NON-NLS-1$
            size++;
        } else if (cratio >= (chunkSize / 2)
                || Math.abs((chunkSize / 2) - cratio) <= cratio) {
            symbol += "▌"; //$NON-NLS-1$
            size++;
        } else if (cratio >= (3 * chunkSize / 8)
                || Math.abs((3 * chunkSize / 8) - cratio) <= cratio) {
            symbol += "▍"; //$NON-NLS-1$
            size++;
        } else if (cratio >= (chunkSize / 4)
                || Math.abs((chunkSize / 4) - cratio) <= cratio) {
            symbol += "▎"; //$NON-NLS-1$
            size++;
        } else if (cratio >= (chunkSize / 8)
                || Math.abs((chunkSize / 8) - cratio) <= cratio) {
            symbol += "▏"; //$NON-NLS-1$
            size++;
        }

        while (size < progressSize) {
            symbol += " "; //$NON-NLS-1$
            size++;
        }

        return symbol;
    }

    /**
     * Print the number of dropped instances during the analysis, and the reason
     * why they were dropped
     * @param smva The state machine variable analysis instance
     */
    public void printDropped(StateMachineVariableAnalysis smva) {
        if (!shouldPrint(ANALYZE)) {
            return;
        }

        if (smva.getDroppedInstances() > 0) {
            StateMachineReport.R.println(String.format("%d instances not considered:", smva.getDroppedInstances())); //$NON-NLS-1$
            if (smva.getDroppedInstances(StateMachineVariableAnalysis.NO_STATE_SYSTEM_FOUND) > 0) {
                StateMachineReport.R.println(String.format(" - %d because no state system analysis module was found.", smva.getDroppedInstances(StateMachineVariableAnalysis.NO_STATE_SYSTEM_FOUND))); //$NON-NLS-1$
            }
            if (smva.getDroppedInstances(StateMachineVariableAnalysis.NO_TID_FOUND) > 0) {
                StateMachineReport.R.println(String.format(" - %d because no tid was found.", smva.getDroppedInstances(StateMachineVariableAnalysis.NO_TID_FOUND))); //$NON-NLS-1$
            }
            if (smva.getDroppedInstances(StateMachineVariableAnalysis.OUT_OF_TIME_RANGE) > 0) {
                StateMachineReport.R.println(String.format(" - %d because the interval was out of state system time range.", smva.getDroppedInstances(StateMachineVariableAnalysis.OUT_OF_TIME_RANGE))); //$NON-NLS-1$
            }
            StateMachineReport.R.println();
        }
    }

    @SuppressWarnings("javadoc")
    public static class Font {

        private static String convert(String str, Map<String, String> map) {
            String n = ""; //$NON-NLS-1$
            for (int i = 0; i < str.length(); i++) {
                String c = str.substring(i, i + 1);
                if (map.containsKey(c)) {
                    n += map.get(c);
                } else {
                    n += c;
                }
            }

            return n;
        }

        public static String bold(String str) {
            Map<String, String> bold = new HashMap<>();
            bold.put("A", "𝐀"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("B", "𝐁"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("C", "𝐂"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("D", "𝐃"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("E", "𝐄"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("F", "𝐅"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("G", "𝐆"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("H", "𝐇"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("I", "𝐈"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("J", "𝐉"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("K", "𝐊"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("L", "𝐋"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("M", "𝐌"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("N", "𝐍"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("O", "𝐎"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("P", "𝐏"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("Q", "𝐐"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("R", "𝐑"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("S", "𝐒"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("T", "𝐓"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("U", "𝐔"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("V", "𝐕"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("W", "𝐖"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("X", "𝐗"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("Y", "𝐘"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("Z", "𝐙"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("a", "𝐚"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("b", "𝐛"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("c", "𝐜"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("d", "𝐝"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("e", "𝐞"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("f", "𝐟"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("g", "𝐠"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("h", "𝐡"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("i", "𝐢"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("j", "𝐣"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("k", "𝐤"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("l", "𝐥"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("m", "𝐦"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("n", "𝐧"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("o", "𝐨"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("p", "𝐩"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("q", "𝐪"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("r", "𝐫"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("s", "𝐬"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("t", "𝐭"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("u", "𝐮"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("v", "𝐯"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("w", "𝐰"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("x", "𝐱"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("y", "𝐲"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("z", "𝐳"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("0", "𝟎"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("1", "𝟏"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("2", "𝟐"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("3", "𝟑"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("4", "𝟒"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("5", "𝟓"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("6", "𝟔"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("7", "𝟕"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("8", "𝟖"); //$NON-NLS-1$ //$NON-NLS-2$
            bold.put("9", "𝟗"); //$NON-NLS-1$ //$NON-NLS-2$

            return convert(str, bold);
        }

        public static String italic(String str) {
            Map<String, String> italic = new HashMap<>();
            italic.put("A", "𝐴"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("B", "𝐵"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("C", "𝐶"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("D", "𝐷"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("E", "𝐸"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("F", "𝐹"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("G", "𝐺"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("H", "𝐻"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("I", "𝐼"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("J", "𝐽"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("K", "𝐾"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("L", "𝐿"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("M", "𝑀"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("N", "𝑁"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("O", "𝑂"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("P", "𝑃"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("Q", "𝑄"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("R", "𝑅"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("S", "𝑆"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("T", "𝑇"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("U", "𝑈"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("V", "𝑉"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("W", "𝑊"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("X", "𝑋"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("Y", "𝑌"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("Z", "𝑍"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("a", "𝑎"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("b", "𝑏"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("c", "𝑐"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("d", "𝑑"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("e", "𝑒"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("f", "𝑓"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("g", "𝑔"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("h", "ℎ"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("i", "𝑖"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("j", "𝑗"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("k", "𝑘"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("l", "𝑙"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("m", "𝑚"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("n", "𝑛"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("o", "𝑜"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("p", "𝑝"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("q", "𝑞"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("r", "𝑟"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("s", "𝑠"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("t", "𝑡"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("u", "𝑢"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("v", "𝑣"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("w", "𝑤"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("x", "𝑥"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("y", "𝑦"); //$NON-NLS-1$ //$NON-NLS-2$
            italic.put("z", "𝑧"); //$NON-NLS-1$ //$NON-NLS-2$

            return convert(str, italic);
        }

        public static String boldItalic(String str) {
            Map<String, String> boldItalic = new HashMap<>();
            boldItalic.put("A", "𝑨"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("B", "𝑩"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("C", "𝑪"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("D", "𝑫"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("E", "𝑬"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("F", "𝑭"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("G", "𝑮"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("H", "𝑯"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("I", "𝑰"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("J", "𝑱"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("K", "𝑲"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("L", "𝑳"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("M", "𝑴"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("N", "𝑵"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("O", "𝑶"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("P", "𝑷"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("Q", "𝑸"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("R", "𝑹"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("S", "𝑺"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("T", "𝑻"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("U", "𝑼"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("V", "𝑽"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("W", "𝑾"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("X", "𝑿"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("Y", "𝒀"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("Z", "𝒁"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("a", "𝒂"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("b", "𝒃"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("c", "𝒄"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("d", "𝒅"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("e", "𝒆"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("f", "𝒇"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("g", "𝒈"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("h", "𝒉"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("i", "𝒊"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("j", "𝒋"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("k", "𝒌"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("l", "𝒍"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("m", "𝒎"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("n", "𝒏"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("o", "𝒐"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("p", "𝒑"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("q", "𝒒"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("r", "𝒓"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("s", "𝒔"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("t", "𝒕"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("u", "𝒖"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("v", "𝒗"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("w", "𝒘"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("x", "𝒙"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("y", "𝒚"); //$NON-NLS-1$ //$NON-NLS-2$
            boldItalic.put("z", "𝒛"); //$NON-NLS-1$ //$NON-NLS-2$

            return convert(str, boldItalic);
        }

        public static String script(String str) {
            Map<String, String> script = new HashMap<>();
            script.put("A", "𝒜"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("B", "ℬ"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("C", "𝒞"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("D", "𝒟"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("E", "ℰ"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("F", "ℱ"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("G", "𝒢"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("H", "ℋ"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("I", "ℐ"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("J", "𝒥"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("K", "𝒦"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("L", "ℒ"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("M", "ℳ"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("N", "𝒩"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("O", "𝒪"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("P", "𝒫"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("Q", "𝒬"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("R", "ℛ"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("S", "𝒮"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("T", "𝒯"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("U", "𝒰"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("V", "𝒱"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("W", "𝒲"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("X", "𝒳"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("Y", "𝒴"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("Z", "𝒵"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("a", "𝒶"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("b", "𝒷"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("c", "𝒸"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("d", "𝒹"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("e", "ℯ"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("f", "𝒻"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("g", "ℊ"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("h", "𝒽"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("i", "𝒾"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("j", "𝒿"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("k", "𝓀"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("l", "𝓁"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("m", "𝓂"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("n", "𝓃"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("o", "ℴ"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("p", "𝓅"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("q", "𝓆"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("r", "𝓇"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("s", "𝓈"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("t", "𝓉"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("u", "𝓊"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("v", "𝓋"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("w", "𝓌"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("x", "𝓍"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("y", "𝓎"); //$NON-NLS-1$ //$NON-NLS-2$
            script.put("z", "𝓏"); //$NON-NLS-1$ //$NON-NLS-2$

            return convert(str, script);
        }

        public static String boldScript(String str) {
            Map<String, String> boldScript = new HashMap<>();
            boldScript.put("A", "𝓐"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("B", "𝓑"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("C", "𝓒"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("D", "𝓓"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("E", "𝓔"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("F", "𝓕"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("G", "𝓖"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("H", "𝓗"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("I", "𝓘"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("J", "𝓙"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("K", "𝓚"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("L", "𝓛"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("M", "𝓜"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("N", "𝓝"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("O", "𝓞"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("P", "𝓟"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("Q", "𝓠"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("R", "𝓡"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("S", "𝓢"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("T", "𝓣"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("U", "𝓤"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("V", "𝓥"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("W", "𝓦"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("X", "𝓧"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("Y", "𝓨"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("Z", "𝓩"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("a", "𝓪"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("b", "𝓫"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("c", "𝓬"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("d", "𝓭"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("e", "𝓮"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("f", "𝓯"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("g", "𝓰"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("h", "𝓱"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("i", "𝓲"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("j", "𝓳"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("k", "𝓴"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("l", "𝓵"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("m", "𝓶"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("n", "𝓷"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("o", "𝓸"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("p", "𝓹"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("q", "𝓺"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("r", "𝓻"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("s", "𝓼"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("t", "𝓽"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("u", "𝓾"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("v", "𝓿"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("w", "𝔀"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("x", "𝔁"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("y", "𝔂"); //$NON-NLS-1$ //$NON-NLS-2$
            boldScript.put("z", "𝔃"); //$NON-NLS-1$ //$NON-NLS-2$

            return convert(str, boldScript);
        }

        public static String fraktur(String str) {
            Map<String, String> fraktur = new HashMap<>();
            fraktur.put("A", "𝔄"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("B", "𝔅"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("C", "ℭ"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("D", "𝔇"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("E", "𝔈"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("F", "𝔉"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("G", "𝔊"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("H", "ℌ"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("I", "ℑ"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("J", "𝔍"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("K", "𝔎"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("L", "𝔏"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("M", "𝔐"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("N", "𝔑"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("O", "𝔒"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("P", "𝔓"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("Q", "𝔔"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("R", "ℜ"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("S", "𝔖"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("T", "𝔗"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("U", "𝔘"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("V", "𝔙"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("W", "𝔚"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("X", "𝔛"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("Y", "𝔜"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("Z", "ℨ"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("a", "𝔞"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("b", "𝔟"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("c", "𝔠"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("d", "𝔡"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("e", "𝔢"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("f", "𝔣"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("g", "𝔤"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("h", "𝔥"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("i", "𝔦"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("j", "𝔧"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("k", "𝔨"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("l", "𝔩"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("m", "𝔪"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("n", "𝔫"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("o", "𝔬"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("p", "𝔭"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("q", "𝔮"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("r", "𝔯"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("s", "𝔰"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("t", "𝔱"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("u", "𝔲"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("v", "𝔳"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("w", "𝔴"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("x", "𝔵"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("y", "𝔶"); //$NON-NLS-1$ //$NON-NLS-2$
            fraktur.put("z", "𝔷"); //$NON-NLS-1$ //$NON-NLS-2$

            return convert(str, fraktur);
        }

        public static String boldFraktur(String str) {
            Map<String, String> boldFraktur = new HashMap<>();
            boldFraktur.put("A", "𝕬"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("B", "𝕭"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("C", "𝕮"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("D", "𝕯"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("E", "𝕰"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("F", "𝕱"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("G", "𝕲"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("H", "𝕳"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("I", "𝕴"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("J", "𝕵"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("K", "𝕶"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("L", "𝕷"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("M", "𝕸"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("N", "𝕹"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("O", "𝕺"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("P", "𝕻"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("Q", "𝕼"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("R", "𝕽"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("S", "𝕾"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("T", "𝕿"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("U", "𝖀"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("V", "𝖁"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("W", "𝖂"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("X", "𝖃"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("Y", "𝖄"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("Z", "𝖅"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("a", "𝖆"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("b", "𝖇"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("c", "𝖈"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("d", "𝖉"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("e", "𝖊"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("f", "𝖋"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("g", "𝖌"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("h", "𝖍"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("i", "𝖎"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("j", "𝖏"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("k", "𝖐"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("l", "𝖑"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("m", "𝖒"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("n", "𝖓"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("o", "𝖔"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("p", "𝖕"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("q", "𝖖"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("r", "𝖗"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("s", "𝖘"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("t", "𝖙"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("u", "𝖚"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("v", "𝖛"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("w", "𝖜"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("x", "𝖝"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("y", "𝖞"); //$NON-NLS-1$ //$NON-NLS-2$
            boldFraktur.put("z", "𝖟"); //$NON-NLS-1$ //$NON-NLS-2$

            return convert(str, boldFraktur);
        }

        public static String doubleStruck(String str) {
            Map<String, String> doubleStruck = new HashMap<>();
            doubleStruck.put("A", "𝔸"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("B", "𝔹"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("C", "ℂ"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("D", "𝔻"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("E", "𝔼"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("F", "𝔽"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("G", "𝔾"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("H", "ℍ"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("I", "𝕀"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("J", "𝕁"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("K", "𝕂"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("L", "𝕃"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("M", "𝕄"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("N", "ℕ"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("O", "𝕆"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("P", "ℙ"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("Q", "ℚ"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("R", "ℝ"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("S", "𝕊"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("T", "𝕋"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("U", "𝕌"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("V", "𝕍"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("W", "𝕎"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("X", "𝕏"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("Y", "𝕐"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("Z", "ℤ"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("a", "𝕒"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("b", "𝕓"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("c", "𝕔"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("d", "𝕕"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("e", "𝕖"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("f", "𝕗"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("g", "𝕘"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("h", "𝕙"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("i", "𝕚"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("j", "𝕛"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("k", "𝕜"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("l", "𝕝"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("m", "𝕞"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("n", "𝕟"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("o", "𝕠"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("p", "𝕡"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("q", "𝕢"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("r", "𝕣"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("s", "𝕤"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("t", "𝕥"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("u", "𝕦"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("v", "𝕧"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("w", "𝕨"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("x", "𝕩"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("y", "𝕪"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("z", "𝕫"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("0", "𝟘"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("1", "𝟙"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("2", "𝟚"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("3", "𝟛"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("4", "𝟜"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("5", "𝟝"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("6", "𝟞"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("7", "𝟟"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("8", "𝟠"); //$NON-NLS-1$ //$NON-NLS-2$
            doubleStruck.put("9", "𝟡"); //$NON-NLS-1$ //$NON-NLS-2$

            return convert(str, doubleStruck);
        }

        public static String sansSerif(String str) {
            Map<String, String> sansSerif = new HashMap<>();
            sansSerif.put("A", "𝖠"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("B", "𝖡"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("C", "𝖢"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("D", "𝖣"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("E", "𝖤"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("F", "𝖥"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("G", "𝖦"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("H", "𝖧"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("I", "𝖨"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("J", "𝖩"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("K", "𝖪"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("L", "𝖫"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("M", "𝖬"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("N", "𝖭"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("O", "𝖮"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("P", "𝖯"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("Q", "𝖰"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("R", "𝖱"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("S", "𝖲"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("T", "𝖳"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("U", "𝖴"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("V", "𝖵"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("W", "𝖶"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("X", "𝖷"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("Y", "𝖸"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("Z", "𝖹"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("a", "𝖺"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("b", "𝖻"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("c", "𝖼"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("d", "𝖽"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("e", "𝖾"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("f", "𝖿"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("g", "𝗀"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("h", "𝗁"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("i", "𝗂"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("j", "𝗃"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("k", "𝗄"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("l", "𝗅"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("m", "𝗆"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("n", "𝗇"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("o", "𝗈"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("p", "𝗉"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("q", "𝗊"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("r", "𝗋"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("s", "𝗌"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("t", "𝗍"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("u", "𝗎"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("v", "𝗏"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("w", "𝗐"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("x", "𝗑"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("y", "𝗒"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("z", "𝗓"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("0", "𝟢"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("1", "𝟣"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("2", "𝟤"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("3", "𝟥"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("4", "𝟦"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("5", "𝟧"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("6", "𝟨"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("7", "𝟩"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("8", "𝟪"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerif.put("9", "𝟫"); //$NON-NLS-1$ //$NON-NLS-2$

            return convert(str, sansSerif);
        }

        public static String sansSerifBold(String str) {
            Map<String, String> sansSerifBold = new HashMap<>();
            sansSerifBold.put("A", "𝗔"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("B", "𝗕"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("C", "𝗖"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("D", "𝗗"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("E", "𝗘"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("F", "𝗙"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("G", "𝗚"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("H", "𝗛"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("I", "𝗜"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("J", "𝗝"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("K", "𝗞"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("L", "𝗟"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("M", "𝗠"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("N", "𝗡"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("O", "𝗢"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("P", "𝗣"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("Q", "𝗤"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("R", "𝗥"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("S", "𝗦"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("T", "𝗧"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("U", "𝗨"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("V", "𝗩"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("W", "𝗪"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("X", "𝗫"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("Y", "𝗬"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("Z", "𝗭"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("a", "𝗮"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("b", "𝗯"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("c", "𝗰"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("d", "𝗱"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("e", "𝗲"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("f", "𝗳"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("g", "𝗴"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("h", "𝗵"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("i", "𝗶"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("j", "𝗷"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("k", "𝗸"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("l", "𝗹"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("m", "𝗺"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("n", "𝗻"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("o", "𝗼"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("p", "𝗽"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("q", "𝗾"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("r", "𝗿"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("s", "𝘀"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("t", "𝘁"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("u", "𝘂"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("v", "𝘃"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("w", "𝘄"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("x", "𝘅"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("y", "𝘆"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("z", "𝘇"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("0", "𝟬"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("1", "𝟭"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("2", "𝟮"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("3", "𝟯"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("4", "𝟰"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("5", "𝟱"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("6", "𝟲"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("7", "𝟳"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("8", "𝟴"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBold.put("9", "𝟵"); //$NON-NLS-1$ //$NON-NLS-2$

            return convert(str, sansSerifBold);
        }

        public static String sansSerifItalic(String str) {
            Map<String, String> sansSerifItalic = new HashMap<>();
            sansSerifItalic.put("A", "𝘈"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("B", "𝘉"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("C", "𝘊"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("D", "𝘋"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("E", "𝘌"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("F", "𝘍"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("G", "𝘎"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("H", "𝘏"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("I", "𝘐"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("J", "𝘑"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("K", "𝘒"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("L", "𝘓"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("M", "𝘔"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("N", "𝘕"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("O", "𝘖"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("P", "𝘗"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("Q", "𝘘"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("R", "𝘙"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("S", "𝘚"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("T", "𝘛"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("U", "𝘜"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("V", "𝘝"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("W", "𝘞"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("X", "𝘟"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("Y", "𝘠"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("Z", "𝘡"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("a", "𝘢"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("b", "𝘣"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("c", "𝘤"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("d", "𝘥"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("e", "𝘦"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("f", "𝘧"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("g", "𝘨"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("h", "𝘩"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("i", "𝘪"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("j", "𝘫"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("k", "𝘬"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("l", "𝘭"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("m", "𝘮"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("n", "𝘯"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("o", "𝘰"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("p", "𝘱"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("q", "𝘲"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("r", "𝘳"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("s", "𝘴"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("t", "𝘵"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("u", "𝘶"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("v", "𝘷"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("w", "𝘸"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("x", "𝘹"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("y", "𝘺"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifItalic.put("z", "𝘻"); //$NON-NLS-1$ //$NON-NLS-2$

            return convert(str, sansSerifItalic);
        }

        public static String sansSerifBoldItalic(String str) {
            Map<String, String> sansSerifBoldItalic = new HashMap<>();
            sansSerifBoldItalic.put("A", "𝘼"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("B", "𝘽"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("C", "𝘾"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("D", "𝘿"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("E", "𝙀"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("F", "𝙁"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("G", "𝙂"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("H", "𝙃"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("I", "𝙄"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("J", "𝙅"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("K", "𝙆"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("L", "𝙇"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("M", "𝙈"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("N", "𝙉"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("O", "𝙊"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("P", "𝙋"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("Q", "𝙌"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("R", "𝙍"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("S", "𝙎"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("T", "𝙏"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("U", "𝙐"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("V", "𝙑"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("W", "𝙒"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("X", "𝙓"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("Y", "𝙔"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("Z", "𝙕"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("a", "𝙖"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("b", "𝙗"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("c", "𝙘"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("d", "𝙙"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("e", "𝙚"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("f", "𝙛"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("g", "𝙜"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("h", "𝙝"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("i", "𝙞"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("j", "𝙟"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("k", "𝙠"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("l", "𝙡"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("m", "𝙢"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("n", "𝙣"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("o", "𝙤"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("p", "𝙥"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("q", "𝙦"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("r", "𝙧"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("s", "𝙨"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("t", "𝙩"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("u", "𝙪"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("v", "𝙫"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("w", "𝙬"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("x", "𝙭"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("y", "𝙮"); //$NON-NLS-1$ //$NON-NLS-2$
            sansSerifBoldItalic.put("z", "𝙯"); //$NON-NLS-1$ //$NON-NLS-2$

            return convert(str, sansSerifBoldItalic);
        }
    }

    public void setOutput(MessageConsoleStream consoleStream) {
        output = new RTOutputStream(consoleStream);
    }

}
