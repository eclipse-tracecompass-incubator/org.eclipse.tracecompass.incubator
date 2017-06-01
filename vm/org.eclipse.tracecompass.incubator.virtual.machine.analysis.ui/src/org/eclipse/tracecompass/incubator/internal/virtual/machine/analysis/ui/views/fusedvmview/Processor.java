/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.fusedvmview;

/**
 * @author Cedric Biancheri
 */
public class Processor {
    private String n;
    private Boolean highlighted;
    private Machine machine;
    private float heightFactory;

    public Processor(String p, Machine m){
        n = p;
        highlighted = true;
        heightFactory = FusedVMViewPresentationProvider.FULL_HEIGHT;
        machine = m;
    }

    @Override
    public String toString(){
        return n;
    }

    public Boolean isHighlighted(){
        return highlighted;
    }

    public void setHighlighted(Boolean b) {
        highlighted = b;
    }

    public Machine getMachine() {
        return machine;
    }

    public String getNumber() {
        return n;
    }

    public float getHeightFactory() {
        return heightFactory;
    }
}
