/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.fileacess;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.kernel.core.Activator;
import org.eclipse.tracecompass.incubator.internal.kernel.core.filedescriptor.FileDescriptorStateProvider;
import org.eclipse.tracecompass.incubator.internal.kernel.core.filedescriptor.HandlerParameter;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableSet;

/**
 * <p>
 * File Access State Provider, handles file descriptors of files on a file
 * system.
 * </p>
 * Handles
 * <ul>
 * <li>open</li>
 * <li>openat</li>
 * <li>dup(soon)</li>
 * <li>dirstat(soon)</li></ul
 *
 * @author Matthew Khouzam
 */
public class FileAccessStateProvider extends FileDescriptorStateProvider {

    private static final String PID = "pid"; //$NON-NLS-1$

    private static final String STDERR = "stderr"; //$NON-NLS-1$

    private static final String STDOUT = "stdout"; //$NON-NLS-1$

    private static final String STDIN = "stdin"; //$NON-NLS-1$

    private static final String ID = "org.eclipse.tracecompass.incubator.internal.kernel.core.fileacess"; //$NON-NLS-1$


    private static final String OPENAT = "openat"; //$NON-NLS-1$
    private static final String OPEN = "open"; //$NON-NLS-1$
    private static final String LTTNG_STATEDUMP_FILE_DESCRIPTOR = "lttng_statedump_file_descriptor"; //$NON-NLS-1$
    private static final Set<String> NO_NO_LIST = ImmutableSet.of("socket:", "pipe:"); //$NON-NLS-1$ //$NON-NLS-2$

    private final String fFileName;
    private final Map<Integer, String> fOpenContexts = new HashMap<>();

    /**
     * Constructor
     *
     * @param trace
     *            the trace to handle
     */
    public FileAccessStateProvider(IKernelTrace trace) {
        super(trace, ID);
        IKernelAnalysisEventLayout layout = getLayout();
        addEventHandler(layout.eventSyscallEntryPrefix() + OPENAT, this::handleOpen);
        addEventHandler(layout.eventSyscallExitPrefix() + OPENAT, this::handleOpenExit);
        addEventHandler(layout.eventSyscallEntryPrefix() + OPEN, this::handleOpen);
        addEventHandler(layout.eventSyscallExitPrefix() + OPEN, this::handleOpenExit);
        addEventHandler(LTTNG_STATEDUMP_FILE_DESCRIPTOR, this::handleStateDump);
        fFileName = layout.fieldFilename();
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        ITmfTrace trace = getTrace();
        if (trace instanceof IKernelTrace) {
            return new FileAccessStateProvider((IKernelTrace) trace);
        }
        throw new IllegalStateException("Trace " + trace + " is not a kernel trace"); //$NON-NLS-1$//$NON-NLS-2$
    }

    private void handleOpenExit(HandlerParameter hp) {
        Integer tid = hp.getTid();
        ITmfStateSystemBuilder ssb = hp.getSsb();
        long time = hp.getTime();
        ITmfEvent event = hp.getEvent();
        String fileName = fOpenContexts.remove(tid);
        Long ret = event.getContent().getFieldValue(Long.class, getLayout().fieldSyscallRet());
        if (ret == null || fileName == null || ret < 0) {
            return;
        }
        handleCommonOpen(ssb, time, tid, ret, fileName);
    }

    private static void handleCommonOpen(ITmfStateSystemBuilder ssb, long time, Integer tid, Long fd, String filename) {
        String fn = filename;
        if (fd == 0) {
            fn = STDIN;
        } else if (fd == 1) {
            fn = STDOUT;
        } else if (fd == 2) {
            fn = STDERR;
        }
        if (fn.isEmpty()) {
            return;
        }
        try {
            int tidQuark = ssb.getQuarkAbsoluteAndAdd(TID, String.valueOf(tid));
            StateSystemBuilderUtils.incrementAttributeInt(ssb, time, tidQuark, 1);
            int tidFileQuark = ssb.getQuarkAbsoluteAndAdd(TID, String.valueOf(tid), String.valueOf(fd));
            ssb.modifyAttribute(time, fn, tidFileQuark);
            int fileQuark = ssb.getQuarkAbsoluteAndAdd(RESOURCES, fn);
            int fileTidQuark = ssb.getQuarkAbsoluteAndAdd(RESOURCES, fn, String.valueOf(tid));
            StateSystemBuilderUtils.incrementAttributeInt(ssb, time, fileQuark, 1);
            ssb.modifyAttribute(time, 1, fileTidQuark);
        } catch (StateValueTypeException | AttributeNotFoundException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
    }

    private void handleOpen(HandlerParameter param) {
        ITmfEvent event = param.getEvent();
        Integer tid = param.getTid();
        String fileName = event.getContent().getFieldValue(String.class, fFileName);
        if (fileName == null) {
            return;
        }
        fOpenContexts.put(tid, fileName);
    }

    private void handleStateDump(HandlerParameter params) {
        ITmfEvent event = params.getEvent();
        ITmfEventField content = event.getContent();
        Long pid = content.getFieldValue(Long.class, PID);
        Long fd = content.getFieldValue(Long.class, DESCRIPTOR);
        String filename = content.getFieldValue(String.class, fFileName);
        if (pid == null || fd == null || filename == null || NO_NO_LIST.stream().anyMatch(filename::startsWith)) {
            return;
        }
        int tid = pid.intValue();
        fOpenContexts.put(tid, filename);
        handleCommonOpen(params.getSsb(), event.getTimestamp().toNanos(), tid, fd, filename);
    }

}
