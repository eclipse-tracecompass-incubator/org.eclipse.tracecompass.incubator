/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.golang.core.trace;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.golang.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCallsiteAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfDeviceAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfEventFieldAspect;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfCallsite;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.io.BufferedRandomAccessFile;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

/**
 * Golang Execution Tracer parser
 *
 * Doc is here. But it's not up to date.
 * https://docs.google.com/document/d/1FP5apqzBgr7ahCCgFO-yoVhk4YZrNIDNf9RybngBc14/edit#
 *
 * Painfully reverse engineered from golang 1.11, read code base which is BSD-3
 * clause
 *
 * @author Matthew Khouzam
 *
 */
public class GolangTrace extends TmfTrace {

    private static final String NAME = "name"; //$NON-NLS-1$

    @FunctionalInterface
    private interface EventHandler {
        ITmfEvent apply(long rank, DataInput reader) throws IOException;
    }

    private static final ITmfEventType GOBATCH = new TmfEventType("Batch", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOCPUSAMPLE = new TmfEventType("CPUSample", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOFREQUENCY = new TmfEventType("Frequency", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOFUTILEWAKEUP = new TmfEventType("FutileWakeup", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGCDONE = new TmfEventType("GCDone", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGCMARKASSISTDONE = new TmfEventType("GCMarkAssistDone", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGCMARKASSISTSTART = new TmfEventType("GCMarkAssistStart", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGCSTART = new TmfEventType("GCStart", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGCSTWDONE = new TmfEventType("GCSTWDone", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGCSTWSTART = new TmfEventType("GCSTWStart", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGCSWEEPDONE = new TmfEventType("GCSweepDone", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGCSWEEPSTART = new TmfEventType("GCSweepStart", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOBLOCK = new TmfEventType("GoBlock", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOBLOCKCOND = new TmfEventType("GoBlockCond", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOBLOCKGC = new TmfEventType("GoBlockGC", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOBLOCKNET = new TmfEventType("GoBlockNet", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOBLOCKRECV = new TmfEventType("GoBlockRecv", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOBLOCKSELECT = new TmfEventType("GoBlockSelect", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOBLOCKSEND = new TmfEventType("GoBlockSend", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOBLOCKSYNC = new TmfEventType("GoBlockSync", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOCREATE = new TmfEventType("GoCreate", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOEND = new TmfEventType("GoEnd", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOINSYSCALL = new TmfEventType("GoInSyscall", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOMAXPROCS = new TmfEventType("Gomaxprocs", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOPREEMPT = new TmfEventType("GoPreempt", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOSCHED = new TmfEventType("GoSched", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOSLEEP = new TmfEventType("GoSleep", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOSTART = new TmfEventType("GoStart", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOSTARTLABEL = new TmfEventType("GoStartLabel", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOSTARTLOCAL = new TmfEventType("GoStartLocal", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOSTOP = new TmfEventType("GoStop", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOSYSBLOCK = new TmfEventType("GoSysBlock", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOSYSCALL = new TmfEventType("GoSysCall", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOSYSEXIT = new TmfEventType("GoSysExit", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOSYSEXITLOCAL = new TmfEventType("GoSysExitLocal", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOUNBLOCK = new TmfEventType("GoUnblock", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOUNBLOCKLOCAL = new TmfEventType("GoUnblockLocal", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOGOWAITING = new TmfEventType("GoWaiting", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOHEAPALLOC = new TmfEventType("HeapAlloc", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOHEAPGOAL = new TmfEventType("HeapGoal", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GONONE = new TmfEventType("None", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOPROCSTART = new TmfEventType("ProcStart", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOPROCSTOP = new TmfEventType("ProcStop", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOSTACK = new TmfEventType("Stack", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOSTRING = new TmfEventType("String", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOTIMERGOROUTINE = new TmfEventType("TimerGoroutine", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOUSERLOG = new TmfEventType("UserLog", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOUSERREGION = new TmfEventType("UserRegion", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOUSERTASKCREATE = new TmfEventType("UserTaskCreate", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$
    private static final ITmfEventType GOUSERTASKEND = new TmfEventType("UserTaskEnd", new TmfEventField(ITmfEventField.ROOT_FIELD_ID, 0, null)); //$NON-NLS-1$

    private static final @NonNull Pattern HEADER_REGEX = Pattern.compile("go (\\d\\.\\d+) trace"); //$NON-NLS-1$

    private static final String G_FIELD = "g"; //$NON-NLS-1$
    private static final String P_FIELD = "p"; //$NON-NLS-1$
    private static final String STACK_ID = "StackID"; //$NON-NLS-1$
    private static final String OFFSET = "offset"; //$NON-NLS-1$

    private static String readUTF(DataInput reader) throws IOException {
        long size = LEB128Util.read(reader);

        if (size > 100000) {
            throw new IOException("size is too large : " + size); //$NON-NLS-1$
        }
        byte[] data = new byte[(int) size];
        reader.readFully(data);
        return new String(data);
    }

    private final List<ITmfEvent> fAwfulBackup = new ArrayList<>();

    private long fCurrentTime = 0;

    private @Nullable File fFile = null;

    private final EventHandler[] fHandlers = new EventHandler[256];

    private long fLastG = 0;

    private long fLastP = 0;

    private long fPosition = -1;

    private final Map<Long, String> fStringMap = new HashMap<>();
    private final List<ITmfEventAspect<?>> fAspects;
    private Map<Long, List<@NonNull ITmfCallsite>> fStacks = new HashMap<>();

    /**
     * Default and only constructor
     */
    public GolangTrace() {
        fHandlers[0] = (this::generateGoNone);
        fHandlers[1] = (this::generateGoBatch);
        fHandlers[2] = (this::generateGoFrequency);
        fHandlers[3] = (this::generateGoStack);
        fHandlers[4] = (this::generateGoGomaxprocs);
        fHandlers[5] = (this::generateGoProcStart);
        fHandlers[6] = (this::generateGoProcStop);
        fHandlers[7] = (this::generateGoGCStart);
        fHandlers[8] = (this::generateGoGCDone);
        fHandlers[9] = (this::generateGoGCSTWStart);
        fHandlers[10] = (this::generateGoGCSTWDone);
        fHandlers[11] = (this::generateGoGCSweepStart);
        fHandlers[12] = (this::generateGoGCSweepDone);
        fHandlers[13] = (this::generateGoGoCreate);
        fHandlers[14] = (this::generateGoGoStart);
        fHandlers[15] = (this::generateGoGoEnd);
        fHandlers[16] = (this::generateGoGoStop);
        fHandlers[17] = (this::generateGoGoSched);
        fHandlers[18] = (this::generateGoGoPreempt);
        fHandlers[19] = (this::generateGoGoSleep);
        fHandlers[20] = (this::generateGoGoBlock);
        fHandlers[21] = (this::generateGoGoUnblock);
        fHandlers[22] = (this::generateGoGoBlockSend);
        fHandlers[23] = (this::generateGoGoBlockRecv);
        fHandlers[24] = (this::generateGoGoBlockSelect);
        fHandlers[25] = (this::generateGoGoBlockSync);
        fHandlers[26] = (this::generateGoGoBlockCond);
        fHandlers[27] = (this::generateGoGoBlockNet);
        fHandlers[28] = (this::generateGoGoSysCall);
        fHandlers[29] = (this::generateGoGoSysExit);
        fHandlers[30] = (this::generateGoGoSysBlock);
        fHandlers[31] = (this::generateGoGoWaiting);
        fHandlers[32] = (this::generateGoGoInSyscall);
        fHandlers[33] = (this::generateGoHeapAlloc);
        fHandlers[34] = (this::generateGoHeapGoal);
        fHandlers[35] = (this::generateGoTimerGoroutine);
        fHandlers[36] = (this::generateGoFutileWakeup);
        fHandlers[37] = (this::generateGoString);
        fHandlers[38] = (this::generateGoGoStartLocal);
        fHandlers[39] = (this::generateGoGoUnblockLocal);
        fHandlers[40] = (this::generateGoGoSysExitLocal);
        fHandlers[41] = (this::generateGoGoStartLabel);
        fHandlers[42] = (this::generateGoGoBlockGC);
        fHandlers[43] = (this::generateGoGCMarkAssistStart);
        fHandlers[44] = (this::generateGoGCMarkAssistDone);
        fHandlers[45] = (this::generateGoUserTaskCreate);
        fHandlers[46] = (this::generateGoUserTaskEnd);
        fHandlers[47] = (this::generateGoUserRegion);
        fHandlers[48] = (this::generateGoUserLog);
        fHandlers[49] = (this::generateGoCPUSample);

        fAspects = new ArrayList<>();
        Iterable<ITmfEventAspect<?>> aspects = super.getEventAspects();
        for (ITmfEventAspect<?> aspect : aspects) {
            fAspects.add(aspect);
        }
        fAspects.add(new TmfEventFieldAspect(OFFSET, OFFSET, ITmfEvent::getContent));
        fAspects.add(new TmfEventFieldAspect(NAME, NAME, ITmfEvent::getContent));
        fAspects.add(new TmfEventFieldAspect("taskid", "taskid", ITmfEvent::getContent));
        fAspects.add(new TmfEventFieldAspect(P_FIELD, P_FIELD, ITmfEvent::getContent));
        fAspects.add(new TmfEventFieldAspect(G_FIELD, G_FIELD, ITmfEvent::getContent));
        fAspects.add(new TmfEventFieldAspect(STACK_ID, STACK_ID, ITmfEvent::getContent));
        fAspects.add(new TmfCallsiteAspect() {

            @Override
            public @Nullable List<@NonNull ITmfCallsite> resolve(@NonNull ITmfEvent event) {
                Long stack_id = event.getContent().getFieldValue(Long.class, STACK_ID);
                if (stack_id == null) {
                    return null;
                }
                List<@NonNull ITmfCallsite> stack = fStacks.get(stack_id);
                return stack;
            }

        });
        fAspects.add(new TmfDeviceAspect() {

            @Override
            public @NonNull String getName() {
                return P_FIELD;
            }

            @Override
            public @NonNull String getHelpText() {
                return "Process ID"; //$NON-NLS-1$
            }

            @Override
            public @Nullable Integer resolve(@NonNull ITmfEvent event) {
                Long fieldValue = event.getContent().getFieldValue(Long.class, P_FIELD);
                return fieldValue == null ? null : fieldValue.intValue();
            }

            @Override
            public @NonNull String getDeviceType() {
                return "Process"; //$NON-NLS-1$
            }

        });
        ITmfEventAspect<?> content = fAspects.remove(2);
        fAspects.add(content);

    }

    @Override
    public @NonNull Iterable<ITmfEventAspect<?>> getEventAspects() {
        return fAspects;
    }

    /**
     * EventBatch handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoBatch(long rank, DataInput reader) throws IOException {

        long p = LEB128Util.read(reader);
        fLastP = p;
        long ticks = LEB128Util.read(reader);
        setTime(ticks);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOBATCH,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(P_FIELD, p, null), new TmfEventField("ticks", ticks, null), new TmfEventField(G_FIELD, fLastG, null)) //$NON-NLS-1$
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventCPUSample handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoCPUSample(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        long ts = LEB128Util.read(reader);
        long p = LEB128Util.read(reader);
        long g = LEB128Util.read(reader);
        fLastG = g;
        fLastP = p;
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOCPUSAMPLE, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                Arrays.asList(new TmfEventField(STACK_ID, stackID, null), new TmfEventField("ts", ts, null), new TmfEventField(P_FIELD, p, null), //$NON-NLS-1$
                        new TmfEventField(G_FIELD, g, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventFrequency handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoFrequency(long rank, DataInput reader) throws IOException {

        long freq = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOFREQUENCY,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField("freq", freq, null), new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null)) //$NON-NLS-1$
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventFutileWakeup handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoFutileWakeup(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOFUTILEWAKEUP, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventGCDone handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGCDone(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGCDONE, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventGCMarkAssistDone handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGCMarkAssistDone(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGCMARKASSISTDONE, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventGCMarkAssistStart handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGCMarkAssistStart(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGCMARKASSISTSTART,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(STACK_ID, stackID, null), new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGCStart handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGCStart(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        long seq = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGCSTART,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, Arrays.asList(new TmfEventField(STACK_ID, stackID, null), new TmfEventField(G_FIELD, fLastG, null),
                        new TmfEventField(P_FIELD, fLastP, null), new TmfEventField("seq", seq, null)).toArray(new ITmfEventField[0]))); //$NON-NLS-1$
    }

    /**
     * EventGCSTWDone handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGCSTWDone(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGCSTWDONE, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventGCSTWStart handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGCSTWStart(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long kindid = LEB128Util.read(reader);
        String kind = kindid == 0 ? "mark termination" : kindid == 1 ? "sweep termination" : "unknown"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGCSTWSTART,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, Arrays.asList(new TmfEventField("kindid", kindid, null), new TmfEventField(G_FIELD, fLastG, null), //$NON-NLS-1$
                        new TmfEventField(P_FIELD, fLastP, null), new TmfEventField("kind", kind, null)).toArray(new ITmfEventField[0]))); //$NON-NLS-1$
    }

    /**
     * EventGCSweepDone handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGCSweepDone(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long swept = LEB128Util.read(reader);
        long reclaimed = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGCSWEEPDONE,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, Arrays.asList(new TmfEventField("swept", swept, null), new TmfEventField("reclaimed", reclaimed, null), new TmfEventField(G_FIELD, fLastG, null), //$NON-NLS-1$ //$NON-NLS-2$
                        new TmfEventField(P_FIELD, fLastP, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventGCSweepStart handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGCSweepStart(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGCSWEEPSTART,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(STACK_ID, stackID, null), new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoBlock handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoBlock(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOBLOCK,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoBlockCond handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoBlockCond(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOBLOCKCOND,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoBlockGC handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoBlockGC(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOBLOCKGC,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(STACK_ID, stackID, null), new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoBlockNet handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoBlockNet(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOBLOCKNET,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoBlockRecv handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoBlockRecv(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOBLOCKRECV,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoBlockSelect handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoBlockSelect(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOBLOCKSELECT,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoBlockSend handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoBlockSend(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOBLOCKSEND,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoBlockSync handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoBlockSync(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOBLOCKSYNC,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoCreate handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoCreate(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        long g = LEB128Util.read(reader);
        fLastG = g;
        long stack = LEB128Util.read(reader);
        LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOCREATE,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, Arrays.asList(new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null),
                        new TmfEventField(G_FIELD, g, null), new TmfEventField("stack", stack, null)).toArray(new ITmfEventField[0]))); //$NON-NLS-1$
    }

    /**
     * EventGoEnd handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoEnd(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOEND, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoInSyscall handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoInSyscall(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long g = LEB128Util.read(reader);
        fLastG = g;
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOINSYSCALL,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, Arrays.asList(new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(G_FIELD, g, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventGomaxprocs handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGomaxprocs(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long procs = LEB128Util.read(reader);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOMAXPROCS,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, Arrays.asList(new TmfEventField(STACK_ID, stackID, null), new TmfEventField("procs", procs, null), new TmfEventField(G_FIELD, fLastG, null), //$NON-NLS-1$
                        new TmfEventField(P_FIELD, fLastP, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoPreempt handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoPreempt(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOPREEMPT,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoSched handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoSched(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOSCHED,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoSleep handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoSleep(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOSLEEP,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoStart handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoStart(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long g = LEB128Util.read(reader);
        fLastG = g;
        long seq = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOSTART,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(G_FIELD, g, null), new TmfEventField("seq", seq, null)).toArray(new ITmfEventField[0]))); //$NON-NLS-1$
    }

    /**
     * EventGoStartLabel handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoStartLabel(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long g = LEB128Util.read(reader);
        fLastG = g;
        long seq = LEB128Util.read(reader);
        long labelid = LEB128Util.read(reader);
        String label = readUTF(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOSTARTLABEL, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                Arrays.asList(new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(G_FIELD, g, null), new TmfEventField("seq", seq, null), new TmfEventField("labelid", labelid, null), //$NON-NLS-1$ //$NON-NLS-2$
                        new TmfEventField("label", label, null)).toArray(new ITmfEventField[0]))); //$NON-NLS-1$
    }

    /**
     * EventGoStartLocal handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoStartLocal(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long g = LEB128Util.read(reader);
        fLastG = g;
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOSTARTLOCAL,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, Arrays.asList(new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(G_FIELD, g, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoStop handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoStop(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOSTOP,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoSysBlock handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoSysBlock(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOSYSBLOCK, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoSysCall handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoSysCall(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOSYSCALL,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoSysExit handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoSysExit(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long g = LEB128Util.read(reader);
        fLastG = g;
        long seq = LEB128Util.read(reader);
        long ts = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOSYSEXIT,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(G_FIELD, g, null), new TmfEventField("seq", seq, null), new TmfEventField("ts", ts, null)) //$NON-NLS-1$ //$NON-NLS-2$
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoSysExitLocal handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoSysExitLocal(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long g = LEB128Util.read(reader);
        fLastG = g;
        long ts = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOSYSEXITLOCAL,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(G_FIELD, g, null), new TmfEventField("ts", ts, null)).toArray(new ITmfEventField[0]))); //$NON-NLS-1$
    }

    /**
     * EventGoUnblock handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoUnblock(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        long g = LEB128Util.read(reader);
        fLastG = g;
        long seq = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOUNBLOCK,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null), new TmfEventField(G_FIELD, g, null), new TmfEventField("seq", seq, null)) //$NON-NLS-1$
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoUnblockLocal handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoUnblockLocal(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        long g = LEB128Util.read(reader);
        fLastG = g;
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOUNBLOCKLOCAL,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(STACK_ID, stackID, null), new TmfEventField(G_FIELD, g, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventGoWaiting handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoGoWaiting(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long g = LEB128Util.read(reader);
        fLastG = g;
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOGOWAITING,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, Arrays.asList(new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(G_FIELD, g, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventHeapAlloc handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoHeapAlloc(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long mem = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOHEAPALLOC,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField("mem", mem, null)) //$NON-NLS-1$
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventHeapGoal handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoHeapGoal(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long mem = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOHEAPGOAL,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null), new TmfEventField("mem", mem, null)) //$NON-NLS-1$
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventNone handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoNone(long rank, DataInput reader) throws IOException {
        // the type
        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GONONE, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, null));
    }

    /**
     * EventProcStart handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoProcStart(long rank, DataInput reader) throws IOException {

        // the type
        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long thread = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOPROCSTART,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField("thread", thread, null), new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null)) //$NON-NLS-1$
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventProcStop handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoProcStop(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOPROCSTOP, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                Arrays.asList(new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventStack handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoStack(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long id = LEB128Util.read(reader);
        long siz = LEB128Util.read(reader);
        List<@NonNull ITmfCallsite> callsites = new ArrayList<>();
        for (int i = 0; i < siz; i++) {
            long ip = LEB128Util.read(reader);
            String function = fStringMap.get(LEB128Util.read(reader));
            String file = fStringMap.get(LEB128Util.read(reader));
            long line = LEB128Util.read(reader);
            callsites.add(new FullCallsite(ip, function, file, line));
        }
        fStacks.put(id, callsites);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOSTACK,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, Arrays.asList(new TmfEventField("id", id, null), new TmfEventField(G_FIELD, fLastG, null), //$NON-NLS-1$
                        new TmfEventField(P_FIELD, fLastP, null), new TmfEventField("siz", siz, null), new TmfEventField("callsite", callsites, null)).toArray(new ITmfEventField[0]))); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * EventString handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoString(long rank, DataInput reader) throws IOException {

        long id = LEB128Util.read(reader);
        if (id == 0) {
            throw new IOException("string has invalid id of 0"); //$NON-NLS-1$
        }
        if (fStringMap.containsKey(id)) {
            throw new IOException("string has duplicate id of " + id); //$NON-NLS-1$
        }
        String label = readUTF(reader);
        fStringMap.put(id, label);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOSTRING, new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                Arrays.asList(new TmfEventField("label", label, null), new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null)).toArray(new ITmfEventField[0]))); //$NON-NLS-1$
    }

    /**
     * EventTimerGoroutine handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoTimerGoroutine(long rank, DataInput reader) throws IOException {

        long g = LEB128Util.read(reader);
        fLastG = g;
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOTIMERGOROUTINE,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, Arrays.asList(new TmfEventField(P_FIELD, fLastP, null), new TmfEventField(G_FIELD, g, null)).toArray(new ITmfEventField[0])));
    }

    /**
     * EventUserLog handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoUserLog(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        long id = LEB128Util.read(reader);
        long keyid = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOUSERLOG,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(STACK_ID, stackID, null), new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null),
                                new TmfEventField("id", id, null), new TmfEventField("keyid", keyid, null)).toArray(new ITmfEventField[0]))); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * EventUserRegion handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoUserRegion(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        long taskid = LEB128Util.read(reader);
        long mode = LEB128Util.read(reader);
        long typeid = LEB128Util.read(reader);
        String name = fStringMap.get(typeid);
        LEB128Util.read(reader);

        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOUSERREGION,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(STACK_ID, stackID, null), new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null),
                                new TmfEventField("taskid", taskid, null), new TmfEventField("mode", mode, null), new TmfEventField("typeid", typeid, null), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                new TmfEventField(NAME, name, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventUserTaskCreate handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoUserTaskCreate(long rank, DataInput reader) throws IOException {

        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        long taskid = LEB128Util.read(reader);
        long pid = LEB128Util.read(reader);
        long typeid = LEB128Util.read(reader);
        LEB128Util.read(reader);
        String name = fStringMap.get(typeid);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOUSERTASKCREATE,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null,
                        Arrays.asList(new TmfEventField(STACK_ID, stackID, null), new TmfEventField(G_FIELD, fLastG, null), new TmfEventField(P_FIELD, fLastP, null),
                                new TmfEventField("taskid", taskid, null), new TmfEventField("pid", pid, null), new TmfEventField("typeid", typeid, null), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                new TmfEventField(NAME, name, null))
                                .toArray(new ITmfEventField[0])));
    }

    /**
     * EventUserTaskEnd handler
     *
     * @param rank
     *            the rank of the event
     * @param reader
     *            the file to read
     * @return the event
     * @throws IOException
     *             the event could not be read
     */
    private ITmfEvent generateGoUserTaskEnd(long rank, DataInput reader) throws IOException {
        long timeDiff = LEB128Util.read(reader);
        updateTime(timeDiff);
        long stackID = LEB128Util.read(reader);
        long taskid = LEB128Util.read(reader);
        return new TmfEvent(this, rank, createTimestamp(fCurrentTime), GOUSERTASKEND,
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, Arrays.asList(new TmfEventField(STACK_ID, stackID, null), new TmfEventField(G_FIELD, fLastG, null),
                        new TmfEventField(P_FIELD, fLastP, null), new TmfEventField("taskid", taskid, null)).toArray(new ITmfEventField[0]))); //$NON-NLS-1$
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        return new TmfLongLocation(fPosition);
    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        if (!(location instanceof TmfLongLocation)) {
            return -1;
        }

        TmfLongLocation loc = (TmfLongLocation) location;
        return ((double) loc.getLocationInfo() / fAwfulBackup.size());
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> eventType, String name, String traceTypeId) throws TmfTraceException {
        super.initTrace(resource, path, eventType, name, traceTypeId);
        fFile = new File(path);
        try (
                BufferedRandomAccessFile bar = new BufferedRandomAccessFile(fFile, "r");) { //$NON-NLS-1$
            char current;
            StringBuilder sb = new StringBuilder();
            do {
                current = (char) bar.readByte();
                sb.append(current);
                fPosition++;
            } while (current != 0 && sb.length() < 20);
            fPosition = 16;
            bar.seek(fPosition);
            fPosition = bar.getFilePointer();
            populateEvents(bar);
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    @Override
    public ITmfEvent parseEvent(ITmfContext ctx) {
        ITmfContext context = ctx;
        if (context == null) {
            context = seekEvent(0);
        }
        long rank = context.getRank();
        if (rank >= 0 && rank < fAwfulBackup.size()) {
            return fAwfulBackup.get((int) rank);
        }
        Activator.getInstance().logError("Rank out of bounds : " + rank + " at " + Objects.requireNonNull(fFile).getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
        return null;
    }

    /**
     * @param bar
     * @param event
     */
    private void populateEvents(BufferedRandomAccessFile bar) {
        try {
            long rank = 0;
            ITmfEvent event = null;
            do {
                int read = bar.read();
                int type = ((read << 2) & 0xff) >> 2;
                EventHandler handler = fHandlers[type];
                if (handler != null) {
                    event = handler.apply(rank, bar);
                    rank++;
                    if (event != null) {
                        fAwfulBackup.add(event);
                    }
                } else {
                    event = null;
                    Activator.getInstance().logError("EventID out of bounds : " + type + " at " + Objects.requireNonNull(fFile).getAbsolutePath() + ":" + bar.getFilePointer()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
            } while (event != null && bar.length() > bar.getFilePointer());
        } catch (IOException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        return new TmfContext(new TmfLongLocation((long) (ratio * fAwfulBackup.size())));
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {
        if (location == null) {
            return new TmfContext(new TmfLongLocation(0));
        }
        if (!(location instanceof TmfLongLocation)) {
            return null;
        }
        TmfLongLocation loc = (TmfLongLocation) location;
        return new TmfContext(loc);
    }

    private void setTime(long time) {
        fCurrentTime = time;
    }

    private void updateTime(long timeDiff) {
        fCurrentTime += timeDiff;
    }

    @Override
    public IStatus validate(IProject project, String path) {
        File file = new File(path);
        if (!file.exists()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "File not found: " + path); //$NON-NLS-1$
        }
        if (!file.isFile()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a file. It's a directory: " + path); //$NON-NLS-1$
        }
        int confidence = 32;
        // Look up the magic number
        try {
            if (!TmfTraceUtils.isText(file)) {
                try (RandomAccessFile raf = new RandomAccessFile(file, "r")) { //$NON-NLS-1$
                    char current;
                    StringBuilder sb = new StringBuilder();
                    do {
                        current = (char) raf.readByte();
                        sb.append(current);
                    } while (current != 0 && sb.length() < 20);
                    String data = sb.toString();
                    if (data.length() == 0) {
                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "File too small: " + path); //$NON-NLS-1$
                    }
                    Matcher matcher = HEADER_REGEX.matcher(data);
                    if (!matcher.find()) {
                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Invalid Magic Number " + path); //$NON-NLS-1$
                    }
                }
                return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
            }
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.toString());
        }
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "text trace"); //$NON-NLS-1$
    }

}
