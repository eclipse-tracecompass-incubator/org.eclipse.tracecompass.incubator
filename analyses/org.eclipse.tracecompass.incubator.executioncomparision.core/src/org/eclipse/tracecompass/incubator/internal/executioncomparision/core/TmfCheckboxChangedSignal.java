/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparision.core;


import java.util.List;

import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;

/**
 * TmfCheckboxChangedSignal for handling check box change events
 *
 *  * @author Fateme Faraji Daneshgar
 */
public class TmfCheckboxChangedSignal extends TmfSignal {

    private List<String> fTraceList;

    /**
     * @param source
     *          the source class of signal
     * @param traceList
     *          the list of traces remains checked in the check box
     */
    public TmfCheckboxChangedSignal(Object source, List<String> traceList) {
        super(source);
        fTraceList = traceList;
    }

    /**
     * @return the list of traces remains checked in the check box
     */
    public List<String> getTraceList() {
        return fTraceList;
    }



}
