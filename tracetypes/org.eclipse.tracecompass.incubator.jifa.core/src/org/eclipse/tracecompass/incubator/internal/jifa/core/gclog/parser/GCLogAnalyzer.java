/********************************************************************************
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.Activator;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;

public class GCLogAnalyzer {
    private final File file;
    private final IProgressMonitor listener;

    private final int MAX_SINGLE_LINE_LENGTH = 2048; // max length in hotspot

    public GCLogAnalyzer(File file, IProgressMonitor listener) {
        this.file = file;
        this.listener = listener;
    }

    public GCModel parse() throws TmfTraceException {

        GCLogParser parser = null;
        try (BufferedReader br = new BufferedReader(new FileReader(file))){
            listener.beginTask("Paring " + file.getName(), 1000);
            listener.setTaskName( "Deciding gc log format.");

            // decide log format

            GCLogParserFactory logParserFactory = new GCLogParserFactory();
            br.mark(GCLogParserFactory.MAX_ATTEMPT_LINE * MAX_SINGLE_LINE_LENGTH);
            parser = logParserFactory.getParser(br);
            listener.worked(100);
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))){

            // read original info from log file
            listener.setTaskName( "Parsing gc log file.");
            GCModel model = parser.parse(br);
            if (model.isEmpty()) {
                throw new TmfTraceException("Fail to find any gc event in this log.");
            }
            listener.worked(500);

            // calculate derived info for query from original info
            listener.setTaskName("Calculating information from original data.");
            model.calculateDerivedInfo(listener);

            return model;
        } catch (IOException | TmfTraceException e) {
            Activator.getInstance().logInfo(String.format("fail to parse gclog {0}: {1}", file.getName(), e.getMessage()));
            throw new TmfTraceException(e.getMessage(), e);
        }
    }
}
