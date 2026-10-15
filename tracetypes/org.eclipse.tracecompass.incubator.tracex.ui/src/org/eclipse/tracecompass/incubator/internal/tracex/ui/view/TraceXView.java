/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.ui.view;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXDataProviderFactory;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTimerDirection;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.incubator.internal.tracex.ui.Activator;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTimestampFormatUpdateSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceUpdatedSignal;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;

/**
 * TraceX execution time-graph view.
 */
@SuppressWarnings("restriction")
public class TraceXView extends BaseDataProviderTimeGraphView {

    /** View extension identifier. */
    public static final String ID = "org.eclipse.tracecompass.incubator.tracex.ui.view"; //$NON-NLS-1$

    private final Action fConfigureClock;

    /** Constructor. */
    public TraceXView() {
        super(ID, new BaseDataProviderTimeGraphPresentationProvider(),
                TraceXDataProviderFactory.ID);
        fConfigureClock = new Action("Clock...") { //$NON-NLS-1$
            @Override
            public void run() {
                configureClock();
            }
        };
    }

    @Override
    @NonNullByDefault({})
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        fConfigureClock.setToolTipText("Configure TraceX clock"); //$NON-NLS-1$
        getViewSite().getActionBars().getToolBarManager().add(fConfigureClock);
        refreshClockStatus(false);
    }

    /** Edit an individual trace and apply its validated clock in a background job. */
    private void configureClock() {
        if (!(getTrace() instanceof TraceXTrace)) {
            return;
        }
        TraceXTrace trace = (TraceXTrace) getTrace();
        TraceXClockDialog dialog = new TraceXClockDialog(getSite().getShell(), trace);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return;
        }
        TraceXTimerDirection direction = dialog.getDirection();
        String frequency = dialog.getFrequency();
        Job job = new Job("Apply TraceX clock") { //$NON-NLS-1$
            @Override
            @NonNullByDefault({})
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    trace.configureClock(direction, frequency);
                    refreshClockStatus(false);
                    return Status.OK_STATUS;
                } catch (TmfTraceException | IllegalArgumentException e) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            "Failed to apply TraceX clock", e); //$NON-NLS-1$
                }
            }
        };
        job.setUser(true);
        job.schedule();
    }

    /** Refresh the unit label and replace cached view models when the clock changes. */
    private void refreshClockStatus(boolean rebuildClock) {
        if (getSite() == null || getSite().getShell().isDisposed()) {
            return;
        }
        getSite().getShell().getDisplay().asyncExec(() -> {
            if (getTimeGraphViewer().getControl().isDisposed()) {
                return;
            }
            if (getTrace() instanceof TraceXTrace) {
                TraceXTrace trace = (TraceXTrace) getTrace();
                fConfigureClock.setEnabled(true);
                setContentDescription(trace.getTicksPerSecond().isPresent()
                        ? "Nanoseconds; " + trace.getTicksPerSecond().getAsLong() + " ticks per second; " //$NON-NLS-1$ //$NON-NLS-2$
                                + trace.getTimerDirection().getPropertyValue()
                        : "Ordering ticks; physical time unknown; " + trace.getTimerDirection().getPropertyValue()); //$NON-NLS-1$
                if (rebuildClock) {
                    rebuild();
                    // Event editors must re-read cached timestamps on the UI thread.
                    TmfSignalManager.dispatchSignal(new TmfTimestampFormatUpdateSignal(this));
                }
            } else {
                fConfigureClock.setEnabled(false);
                setContentDescription(""); //$NON-NLS-1$
            }
        });
    }

    @Override
    @TmfSignalHandler
    @NonNullByDefault({})
    public void traceOpened(TmfTraceOpenedSignal signal) {
        super.traceOpened(signal);
        refreshClockStatus(false);
    }

    @Override
    @TmfSignalHandler
    @NonNullByDefault({})
    public void traceSelected(TmfTraceSelectedSignal signal) {
        super.traceSelected(signal);
        refreshClockStatus(false);
    }

    @Override
    @TmfSignalHandler
    @NonNullByDefault({})
    public void traceClosed(TmfTraceClosedSignal signal) {
        super.traceClosed(signal);
        refreshClockStatus(false);
    }

    @Override
    @TmfSignalHandler
    @NonNullByDefault({})
    public void traceUpdated(TmfTraceUpdatedSignal signal) {
        super.traceUpdated(signal);
        if (signal.getTrace() == getTrace()) {
            // The trace emits its final update after invalidating time-dependent data.
            refreshClockStatus(signal.getSource() == signal.getTrace());
        }
    }
}
