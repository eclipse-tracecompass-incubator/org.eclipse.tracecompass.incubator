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

public class Frame extends SymbolBase {

    private Method method;

    private int line;

    private String string;

    @Override
    public String toString() {
        if (this.string != null) {
            return string;
        }

        if (this.line == 0) {
            this.string = method.toString();
        } else {
            this.string = String.format("%s:%d", method, line);
        }

        return this.string;
    }

    @Override
    public int genHashCode() {
        return Objects.hash(method, line);
    }

    @Override
    public boolean isEquals(Object b) {
        if (!(b instanceof Frame f2)) {
            return false;
        }

        return line == f2.getLine() && method.equals(f2.getMethod());
    }

    /**
     * @return the method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * @return the line
     */
    public int getLine() {
        return line;
    }

    /**
     * @return the string
     */
    public String getString() {
        return string;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(Method method) {
        this.method = method;
    }

    /**
     * @param line the line to set
     */
    public void setLine(int line) {
        this.line = line;
    }

    /**
     * @param string the string to set
     */
    public void setString(String string) {
        this.string = string;
    }
}
