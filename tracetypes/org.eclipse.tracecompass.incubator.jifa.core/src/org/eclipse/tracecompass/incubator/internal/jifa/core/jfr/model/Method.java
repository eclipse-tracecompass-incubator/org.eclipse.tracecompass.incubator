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

public class Method extends SymbolBase {
    private String packageName;

    private String type;

    private String name;

    private String descriptor;

    private String string;

    @Override
    public int genHashCode() {
        return Objects.hash(packageName, type, name, descriptor);
    }

    @Override
    public boolean isEquals(Object b) {
        if (!(b instanceof Method m2)) {
            return false;
        }

        return Objects.equals(packageName, m2.getPackageName())
                && Objects.equals(type, m2.getType())
                && Objects.equals(name, m2.getName())
                && Objects.equals(descriptor, m2.getDescriptor());
    }

    public String toString(boolean includeDescriptor) {
        if (string != null) {
            return string;
        }

        String str;
        if (this.descriptor != null && !this.descriptor.isEmpty() && includeDescriptor) {
            str = String.format("%s%s", this.name, this.descriptor);
        } else {
            str = this.name;
        }

        if (this.type != null && !this.type.isEmpty()) {
            str = String.format("%s.%s", this.type, str);
        }

        if (this.packageName != null && !this.packageName.isEmpty()) {
            str = String.format("%s.%s", this.packageName, str);
        }

        this.string = str;

        return str;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * @return the packageName
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the descriptor
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * @return the string
     */
    public String getString() {
        return string;
    }

    /**
     * @param packageName the packageName to set
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param descriptor the descriptor to set
     */
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * @param string the string to set
     */
    public void setString(String string) {
        this.string = string;
    }
}
