/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracecompass.core.trace;

import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

/**
 * @author Geneviève Bastien
 */
public class LttngTraceCompassJulTrace extends CtfTmfTrace {

    /**
     * Default constructor
     */
    public LttngTraceCompassJulTrace() {
        super(LttngTraceCompassJulEventFactory.instance());

    }

//    @Override
//    protected int getAverageEventSize() {
//        return 300;
//    }
}
