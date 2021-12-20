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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator;

import java.util.Comparator;

/**
 * Comparator for BinaryFTraceCPUSectionIterator based on the time stamp of the
 * most current trace event that is read.
 *
 * @author Hoang Thuan Pham
 *
 */
public class BinaryFTraceCPUSectionIteratorComparator implements
        Comparator<BinaryFTraceCPUSectionIterator> {
    @Override
    public int compare(BinaryFTraceCPUSectionIterator a, BinaryFTraceCPUSectionIterator b) {
        return Long.compare(a.getCurrentTimeStamp(), b.getCurrentTimeStamp());
    }
}
