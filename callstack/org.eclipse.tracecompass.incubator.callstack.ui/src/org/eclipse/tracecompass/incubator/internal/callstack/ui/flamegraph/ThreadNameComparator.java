/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.callstack.ui.flamegraph;

import java.util.Comparator;

import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * Comparator to compare by thread name.
 *
 * @author Bernd Hufmann
 *
 */
class ThreadNameComparator implements Comparator<TimeGraphEntry> {
    @Override
    public int compare(TimeGraphEntry o1, TimeGraphEntry o2) {
        return o1.getName().compareTo(o2.getName());
    }
}
