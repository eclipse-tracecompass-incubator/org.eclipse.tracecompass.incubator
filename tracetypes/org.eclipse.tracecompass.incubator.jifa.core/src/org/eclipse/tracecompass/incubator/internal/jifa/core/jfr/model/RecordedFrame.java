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

import java.util.Objects;

public class RecordedFrame extends SymbolBase {
    private boolean javaFrame;
    /**
     * @param javaFrame the javaFrame to set
     */
    public void setJavaFrame(boolean javaFrame) {
        this.javaFrame = javaFrame;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @param bytecodeIndex the bytecodeIndex to set
     */
    public void setBytecodeIndex(int bytecodeIndex) {
        this.bytecodeIndex = bytecodeIndex;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(RecordedMethod method) {
        this.method = method;
    }

    /**
     * @param lineNumber the lineNumber to set
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    private String type;
    private int bytecodeIndex;
    private RecordedMethod method;
    private int lineNumber;

    public RecordedFrame(boolean javaFrame, String type, int bytecodeIndex, int lineNumber, RecordedMethod method) {
        this.javaFrame = javaFrame;
        this.type = type;
        this.bytecodeIndex = bytecodeIndex;
        this.lineNumber = lineNumber;
        this.method = method;
    }

    public RecordedFrame() {
    }

    @Override
    public boolean isEquals(Object b) {
        if (!(b instanceof RecordedFrame)) {
            return false;
        }

        RecordedFrame f2 = (RecordedFrame) b;

        return getBytecodeIndex() == f2.getBytecodeIndex()
                && getLineNumber() == f2.getLineNumber()
                && isJavaFrame() == f2.isJavaFrame()
                && this.getMethod().equals(f2.getMethod())
                && Objects.equals(getType(), f2.getType());
    }

    @Override
    public int genHashCode() {
        return Objects.hash(isJavaFrame(), getType(), getBytecodeIndex(), getMethod(), getLineNumber());
    }

    /**
     * @return the javaFrame
     */
    public boolean isJavaFrame() {
        return javaFrame;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the bytecodeIndex
     */
    public int getBytecodeIndex() {
        return bytecodeIndex;
    }

    /**
     * @return the method
     */
    public RecordedMethod getMethod() {
        return method;
    }

    /**
     * @return the lineNumber
     */
    public int getLineNumber() {
        return lineNumber;
    }
}
