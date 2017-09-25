/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.builder;

/**
 * @author Raphaël Beamonte
 */
public class BuilderEventInfoRateEqualityRunner extends EqualityRunner<BuilderEventInfo> {

    private final double rate;

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