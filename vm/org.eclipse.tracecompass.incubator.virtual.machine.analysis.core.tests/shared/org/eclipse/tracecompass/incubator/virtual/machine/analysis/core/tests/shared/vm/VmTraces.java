/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.Activator;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.trace.VmXmlKernelTraceStub;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * List the available virtual machine host and guest traces
 *
 * @author Geneviève Bastien
 */
public enum VmTraces {

    /** Host from simple QEMU/KVM experiment */
    HOST_ONE_QEMUKVM("vm/OneQemuKvm/host.xml"),
    /** Guest from simple QEMU/KVM experiment */
    GUEST_ONE_QEMUKVM("vm/OneQemuKvm/guest.xml"),
    /** Guest from simple QEMU/KVM experiment */
    ONE_CONTAINER("vm/Containers/withContainers.xml"),
    /** Host from simple QEMU/KVM experiment */
    HOST_QEMUKVM_CONTAINER("vm/QemuContainer/host.xml", "host"),
    /** Guest from simple QEMU/KVM experiment */
    GUEST_QEMUKVM_CONTAINER("vm/QemuContainer/guest.xml", "guest"),;

    private static final @NonNull String filePath = "testfiles";

    private final @NonNull IPath fPath;
    private final @NonNull String fHostId;

    VmTraces(String path) {
        this(path, null);
    }

    VmTraces(String path, @Nullable String hostId) {
        IPath relativePath = new Path(filePath + File.separator + path);
        Activator plugin = Activator.getDefault();
        URL location = FileLocator.find(plugin.getBundle(), relativePath, null);
        try {
            fPath = new Path(FileLocator.toFileURL(location).getPath());
        } catch (IOException e) {
            throw new IllegalStateException();
        }
        fHostId = hostId != null ? hostId : getFileName();
    }

    /**
     * Return a TmfXmlTraceStub object of this test trace. It will be already
     * initTrace()'ed.
     *
     * Make sure you call {@link #exists()} before calling this!
     *
     * @return A TmfXmlTraceStub reference to this trace
     */
    public @Nullable ITmfTrace getTrace() {
        VmXmlKernelTraceStub trace = new VmXmlKernelTraceStub() {

            @Override
            public @NonNull String getHostId() {
                return fHostId;
            }

        };
        IStatus status = trace.validate(null, fPath.toOSString());
        if (!status.isOK()) {
            return null;
        }
        try {
            trace.initTrace(null, fPath.toOSString(), TmfEvent.class);
        } catch (TmfTraceException e) {
            return null;
        }
        return trace;
    }

    /**
     * Check if the trace actually exists on disk or not.
     *
     * @return If the trace is present
     */
    public boolean exists() {
        return fPath.toFile().exists();
    }

    /**
     * Get the filename of this trace
     *
     * @return The last segment (file name of this trace)
     */
    public @NonNull String getHostId() {
        return fHostId;
    }

    /**
     * Get the filename of this trace
     *
     * @return The last segment (file name of this trace)
     */
    public @NonNull String getFileName() {
        return String.valueOf(fPath.lastSegment());
    }

    /**
     * Get the path of the trace file
     *
     * @return The full path of the trace file
     */
    public @NonNull IPath getPath() {
        return fPath;
    }
}
