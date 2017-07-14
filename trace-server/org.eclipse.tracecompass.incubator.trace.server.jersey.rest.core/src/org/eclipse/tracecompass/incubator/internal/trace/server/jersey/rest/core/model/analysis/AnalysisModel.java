/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.analysis;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;

/**
 * Object to encapsulate the analysis
 *
 * @author Loic Prieur-Drevon
 */
@XmlRootElement
public class AnalysisModel {
    private final String fName;
    private final long fStart;
    private final long fEnd;

    /**
     * Parameterized constructor
     *
     * @param name
     *            trace name
     * @param ss
     *            underlying state system
     */
    public AnalysisModel(String name, @NonNull ITmfStateSystem ss) {
        fName = name;
        fStart = ss.getStartTime();
        fEnd = ss.getCurrentEndTime();
    }

    /**
     * Getter for the trace name
     *
     * @return the name of this analysis' trace
     */
    @XmlElement
    public String getName() {
        return fName;
    }

    /**
     * Getter for the start time of this analysis
     *
     * @return the start time of the underlying state system
     */
    @XmlElement
    public long getStart() {
        return fStart;
    }

    /**
     * Getter for the end time of this analysis
     *
     * @return the current end time of the underlying state system
     */
    @XmlElement
    public long getEnd() {
        return fEnd;
    }

}
