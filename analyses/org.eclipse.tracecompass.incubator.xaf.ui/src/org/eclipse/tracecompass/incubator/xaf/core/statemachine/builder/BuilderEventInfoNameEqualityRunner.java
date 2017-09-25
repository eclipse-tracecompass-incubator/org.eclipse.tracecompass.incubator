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
public class BuilderEventInfoNameEqualityRunner extends EqualityRunner<BuilderEventInfo> {
    @Override
    public boolean isEqual(BuilderEventInfo bei0, BuilderEventInfo bei1) {
        return bei0.getEventName().equals(bei1.getEventName());
    }

    @Override
    public BuilderEventInfo commonValue(BuilderEventInfo bei0, BuilderEventInfo bei1) {
        return new BuilderEventInfo(bei0.getEventName());
    }
}