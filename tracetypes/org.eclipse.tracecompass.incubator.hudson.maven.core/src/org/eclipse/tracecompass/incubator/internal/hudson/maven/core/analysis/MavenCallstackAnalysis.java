/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.hudson.maven.core.analysis;

import java.util.Objects;

import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackAnalysis;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/**
 * Maven callstack analysis
 *
 * @author Matthew Khouzam
 *
 */
public class MavenCallstackAnalysis extends CallStackAnalysis {

    @Override
    protected ITmfStateProvider createStateProvider() {
        return new MavenCallstackStateProvider(Objects.requireNonNull(getTrace()));
    }

}
