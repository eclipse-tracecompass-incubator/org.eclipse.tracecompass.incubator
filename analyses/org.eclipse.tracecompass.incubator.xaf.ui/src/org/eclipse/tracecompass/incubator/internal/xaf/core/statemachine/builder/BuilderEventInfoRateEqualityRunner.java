/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.builder;

/**
 * TODO Review this class
 *
 * @author Raphaël Beamonte
 */
public class BuilderEventInfoRateEqualityRunner extends EqualityRunner<BuilderEventInfo> {

    private final double rate;

    /**
     * Constructor
     *
     * @param rate
     *            the rate
     */
    public BuilderEventInfoRateEqualityRunner(double rate) {
        this.rate = rate;
    }

    @Override
    public boolean isEqual(BuilderEventInfo bei0, BuilderEventInfo bei1) {
        return bei0.getMatchingRate(bei1) >= rate;
    }

    @Override
    public BuilderEventInfo commonValue(BuilderEventInfo bei0, BuilderEventInfo bei1) {
        return bei0.getCommonBuilderEventInfo(bei1);
    }
}