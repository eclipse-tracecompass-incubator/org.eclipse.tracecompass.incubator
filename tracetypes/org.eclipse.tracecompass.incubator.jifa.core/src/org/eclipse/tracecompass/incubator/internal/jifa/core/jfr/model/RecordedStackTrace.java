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

package org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model;

import java.util.List;
import java.util.Objects;

public class RecordedStackTrace extends SymbolBase {
    private boolean truncated;
    /**
     * @return the truncated
     */
    public boolean isTruncated() {
        return truncated;
    }

    /**
     * @param truncated the truncated to set
     */
    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    /**
     * @return the frames
     */
    public List<RecordedFrame> getFrames() {
        return frames;
    }

    /**
     * @param frames the frames to set
     */
    public void setFrames(List<RecordedFrame> frames) {
        this.frames = frames;
    }

    private List<RecordedFrame> frames;

    @Override
    public boolean isEquals(Object b) {
        if (!(b instanceof RecordedStackTrace)) {
            return false;
        }

        RecordedStackTrace t2 = (RecordedStackTrace) b;

        if (truncated != t2.isTruncated()) {
            return false;
        }

        if (frames == null) {
            return t2.getFrames() == null;
        }

        if (frames.size() != t2.getFrames().size()) {
            return false;
        }

        return frames.equals(t2.getFrames());
    }

    @Override
    public int genHashCode() {
        return Objects.hash(truncated, frames);
    }
}