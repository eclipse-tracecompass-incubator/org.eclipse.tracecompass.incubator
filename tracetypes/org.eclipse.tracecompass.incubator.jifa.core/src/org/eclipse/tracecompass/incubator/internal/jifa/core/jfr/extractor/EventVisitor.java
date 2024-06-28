/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor;

import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedEvent;

public abstract class EventVisitor {

    /**
     * @param event
     */
    void visitUnsignedIntFlag(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitGarbageCollection(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitCPUInformation(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitEnvVar(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitCPCRuntimeInformation(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitActiveSetting(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitThreadStart(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitProcessCPULoad(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitThreadCPULoad(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitExecutionSample(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitNativeExecutionSample(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitExecuteVMOperation(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitObjectAllocationInNewTLAB(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitObjectAllocationOutsideTLAB(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitObjectAllocationSample(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitFileRead(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitFileWrite(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitFileForce(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitSocketRead(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitSocketWrite(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitMonitorEnter(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitThreadPark(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitClassLoad(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param event
     */
    void visitThreadSleep(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }
}
