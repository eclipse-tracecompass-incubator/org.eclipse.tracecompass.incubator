/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.filedescriptor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.kernel.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * <p>
 * File Descriptor State Provider
 * </p>
 * <p>
 * This allows handling of generic file descriptors with common operations
 * </p>
 * <p>
 * Common elements
 * </p>
 * <ul>
 * <li>read</li>
 * <li>write</li>
 * <li>close</li>
 * </ul>
 *
 * @author Matthew Khouzam
 */
public abstract class FileDescriptorStateProvider extends AbstractTmfStateProvider {

    /**
     * TID field in state system
     */
    public static final String TID = "TID"; //$NON-NLS-1$
    /**
     * Resources field in state system
     */
    public static final String RESOURCES = "RES"; //$NON-NLS-1$

    /**
     * File descriptor name
     */
    protected static final String DESCRIPTOR = "fd"; //$NON-NLS-1$

    /**
     * Read entry
     */
    public static final String READ = "read"; //$NON-NLS-1$

    /**
     * Write entry
     */
    public static final String WRITE = "write"; //$NON-NLS-1$
    private static final String COUNT = "count"; //$NON-NLS-1$
    private static final String PREAD = "pread"; //$NON-NLS-1$
    private static final String READ64 = "read64"; //$NON-NLS-1$
    private static final String PREAD64 = "pread64"; //$NON-NLS-1$
    private static final String PWRITE = "pwrite"; //$NON-NLS-1$
    private static final String WRITE64 = "write64"; //$NON-NLS-1$
    private static final String PWRITE64 = "pwrite64"; //$NON-NLS-1$
    private static final String CLOSE = "close"; //$NON-NLS-1$

    private final Map<String, Consumer<HandlerParameter>> fHandlers = new HashMap<>();
    private final IKernelAnalysisEventLayout fLayout;

    /*
     * Nullable to make the jdt be quiet
     */
    private final Map<Integer, @Nullable Long> fToRead = new HashMap<>();
    private final Map<Integer, @Nullable Long> fToWrite = new HashMap<>();

    private final Map<Integer, Long> fToClose = new HashMap<>();

    /**
     * Constructor
     *
     * @param trace
     *            The trace
     * @param id
     *            Name given to this state change input. Only used internally.
     */
    public FileDescriptorStateProvider(IKernelTrace trace, String id) {
        super(trace, id);
        fLayout = trace.getKernelEventLayout();

        addEventHandler(getLayout().eventSyscallEntryPrefix() + CLOSE, this::closeBegin);
        addEventHandler(getLayout().eventSyscallExitPrefix() + CLOSE, this::closeEnd);
        addEventHandler(getLayout().eventSyscallEntryPrefix() + READ, this::readBegin);
        addEventHandler(getLayout().eventSyscallEntryPrefix() + PREAD, this::readBegin);
        addEventHandler(getLayout().eventSyscallEntryPrefix() + READ64, this::readBegin);
        addEventHandler(getLayout().eventSyscallEntryPrefix() + PREAD64, this::readBegin);
        addEventHandler(getLayout().eventSyscallExitPrefix() + READ, this::readEnd);
        addEventHandler(getLayout().eventSyscallExitPrefix() + PREAD, this::readEnd);
        addEventHandler(getLayout().eventSyscallExitPrefix() + READ64, this::readEnd);
        addEventHandler(getLayout().eventSyscallExitPrefix() + PREAD64, this::readEnd);
        addEventHandler(getLayout().eventSyscallEntryPrefix() + WRITE, this::writeBegin);
        addEventHandler(getLayout().eventSyscallEntryPrefix() + PWRITE, this::writeBegin);
        addEventHandler(getLayout().eventSyscallEntryPrefix() + WRITE64, this::writeBegin);
        addEventHandler(getLayout().eventSyscallEntryPrefix() + PWRITE64, this::writeBegin);
        addEventHandler(getLayout().eventSyscallExitPrefix() + WRITE, this::writeEnd);
        addEventHandler(getLayout().eventSyscallExitPrefix() + PWRITE, this::writeEnd);
        addEventHandler(getLayout().eventSyscallExitPrefix() + WRITE64, this::writeEnd);
        addEventHandler(getLayout().eventSyscallExitPrefix() + PWRITE64, this::writeEnd);
    }

    /**
     * Add a handler for entries
     *
     * @param eventName
     *            the name to handle
     * @param handler
     *            handler
     */
    protected final void addEventHandler(String eventName, Consumer<HandlerParameter> handler) {
        fHandlers.put(eventName, handler);
    }

    @Override
    protected final void eventHandle(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ssb = getStateSystemBuilder();
        if (ssb == null) {
            return;
        }
        Integer tid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxTidAspect.class, event);
        if (tid == null) {
            return;
        }
        String name = event.getName();
        if (name.contains("read")) { //$NON-NLS-1$
            new Object();
        }
        Consumer<HandlerParameter> eventHandler = fHandlers.get(name);
        if (eventHandler != null) {
            eventHandler.accept(new HandlerParameter(ssb, event, tid));
        }
    }

    /**
     * Get the event layout of this trace. Many known concepts from the Linux
     * kernel may be exported under different names, depending on the tracer.
     *
     * @return The event layout
     */
    protected final IKernelAnalysisEventLayout getLayout() {
        return fLayout;
    }

    /**
     * Check if a file descriptor is valid. Has it been opened? if not, let's
     * ignore it for now.
     *
     * @param ssb
     *            State system
     * @param tid
     *            the TID that owns the file descriptor
     * @param fd
     *            the file descriptor
     * @return the file descriptor or null if invalid
     */
    protected static final @Nullable Long isValidFileDescriptor(ITmfStateSystem ssb, Integer tid, @Nullable Long fd) {
        if (fd == null) {
            return null;
        }
        int tidFileQuark = ssb.optQuarkAbsolute(TID, String.valueOf(tid), String.valueOf(fd));
        if (tidFileQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return null;
        }
        return ssb.queryOngoing(tidFileQuark) != null ? fd : null;
    }

    private void readBegin(HandlerParameter params) {
        ITmfEvent event = params.getEvent();
        Integer tid = params.getTid();
        ITmfStateSystemBuilder ssb = params.getSsb();
        Long fd = (event.getContent().getFieldValue(Long.class, DESCRIPTOR));
        fd = isValidFileDescriptor(ssb, tid, fd);
        if (fd == null) {
            return;
        }
        Long read = event.getContent().getFieldValue(Long.class, COUNT);
        if (read == null) {
            return;
        }
        int tidQuark = ssb.optQuarkAbsolute(TID, String.valueOf(tid));
        if (tidQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return;
        }
        fToRead.put(tid, fd);
    }

    private void readEnd(HandlerParameter params) {
        ITmfEvent event = params.getEvent();
        Integer tid = params.getTid();
        ITmfStateSystemBuilder ssb = params.getSsb();
        long time = params.getTime();
        Long count = (event.getContent().getFieldValue(Long.class, getLayout().fieldSyscallRet()));
        Long fd = fToRead.remove(tid);
        fd = isValidFileDescriptor(ssb, tid, fd);
        if (fd != null && count != null) {
            try {
                int fileNameQuark = ssb.getQuarkAbsoluteAndAdd(TID, String.valueOf(tid), String.valueOf(fd));
                int readTid = ssb.getQuarkAbsoluteAndAdd(TID, String.valueOf(tid), String.valueOf(fd), READ);
                StateSystemBuilderUtils.incrementAttributeLong(ssb, time, readTid, count);
                Object fileNameObj = ssb.queryOngoing(fileNameQuark);
                if (fileNameObj instanceof String) {
                    int readFile = ssb.getQuarkAbsoluteAndAdd(RESOURCES, String.valueOf(fileNameObj), String.valueOf(tid), READ);
                    StateSystemBuilderUtils.incrementAttributeLong(ssb, time, readFile, count);
                }
            } catch (StateValueTypeException | AttributeNotFoundException e) {
                Activator.getInstance().logError(e.getMessage(), e);
            }
        }
    }

    private void writeBegin(HandlerParameter params) {
        ITmfEvent event = params.getEvent();
        Integer tid = params.getTid();
        ITmfStateSystemBuilder ssb = params.getSsb();
        Long fd = (event.getContent().getFieldValue(Long.class, DESCRIPTOR));
        fd = isValidFileDescriptor(ssb, tid, fd);
        if (fd == null) {
            return;
        }
        fToWrite.put(tid, fd);
    }

    private void writeEnd(HandlerParameter params) {
        ITmfEvent event = params.getEvent();
        Integer tid = params.getTid();
        ITmfStateSystemBuilder ssb = params.getSsb();
        long time = params.getTime();
        Long count = (event.getContent().getFieldValue(Long.class, getLayout().fieldSyscallRet()));
        Long fd = fToWrite.remove(tid);
        fd = isValidFileDescriptor(ssb, tid, fd);
        if (fd != null && count != null) {
            try {
                int fileNameQuark = ssb.getQuarkAbsoluteAndAdd(TID, String.valueOf(tid), String.valueOf(fd));
                int writeTid = ssb.getQuarkAbsoluteAndAdd(TID, String.valueOf(tid), String.valueOf(fd), WRITE);
                StateSystemBuilderUtils.incrementAttributeLong(ssb, time, writeTid, count);
                Object fileNameObj = ssb.queryOngoing(fileNameQuark);
                if (fileNameObj instanceof String) {
                    int writeFile = ssb.getQuarkAbsoluteAndAdd(RESOURCES, String.valueOf(fileNameObj), String.valueOf(tid), WRITE);
                    StateSystemBuilderUtils.incrementAttributeLong(ssb, time, writeFile, count);
                }
            } catch (StateValueTypeException | AttributeNotFoundException e) {
                Activator.getInstance().logError(e.getMessage(), e);
            }
        }
    }

    private void closeBegin(HandlerParameter params) {
        ITmfEvent event = params.getEvent();
        ITmfStateSystemBuilder ssb = params.getSsb();
        Integer tid = params.getTid();
        Long fd = (event.getContent().getFieldValue(Long.class, DESCRIPTOR));
        fd = isValidFileDescriptor(ssb, tid, fd);
        if (fd == null) {
            return;
        }
        fToClose.put(tid, fd);
    }

    private void closeEnd(HandlerParameter params) {
        ITmfEvent event = params.getEvent();
        Integer tid = params.getTid();
        ITmfStateSystemBuilder ssb = params.getSsb();
        long time = params.getTime();
        try {
            Long ret = (event.getContent().getFieldValue(Long.class, getLayout().fieldSyscallRet()));
            Long fd = fToClose.remove(tid);
            if (ret == null || fd == null || ret < 0) {
                return;
            }
            int tidQuark = ssb.getQuarkAbsoluteAndAdd(TID, String.valueOf(tid));
            Object current = ssb.queryOngoing(tidQuark);
            if (!(current instanceof Integer)) {
                return;
            }
            if (Integer.valueOf(1).equals(current)) {
                ssb.modifyAttribute(time, (Object) null, tidQuark);
            } else {
                StateSystemBuilderUtils.incrementAttributeInt(ssb, time, tidQuark, -1);
            }
            int tidFileQuark = ssb.getQuarkAbsoluteAndAdd(TID, String.valueOf(tid), String.valueOf(fd));
            ssb.modifyAttribute(time, (Object) null, tidFileQuark);
            if (ssb.optQuarkAbsolute(RESOURCES) != ITmfStateSystem.INVALID_ATTRIBUTE) {
                String fileName = String.valueOf(ssb.queryOngoing(tidFileQuark));
                int fileQuark = ssb.getQuarkAbsoluteAndAdd(RESOURCES, fileName);
                int fileTidQuark = ssb.getQuarkAbsoluteAndAdd(RESOURCES, fileName, String.valueOf(tid));
                current = ssb.queryOngoing(fileQuark);
                if (current instanceof Integer) {
                    if (Integer.valueOf(1).equals(current)) {
                        ssb.modifyAttribute(time, (Object) null, fileQuark);
                    } else {
                        StateSystemBuilderUtils.incrementAttributeInt(ssb, time, fileQuark, -1);
                    }
                }
                ssb.modifyAttribute(time, (Object) null, fileTidQuark);
            }
        } catch (StateValueTypeException | AttributeNotFoundException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
    }
}
