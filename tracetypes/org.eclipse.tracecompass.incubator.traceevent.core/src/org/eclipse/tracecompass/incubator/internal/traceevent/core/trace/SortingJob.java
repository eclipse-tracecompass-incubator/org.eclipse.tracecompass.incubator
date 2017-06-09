/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.trace;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.Activator;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/**
 * On-disk sorting job. It splits a trace into tracelets. Each tracelet is
 * sorted in ram and written to disk, then the tracelets are merged into a big
 * trace.
 *
 * @author Matthew Khouzam
 */
final class SortingJob extends Job {

    private static final int CHARS_PER_LINE_ESTIMATE = 50;
    private static final @NonNull Logger LOGGER = TraceCompassLog.getLogger(SortingJob.class);
    private static final int CHUNK_SIZE = 65535;

    private static final class Pair implements Comparable<Pair> {
        private static final @NonNull BigDecimal MINUS_ONE = BigDecimal.valueOf(-1);

        public Pair(String string, int i) {
            line = string;
            String key = "\"ts\":"; //$NON-NLS-1$
            int indexOf = string.indexOf(key);
            if (indexOf < 0) {
                ts = MINUS_ONE;
            } else {
                int index = indexOf + key.length();
                int end = string.indexOf(',', index);
                if (end == -1) {
                    end = string.indexOf('}', index);
                }
                String number = string.substring(index, end).trim();
                if (!number.isEmpty()) {
                    // This may be a bit slow, it can be optimized if need be.
                    ts = new BigDecimal(number);
                }
                pos = i;
            }
        }

        BigDecimal ts;
        String line;
        int pos;

        @Override
        public int compareTo(Pair o) {
            return ts.compareTo(o.ts);
        }
    }

    private final String fPath;
    private final ITmfTrace fTrace;

    public SortingJob(ITmfTrace trace, String path) {
        super(Messages.SortingJob_description);
        fTrace = trace;
        fPath = path;

    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        ITmfTrace trace = fTrace;
        IProgressMonitor subMonitor = SubMonitor.convert(monitor, 3);
        if (trace == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Trace cannot be null"); //$NON-NLS-1$
        }
        String dir = TmfTraceManager.getSupplementaryFileDir(trace);
        subMonitor.beginTask(Messages.SortingJob_sorting, (int) (new File(fPath).length() / CHARS_PER_LINE_ESTIMATE));
        subMonitor.subTask(Messages.SortingJob_splitting);
        File tempDir = new File(dir + ".tmp"); //$NON-NLS-1$
        tempDir.mkdirs();
        List<File> tracelings = new ArrayList<>();
        try (BufferedInputStream parser = new BufferedInputStream(new FileInputStream(fPath))) {
            char data = (char) parser.read();
            while (data != '[') {
                data = (char) parser.read();
            }
            List<Pair> events = new ArrayList<>(CHUNK_SIZE);
            String eventString = TraceEventTrace.readNextEventString(() -> (char) parser.read());
            if (eventString == null) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Empty event in " + fPath); //$NON-NLS-1$
            }
            Pair line = new Pair(eventString, 0);
            line.line = data + '"' + line.line;
            int cnt = 0;
            int filen = 0;
            while (eventString != null) {
                while (cnt < CHUNK_SIZE) {
                    events.add(line);
                    subMonitor.worked(1);
                    if (subMonitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    eventString = TraceEventTrace.readNextEventString(() -> (char) parser.read());
                    if (eventString == null) {
                        break;
                    }
                    line = new Pair(eventString, 0);
                    cnt++;
                }
                events.sort((o1, o2) -> o1.ts.compareTo(o2.ts));
                cnt = 0;
                File traceling = new File(tempDir + File.separator + "test" + filen + ".json"); //$NON-NLS-1$ //$NON-NLS-2$
                tracelings.add(traceling);
                traceling.createNewFile();
                try (PrintWriter fs = new PrintWriter(traceling)) {
                    fs.println('[');
                    for (Pair sortedEvent : events) {
                        fs.println(sortedEvent.line + ',');
                    }
                    fs.println(']');
                }
                events.clear();
                filen++;
                subMonitor.worked(1);
                if (subMonitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

            }
            subMonitor.subTask(Messages.SortingJob_merging);
            PriorityQueue<Pair> evs = new PriorityQueue<>();
            List<BufferedInputStream> parsers = new ArrayList<>();
            int i = 0;
            for (File traceling : tracelings) {

                /*
                 * This resource is added to a priority queue and then removed
                 * at the very end.
                 */
                @SuppressWarnings("resource")
                BufferedInputStream createParser = new BufferedInputStream(new FileInputStream(traceling));
                while (data != '{') {
                    data = (char) parser.read();
                    if (data == (char) -1) {
                        break;
                    }
                }
                eventString = TraceEventTrace.readNextEventString(() -> (char) createParser.read());
                Pair parse = new Pair(eventString, i);
                evs.add(parse);
                i++;
                parsers.add(createParser);
                subMonitor.worked(1);
                if (subMonitor.isCanceled()) {
                    break;
                }
            }
            if (subMonitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            File file = new File(dir + File.separator + new File(trace.getPath()).getName());
            file.createNewFile();

            try (PrintWriter tempWriter = new PrintWriter(file)) {
                tempWriter.println('[');
                while (!evs.isEmpty()) {
                    Pair sortedEvent = evs.poll();
                    Pair parse = readNextEvent(parsers.get(sortedEvent.pos), sortedEvent.pos);
                    if (parse != null) {
                        tempWriter.println(sortedEvent.line.trim() + ',');
                        evs.add(parse);
                    } else {
                        tempWriter.println(sortedEvent.line.trim() + (evs.isEmpty() ? "" : ',')); //$NON-NLS-1$
                    }
                    subMonitor.worked(1);
                    if (subMonitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                }
                tempWriter.println(']');
            }
            for (BufferedInputStream tmpParser : parsers) {
                tmpParser.close();
            }
        } catch (IOException e) {
            TraceCompassLogUtils.traceInstant(LOGGER, Level.WARNING, "IOException in sorting job", "trace", fPath, "exception", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } finally {
            for (File tl : tracelings) {
                tl.delete();
            }
            tempDir.delete();

            subMonitor.done();
        }
        return Status.OK_STATUS;

    }

    private static @Nullable Pair readNextEvent(BufferedInputStream parser, int i) throws IOException {
        String event = TraceEventTrace.readNextEventString(() -> (char) parser.read());
        return event == null ? null : new Pair(event, i);

    }
}