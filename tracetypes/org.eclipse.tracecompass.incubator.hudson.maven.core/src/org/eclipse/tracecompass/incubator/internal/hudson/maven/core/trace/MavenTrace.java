/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.hudson.maven.core.trace;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimePreferences;
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

    private static final char START_REGEX = '^';
    /** The time stamp format of the trace type. */
    private static final String DATE_FORMAT_STRING = "HH:mm:ss"; //$NON-NLS-1$
    private static final String DATE_PATTERN = "([0-2]?\\d:[0-5]\\d:[0-5]\\d)"; //$NON-NLS-1$
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING, TmfTimePreferences.getLocale());

    private static final String ELAPSED_TIME_STRING = "Time\\selapsed:\\s*([\\d|\\.]+)\\s*sec"; //$NON-NLS-1$
    /*
     * 16:17:03 [INFO] --- maven-clean-plugin:3.0.0:clean (default-clean) @
     * org.eclipse.tracecompass ---
     */
    private static final String GOAL_PATTERN = START_REGEX + DATE_PATTERN + "\\s*\\[(\\S+)\\]\\s+---\\s((.*)\\(.*\\))\\s+@\\s+(\\S+)\\s+---$"; //$NON-NLS-1$
    /*
     * Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 52.34 sec
     * - in org.eclipse.tracecompass.analysis.os.linux.ui.swtbot.tests.latency.
     * SystemCallLatencyTableAnalysisTest
     */
    private static final String TEST_SUMMARY_PATTERN = START_REGEX + DATE_PATTERN + "\\s*Tests\\srun:\\s(\\d+),\\sFailures:\\s(\\d+),\\sErrors:\\s(\\d+),\\sSkipped:\\s(\\d+),\\s*" + ELAPSED_TIME_STRING + "\\s*-\\sin\\s(.*)$"; //$NON-NLS-1$ //$NON-NLS-2$
    /*
     * 16:42:14
     * climbTest(org.eclipse.tracecompass.analysis.os.linux.ui.swtbot.tests.
     * latency.SystemCallLatencyTableAnalysisTest) Time elapsed: 0.162 sec
     */
    private static final String TEST_INDIVIDUAL_PATTERN = START_REGEX + DATE_PATTERN + "\\s*((.*)\\(.*\\))\\s*" + ELAPSED_TIME_STRING + ".*$"; //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * All three regexes ored.
     */
    private static final Pattern FIRST_LINE = Pattern.compile(GOAL_PATTERN + '|' + TEST_SUMMARY_PATTERN + '|' + TEST_INDIVIDUAL_PATTERN);

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
            if (group != null) {
                Date parse = DATE_FORMAT.parse(group);
                MavenEvent event = MavenEvent.createGoal(this, TmfTimestamp.fromMillis(parse.getTime()), matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5));
                return event;
            }
            group = matcher.group(6);
            if (group != null) {
                Date parse = DATE_FORMAT.parse(group);
                MavenEvent event = MavenEvent.createSummary(this, TmfTimestamp.fromMillis(parse.getTime()), matcher.group(12), matcher.group(12), Double.parseDouble(matcher.group(11)));
                return event;
            }
            group = matcher.group(13);
            if (group != null) {
                Date parse = DATE_FORMAT.parse(group);
                MavenEvent event = MavenEvent.createTest(this, TmfTimestamp.fromMillis(parse.getTime()), matcher.group(15), matcher.group(14), Double.parseDouble(matcher.group(16)));
                return event;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    @Override
    protected void parseNextLine(MavenEvent event, String line) {
        // one line events don't need this
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return MavenEvent.EVENT_ASPECTS;
    }

}
