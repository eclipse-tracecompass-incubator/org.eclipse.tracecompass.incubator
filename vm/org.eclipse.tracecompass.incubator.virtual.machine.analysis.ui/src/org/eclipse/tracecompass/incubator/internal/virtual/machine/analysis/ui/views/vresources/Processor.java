/*******************************************************************************
 * Copyright (c) 2016-2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vresources;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Represents a processor in a virtual environment
 *
 * TODO: Move this class to the model
 *
 * @author Cedric Biancheri
 * @author Geneviève Bastien
 */
@NonNullByDefault
public class Processor {

    private final int fProcNo;
    private Machine fMachine;

    /**
     * Constructor
     *
     * @param p
     *            The processor number
     * @param m
     *            The machine this processor belongs to
     */
    public Processor(String p, Machine m) {
        fProcNo = Integer.parseInt(p);
        fMachine = m;
    }

    @Override
    public String toString() {
        return String.valueOf(fProcNo);
    }

    /**
     * Get the machine this processor belongs to
     *
     * @return The machine
     */
    public Machine getMachine() {
        return fMachine;
    }

    /**
     * Get the processor number
     *
     * @return The processor number
     */
    public int getNumber() {
        return fProcNo;
    }

}
