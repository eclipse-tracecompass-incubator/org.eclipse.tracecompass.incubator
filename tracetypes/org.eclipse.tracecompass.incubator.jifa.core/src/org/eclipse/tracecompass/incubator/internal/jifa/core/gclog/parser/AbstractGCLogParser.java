/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.MS2S;

import java.io.BufferedReader;
import java.util.List;

import org.eclipse.tracecompass.incubator.internal.jifa.core.Activator;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.Safepoint;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModelFactory;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.ParseRule.ParseRuleContext;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;

public abstract class AbstractGCLogParser implements GCLogParser {
    private GCModel model;
    private GCLogParsingMetadata metadata;

    public GCLogParsingMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(GCLogParsingMetadata metadata) {
        this.metadata = metadata;
    }

    protected GCModel getModel() {
        return model;
    }

    // for the sake of performance, will try to use less regular expression
    @Override
    public final GCModel parse(BufferedReader br) throws TmfTraceException {
        model = GCModelFactory.getModel(metadata.getCollector());
        model.setLogStyle(metadata.getStyle());
        String line = "";
        try {
            while ((line = br.readLine()) != null) {
                try {
                    if (line.length() > 0) {
                        doParseLine(line);
                    }
                } catch (Exception e) {
                    Activator.getInstance().logInfo(String.format("fail to parse \"{}\", {}", line, e.getMessage()));
                }
            }
            endParsing();
        } catch (Exception e) {
            Activator.getInstance().logInfo(String.format("fail to parse \"{}\", {}", line, e.getMessage()));
        }

        return model;
    }

    protected abstract void doParseLine(String line);

    protected void endParsing() {
    }

    // return true if text can be parsed by any rule
    // order of rules matters
    protected boolean doParseUsingRules(AbstractGCLogParser parser, ParseRuleContext context, String text, List<ParseRule> rules) {
        for (ParseRule rule : rules) {
            if (rule.doParse(parser, context, text)) {
                return true;
            }
        }
        return false;
    }

    // Total time for which application threads were stopped: 0.0001215 seconds,
    // Stopping threads took: 0.0000271 seconds
    protected void parseSafepointStop(double uptime, String s) {
        int begin = s.indexOf("stopped: ") + "stopped: ".length();
        int end = s.indexOf(" seconds, ");
        double duration = Double.parseDouble(s.substring(begin, end)) * MS2S;
        begin = s.lastIndexOf("took: ") + "took: ".length();
        end = s.lastIndexOf(" seconds");
        double timeToEnter = Double.parseDouble(s.substring(begin, end)) * MS2S;
        Safepoint safepoint = new Safepoint();
        // safepoint is printed at the end of it
        safepoint.setStartTime(uptime - duration);
        safepoint.setDuration(duration);
        safepoint.setTimeToEnter(timeToEnter);
        getModel().addSafepoint(safepoint);
    }
}
