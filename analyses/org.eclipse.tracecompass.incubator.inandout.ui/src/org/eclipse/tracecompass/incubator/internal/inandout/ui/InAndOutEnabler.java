/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.inandout.ui;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.inandout.core.analysis.InAndOutAnalysisModule;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfAnalysisElement;

/**
 * In and out enabler
 */
public class InAndOutEnabler extends PropertyTester {

    @Override
    public boolean test(@Nullable Object receiver, @Nullable String property, Object @Nullable [] args, @Nullable Object expectedValue) {
        TmfAnalysisElement analysis = (TmfAnalysisElement) receiver;
        if (analysis != null) {
            return analysis.getAnalysisId().equals(InAndOutAnalysisModule.ID);
        }
        return false;
    }
}
