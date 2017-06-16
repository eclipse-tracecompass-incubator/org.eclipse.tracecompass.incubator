/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.husdon.maven.core.analysis;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisEventBasedModule;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * Maven goals analysis. Computes starts and ends of various stages of the
 * build.
 *
 * @author Marc-Andre Laperle
 */
public class MavenGoalsLatencyAnalysis extends AbstractSegmentStoreAnalysisEventBasedModule {

    /**
     * The ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.hudson.maven.goals.latency"; //$NON-NLS-1$

    private static final String DATA_FILENAME = "maven-goals-latency-analysis.dat"; //$NON-NLS-1$

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public @NonNull String getDataFileName() {
        return DATA_FILENAME;
    }

    @Override
    public AbstractSegmentStoreAnalysisRequest createAnalysisRequest(ISegmentStore<ISegment> syscalls) {
        return new MavenGoalsLatencyAnalysisRequest(syscalls);
    }

    @Deprecated
    @Override
    protected Object[] readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        return checkNotNull((Object[]) ois.readObject());
    }

    private class MavenGoalsLatencyAnalysisRequest extends AbstractSegmentStoreAnalysisRequest {

        private @Nullable String fLastGoal = null;
        private long fLastTime = 0;
        private long fDependenciesStart = 0;

        private long fArchiveStart = 0;

        private final Map<Integer, MavenGoal.InitialInfo> fOngoingSystemCalls = new HashMap<>();
        private final IProgressMonitor fMonitor = new NullProgressMonitor();
        private long fLastRank = 0;

        public MavenGoalsLatencyAnalysisRequest(ISegmentStore<ISegment> syscalls) {
            super(syscalls);
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            long curRank = getIndex() + getNbRead() - 1;

            ITmfEventField messageField = event.getContent().getField("Message"); //$NON-NLS-1$
            if (messageField != null) {
                String value = (String) messageField.getValue();
                if (fDependenciesStart == 0 && value.startsWith("Computing target platform for MavenProject")) { //$NON-NLS-1$
                    fDependenciesStart = event.getTimestamp().getValue();
                    fLastRank = curRank;
                } else if (fDependenciesStart != 0 && value.startsWith("----")) { //$NON-NLS-1$
                    MavenGoal.InitialInfo info = new MavenGoal.InitialInfo(fDependenciesStart, "Resolving dependencies", fLastRank); //$NON-NLS-1$
                    ISegment syscall = new MavenGoal(info, event.getTimestamp().getValue(), curRank - 1);
                    getSegmentStore().add(syscall);
                    fDependenciesStart = 0;
                }

                if (fArchiveStart == 0 && value.startsWith("Archiving artifacts")) { //$NON-NLS-1$
                    fArchiveStart = event.getTimestamp().getValue();
                    fLastRank = curRank;
                } else if (fArchiveStart != 0) {
                    MavenGoal.InitialInfo info = new MavenGoal.InitialInfo(fArchiveStart, "Archiving artifacts", fLastRank); //$NON-NLS-1$
                    ISegment syscall = new MavenGoal(info, event.getTimestamp().getValue(), curRank - 1);
                    getSegmentStore().add(syscall);
                    fArchiveStart = 0;
                }

            }

            ITmfEventField goalField = event.getContent().getField("Goal"); //$NON-NLS-1$
            if (goalField != null) {
                String value = (String) goalField.getValue();
                if (!value.isEmpty()) {
                    long endTime = event.getTimestamp().getValue();
                    if (fLastGoal != null) {
                        MavenGoal.InitialInfo info = new MavenGoal.InitialInfo(fLastTime, fLastGoal, fLastRank);
                        ISegment syscall = new MavenGoal(info, endTime, curRank - 1);
                        getSegmentStore().add(syscall);
                    }
                    fLastGoal = value;
                    fLastTime = endTime;
                    fLastRank = curRank;
                }

            }
        }

        @Override
        public void handleCompleted() {
            fOngoingSystemCalls.clear();
            super.handleCompleted();
        }

        @Override
        public void handleCancel() {
            fMonitor.setCanceled(true);
            super.handleCancel();
        }
    }

}
