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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.context;

import java.io.IOException;

import org.eclipse.tracecompass.incubator.internal.ftrace.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator.BinaryFTraceIterator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.GenericFtraceEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.trace.BinaryFTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;

/**
 * A binary FTrace file context to support parsing events/restoring file
 * location.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceContext implements ITmfContext {
    private BinaryFTraceLocation fCurLocation;
    private long fCurRank;
    private final BinaryFTrace fTrace;
    private BinaryFTraceIterator fIterator;

    /**
     * Constructor
     *
     * @param trace
     *            The Binary Ftrace
     */
    public BinaryFTraceContext(BinaryFTrace trace) {
        this.fTrace = trace;
        fCurLocation = new BinaryFTraceLocation(new BinaryFTraceLocationInfo(0, 0));
    }

    @Override
    public long getRank() {
        return fCurRank;
    }

    @Override
    public synchronized ITmfLocation getLocation() {
        return fCurLocation;
    }

    @Override
    public synchronized boolean hasValidRank() {
        return fCurLocation.getLocationInfo().compareTo((BinaryFTraceLocationInfo) BinaryFTraceLocation.INVALID_LOCATION.getLocationInfo()) != 0;
    }

    @Override
    public synchronized void setLocation(ITmfLocation location) {
        try {
            if (location instanceof BinaryFTraceLocation) {
                BinaryFTraceLocation ctfLocation = (BinaryFTraceLocation) location;
                if (location.getLocationInfo().equals(BinaryFTraceLocation.INVALID_LOCATION.getLocationInfo())) {
                    fCurLocation = ctfLocation;
                } else {
                    @SuppressWarnings("resource")
                    BinaryFTraceIterator iterator = getIterator();
                    if (iterator == null) {
                        return;
                    }
                    iterator.seek(ctfLocation.getLocationInfo());
                    fCurLocation = iterator.getLocation();
                }
            } else {
                fCurLocation = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Activator.getInstance().logError("An error occured while setting the location for binary FTrace context."); //$NON-NLS-1$
        }
    }

    @Override
    public void setRank(long rank) {
        fCurRank = rank;

    }

    @Override
    public void increaseRank() {
        if (hasValidRank()) {
            fCurRank++;
        }
    }

    /**
     * Gets the trace of this context.
     *
     * @return The trace of this context
     */
    public BinaryFTrace getTrace() {
        return fTrace;
    }

    /**
     * Gets the current event. Wrapper to help BinaryFTraceIterator.
     *
     * @return The event or null
     */
    @SuppressWarnings("resource")
    public synchronized GenericFtraceEvent getCurrentEvent() {
        try {
            BinaryFTraceIterator iterator = getIterator();
            return iterator == null ? null : iterator.getCurrentEvent();
        } catch (Exception ex) {
            Activator.getInstance().logError(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Advances to the next event. Wrapper to help BinaryFTraceIterator.
     *
     * @return success or not
     */
    @SuppressWarnings("resource")
    public synchronized boolean advance() {
        try {
            BinaryFTraceLocationInfo curLocationData = fCurLocation.getLocationInfo();
            BinaryFTraceIterator iterator = getIterator();
            if (iterator == null) {
                return false;
            }
            boolean advanceSuccess = iterator.advance();
            GenericFtraceEvent currentEvent = iterator.getCurrentEvent();

            if (currentEvent != null) {
                final long timestampValue = iterator.getCurrentTimestamp();
                if (curLocationData.getTimestamp() == timestampValue) {
                    fCurLocation = new BinaryFTraceLocation(timestampValue, curLocationData.getIndex() + 1);
                } else {
                    fCurLocation = new BinaryFTraceLocation(timestampValue, 0L);
                }
            } else {
                fCurLocation = new BinaryFTraceLocation(BinaryFTraceLocation.INVALID_LOCATION);
            }

            return advanceSuccess;
        } catch (Exception ex) {
            Activator.getInstance().logError(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Seeks to a given timestamp. Wrapper to help {@link BinaryFTraceIterator}.
     *
     * @param timestamp
     *            desired timestamp
     * @return success or not
     */
    @SuppressWarnings("resource")
    public synchronized boolean seek(long timestamp) {
        try {
            BinaryFTraceIterator iterator = getIterator();
            if (iterator == null) {
                return false;
            }
            boolean seekSuccess = iterator.seek(timestamp);
            fCurLocation = iterator.getLocation();
            return seekSuccess;
        } catch (Exception ex) {
            Activator.getInstance().logError(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Seeks to a given location. Wrapper to help {@link BinaryFTraceIterator}.
     *
     * @param location
     *            unique location to find the event
     *
     * @return success or not
     */
    @SuppressWarnings("resource")
    public synchronized boolean seek(BinaryFTraceLocationInfo location) {
        try {
            fCurLocation = new BinaryFTraceLocation(location);
            BinaryFTraceIterator iterator = getIterator();
            return iterator == null ? false : iterator.seek(location);
        } catch (Exception ex) {
            Activator.getInstance().logError(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Get iterator, called every time to get an iterator, no local copy is
     * stored so that there is no need to "update"
     *
     * @return {@link BinaryFTraceIterator} instance
     * @throws IOException
     */
    private BinaryFTraceIterator getIterator() throws IOException {
        if (fIterator == null) {
            fIterator = (BinaryFTraceIterator) fTrace.createIterator();
            ITmfLocation seekLocation = getLocation();
            if (seekLocation != null) {
                BinaryFTraceLocationInfo location = (BinaryFTraceLocationInfo) seekLocation.getLocationInfo();
                fIterator.seek(location);
            }
        }

        return fIterator;
    }

    @Override
    public void dispose() {
        // Do nothing
    }
}
