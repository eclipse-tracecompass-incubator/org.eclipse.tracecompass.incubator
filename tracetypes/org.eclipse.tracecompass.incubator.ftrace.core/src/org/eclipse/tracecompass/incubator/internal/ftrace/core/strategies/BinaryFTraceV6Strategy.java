/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.strategies;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.context.BinaryFTraceContext;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.context.BinaryFTraceLocation;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.context.BinaryFTraceLocationInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceVersion;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceVersionHeader;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator.BinaryFTraceIterator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser.BinaryFTraceFileParser;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.GenericFtraceEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.trace.BinaryFTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;

/**
 * An implementation of {@link IBinaryFTraceStrategy} for FTrace v6.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceV6Strategy implements IBinaryFTraceStrategy {
    private static final byte[] MAGIC_VALUES = { 0x17, 0x08, 0x44, 't', 'r', 'a', 'c', 'i', 'n', 'g' };
    private BinaryFTraceHeaderInfo fTraceHeaderData;
    @NonNull
    private final BinaryFTrace fFTrace;

    /**
     * Constructor
     *
     * @param trace
     *            The {@link BinaryFTrace} that represents the trace to parse
     */
    public BinaryFTraceV6Strategy(@NonNull BinaryFTrace trace) {
        fFTrace = trace;
    }

    /**
     * Validate whether a trace is of the Binary FTrace v6 format
     *
     * @param versionHeader
     *            A {@link BinaryFTraceVersionHeader} object that contains the
     *            magic values and FTrace version
     * @return True if the trace is a Binary FTrace trace and the version is
     *         supported
     */
    public static boolean validate(BinaryFTraceVersionHeader versionHeader) {
        boolean isValidMagicValue = Arrays.equals(MAGIC_VALUES, versionHeader.getMagicValues());
        boolean isSupportedVersion = versionHeader.getFTraceVersion().equals(BinaryFTraceVersion.V6);
        return isValidMagicValue && isSupportedVersion;
    }

    @Override
    public void initTrace(String path) throws TmfTraceException {
        // Parse the file header
        fTraceHeaderData = BinaryFTraceFileParser.parse(path);

        // Set the start and (current) end times for this trace
        BinaryFTraceContext ctx = (BinaryFTraceContext) fFTrace.seekEvent(0L);

        if ((ctx.getLocation().equals(BinaryFTraceIterator.NULL_LOCATION)) || (ctx.getCurrentEvent() == null)) {
            // Handle the case where the trace is empty
            fFTrace.setStartTime(TmfTimestamp.BIG_BANG);
        } else {
            ITmfEvent event = fFTrace.getNext(ctx);
            ITmfTimestamp curTime = event.getTimestamp();
            fFTrace.setStartTime(curTime);
            fFTrace.setEndTime(curTime);
        }
    }

    @Override
    public ITmfEvent getNext(ITmfContext context) {
        if (fTraceHeaderData == null) {
            return null;
        }
        GenericFtraceEvent event = null;
        if (context instanceof BinaryFTraceContext) {
            if (context.getLocation() == null || BinaryFTraceLocation.INVALID_LOCATION.equals(context.getLocation().getLocationInfo())) {
                return null;
            }

            BinaryFTraceContext ftraceCtx = (BinaryFTraceContext) context;
            event = ftraceCtx.getCurrentEvent();

            if (event != null) {
                fFTrace.updateAttributes(context, event);
                ftraceCtx.advance();
                ftraceCtx.increaseRank();
            }
        }

        return event;
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {
        BinaryFTraceLocation currentLocation = (BinaryFTraceLocation) location;
        BinaryFTraceContext context = new BinaryFTraceContext(fFTrace);

        if (fTraceHeaderData == null) {
            context.setLocation(null);
            context.setRank(ITmfContext.UNKNOWN_RANK);
            return context;
        }
        /*
         * The rank is set to 0 if the iterator seeks the beginning. If not, it
         * will be set to UNKNOWN_RANK, since BinaryFTrace traces don't support
         * seeking by rank for now.
         */
        if (currentLocation == null) {
            currentLocation = new BinaryFTraceLocation(new BinaryFTraceLocationInfo(0L, 0L));
            context.setRank(0);
        } else {
            context.setRank(ITmfContext.UNKNOWN_RANK);
        }
        /* This will seek and update the location after the seek */
        context.setLocation(currentLocation);
        return context;
    }

    @Override
    public ITmfContext createIterator() throws IOException {
        return new BinaryFTraceIterator(fTraceHeaderData, fFTrace);
    }

    @Override
    public void dispose() {
        // release (indirect) references to mem-mapped file buffers so that tracecompass
        // can garbage-collect them and unlock the file (e.g. if the user wants to delete it)
        fTraceHeaderData = null;
    }
}
