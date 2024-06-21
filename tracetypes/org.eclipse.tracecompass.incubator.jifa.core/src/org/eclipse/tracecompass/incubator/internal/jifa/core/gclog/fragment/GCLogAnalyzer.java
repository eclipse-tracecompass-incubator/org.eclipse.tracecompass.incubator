/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.fragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.GCLogParser;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.GCLogParserFactory;

public class GCLogAnalyzer {
    private static final java.util.logging.Logger LOGGER = Logger.getAnonymousLogger();

    private final Map<Map<String, String>, GCLogParser> parserMap = new ConcurrentHashMap<>();

    public List<Metric> parseToMetrics(List<String> rawContext, Map<String, String> instanceId, long startTime, long endTime) throws Exception {
        Context context = new Context(rawContext);
        try (BufferedReader br = context.toBufferedReader()) {
            GCLogParser parser = selectParser(instanceId, br);
            GCModel model = parser.parse(br);
            if (!model.isEmpty()) {
                model.calculateDerivedInfo(null);
                return new GCModelConverter().toMetrics(model, instanceId, startTime, endTime);
            }
        }
        return null;
    }

    public GCModel parseToGCModel(List<String> rawContext, Map<String, String> instanceId) {
        Context context = new Context(rawContext);
        BufferedReader br = context.toBufferedReader();
        GCModel model = null;
        try {
            GCLogParser parser = selectParser(instanceId, br);
            model = parser.parse(br);
            br.close();
            if (!model.isEmpty()) {
                model.calculateDerivedInfo(null);
            } else {
                model = null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "fail to parse context " + e.getMessage());
            model = null;
        }
        return model;
    }

    private GCLogParser selectParser(Map<String, String> instanceId, BufferedReader br) throws IOException {
        GCLogParser parser = parserMap.get(instanceId);
        if (parser == null) {
            GCLogParserFactory logParserFactory = new GCLogParserFactory();
            // max length in hotspot
            int MAX_SINGLE_LINE_LENGTH = 2048;
            br.mark(GCLogParserFactory.MAX_ATTEMPT_LINE * MAX_SINGLE_LINE_LENGTH);
            parser = logParserFactory.getParser(br);
            br.reset();
            parserMap.put(instanceId, parser);
        }
        return parser;
    }
}

