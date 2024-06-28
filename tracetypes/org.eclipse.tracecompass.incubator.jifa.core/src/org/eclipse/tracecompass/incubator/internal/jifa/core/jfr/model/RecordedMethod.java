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

public class RecordedMethod extends SymbolBase {
    /**
     * @return the type
     */
    public RecordedClass getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(RecordedClass type) {
        this.type = type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the descriptor
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * @param descriptor the descriptor to set
     */
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * @return the modifiers
     */
    public int getModifiers() {
        return modifiers;
    }

    /**
     * @param modifiers the modifiers to set
     */
    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    /**
     * @return the hidden
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @param hidden the hidden to set
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    private RecordedClass type;
    private String name;
    private String descriptor;
    private int modifiers;
    private boolean hidden;

    @Override
    public boolean isEquals(Object b) {
        if (!(b instanceof RecordedMethod)) {
            return false;
        }

        RecordedMethod m2 = (RecordedMethod) b;

        return Objects.equals(descriptor, m2.getDescriptor())
                && Objects.equals(name, m2.getName())
                && modifiers == m2.getModifiers()
                && type.equals(m2.getType())
                && hidden == m2.isHidden();
    }

    @Override
    public int genHashCode() {
        return Objects.hash(type, name, descriptor, modifiers, hidden);
    }
}
