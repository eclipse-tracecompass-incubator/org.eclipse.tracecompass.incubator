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

public class JavaMethod extends Method {
    private int modifiers;
    private boolean hidden;

    @Override
    public int genHashCode() {
        return Objects.hash(modifiers, hidden, getPackageName(), getType(), getName(), getDescriptor());
    }

    @Override
    public boolean equals(Object b) {
        if (this == b) {
            return true;
        }

        if (b == null) {
            return false;
        }

        if (!(b instanceof JavaMethod)) {
            return false;
        }

        JavaMethod m2 = (JavaMethod) b;

        return modifiers == m2.modifiers && hidden == m2.hidden && super.equals(m2);
    }

    /**
     * @return the modifiers
     */
    public int getModifiers() {
        return modifiers;
    }

    /**
     * @return the hidden
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @param modifiers the modifiers to set
     */
    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    /**
     * @param hidden the hidden to set
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
