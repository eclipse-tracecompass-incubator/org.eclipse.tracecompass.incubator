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

public class JavaFrame extends Frame {
    public enum Type {
        INTERPRETER("Interpreted"),
        JIT("JIT compiled"),
        INLINE("Inlined"),
        NATIVE("Native");

        final String value;

        Type(String value) {
            this.value = value;
        }

        public static Type typeOf(String value) {
            for (Type type : Type.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException(value);
        }
    }

    private boolean isJavaFrame;

    private Type type;

    private long bci = -1;

    public boolean isJavaFrame() {
        return isJavaFrame;
    }

    public void setJavaFrame(boolean javaFrame) {
        isJavaFrame = javaFrame;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the bci
     */
    public long getBci() {
        return bci;
    }

    /**
     * @param type the type to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @param bci the bci to set
     */
    public void setBci(long bci) {
        this.bci = bci;
    }
}
