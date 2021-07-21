/*******************************************************************************
 * Copyright (c) 2021 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.system.core.analsysis.httpd;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;

/**
 * Retry Analysis, catches 40x -> 20x transitions... Fights the "we've tried
 * nothing now it works" mentality.
 *
 * @author Matthew Khouzam
 */
public class HttpdRetryConnectionAnalysis extends TmfStateSystemAnalysisModule {

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new HttpdRetryConnectionStateProvider(Objects.requireNonNull(getTrace()));
    }

}
