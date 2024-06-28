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

public class RecordedClass extends SymbolBase {
    private String packageName;
    private String name;
    /**
     * @param packageName the packageName to set
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param fullName the fullName to set
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    private String fullName;

    public String getFullName() {
        if (fullName == null) {
            fullName = getPackageName() + "." + getName();
        }
        return fullName;
    }

    @Override
    public boolean isEquals(Object b) {
        if (!(b instanceof RecordedClass)) {
            return false;
        }

        RecordedClass c2 = (RecordedClass) b;

        return Objects.equals(getPackageName(), c2.getPackageName()) && Objects.equals(getName(), c2.getName());
    }

    @Override
    public int genHashCode() {
        return Objects.hash(getPackageName(), getName());
    }

    /**
     * @return the packageName
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
}
