/**********************************************************************
 * Copyright (c) 2023 Apex.AI, Inc.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.ros2.core.tests.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostInfo;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcess;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessValue;
import org.junit.Test;

/**
 * Tests for {@link HostProcessValue}
 *
 * @author Christophe Bedard
 */
public class HostProcessValueTest {

    @NonNull
    HostInfo hostInfo1 = new HostInfo("hostid-a", "hostname-a");
    @NonNull
    HostProcess hostProcess1 = new HostProcess(hostInfo1, 1L);
    @NonNull
    HostProcess hostProcess2 = new HostProcess(hostInfo1, 2L);
    // Use concrete HostProcessPointer class
    HostProcessPointer value1 = new HostProcessPointer(hostProcess1, 1L);
    HostProcessPointer value2 = new HostProcessPointer(hostProcess1, 1L);
    HostProcessPointer value3 = new HostProcessPointer(hostProcess1, 2L);
    HostProcessPointer value4 = new HostProcessPointer(hostProcess2, 1L);
    HostProcessPointer value5 = new HostProcessPointer(hostProcess2, 2L);

    /**
     * Test equality.
     */
    @Test
    public void testEquality() {
        assertEquals(value1, value1);
        assertEquals(value1, value2);

        assertNotEquals(value1, value3);
        assertNotEquals(value3, value4);
        assertNotEquals(value4, value5);
    }

    /**
     * Test comparison.
     */
    @Test
    public void testCompareTo() {
        assertEquals(0, value1.compareTo(value1));
        assertEquals(0, value1.compareTo(value2));
        assertEquals(0, value2.compareTo(value1));

        assertTrue(0 > value1.compareTo(value3));
        assertTrue(0 > value3.compareTo(value4));
        assertTrue(0 > value4.compareTo(value5));
        assertTrue(0 < value3.compareTo(value1));
        assertTrue(0 < value4.compareTo(value3));
        assertTrue(0 < value5.compareTo(value4));
    }
}
