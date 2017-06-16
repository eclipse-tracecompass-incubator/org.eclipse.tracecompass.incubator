/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.husdon.maven.core.trace;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.text.TextTrace;

/**
 * Maven Trace parser, parses outputs from hudson
 *
 * @author Matthew Khouzam
 * @author Marc-Andre Laperle
 *
 */
public class MavenTrace extends TextTrace<MavenEvent> {

    private static final Pattern FIRST_LINE = Pattern.compile("(\\S*)\\s*(\\[INFO\\])\\s+---((.*)\\(.*\\))\\s+@\\s+(\\S+)\\s+---"); //$NON-NLS-1$
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("hh:mm:ss"); //$NON-NLS-1$

    /**
     * Default Constructor
     */
    public MavenTrace() {
        // do nothing
    }

    @Override
    protected Pattern getFirstLinePattern() {
        return FIRST_LINE;
    }

    // TODO: make it accept more event types
    @Override
    protected MavenEvent parseFirstLine(Matcher matcher, String line) {
        try {
            String group = matcher.group(1);
            Date parse = DATE_FORMAT.parse(group);
            MavenEvent event = new MavenEvent(this, TmfTimestamp.fromMillis(parse.getTime()), matcher.group(5));
            return event;
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    @Override
    protected void parseNextLine(MavenEvent event, String line) {
        // one line events don't need this
    }

}
