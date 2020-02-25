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