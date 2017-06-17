/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.hudson.maven.core.analysis;

import java.util.Objects;

import org.eclipse.tracecompass.tmf.core.callstack.CallStackAnalysis;
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
