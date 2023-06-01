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
import org.junit.Test;

/**
 * Tests for {@link HostProcess}
 *
 * @author Christophe Bedard
 */
public class HostProcessTest {

    @NonNull
    HostInfo hostInfo1 = new HostInfo("hostid", "hostname-a");
    @NonNull
    HostInfo hostInfo2 = new HostInfo("hostid", "hostname-b");
    HostProcess hostProcess1 = new HostProcess(hostInfo1, 1L);
    HostProcess hostProcess2 = new HostProcess(hostInfo1, 1L);
    HostProcess hostProcess3 = new HostProcess(hostInfo2, 1L);
    HostProcess hostProcess4 = new HostProcess(hostInfo2, 2L);
    HostProcess hostProcess5 = new HostProcess(hostInfo1, 2L);

    /**
     * Test equality.
     */
    @Test
    public void testEquality() {
        assertEquals(hostProcess1, hostProcess1);
        assertEquals(hostProcess1, hostProcess2);
        assertEquals(hostProcess2, hostProcess3);

        assertNotEquals(hostProcess3, hostProcess4);
        assertNotEquals(hostProcess3, hostProcess5);
        assertNotEquals(hostProcess1, hostProcess5);
    }

    /**
     * Test comparison.
     */
    @Test
    public void testCompareTo() {
        assertEquals(0, hostProcess1.compareTo(hostProcess1));
        assertEquals(0, hostProcess1.compareTo(hostProcess2));
        assertEquals(0, hostProcess2.compareTo(hostProcess1));

        assertTrue(0 > hostProcess1.compareTo(hostProcess3));
        assertTrue(0 > hostProcess3.compareTo(hostProcess4));
        assertTrue(0 > hostProcess5.compareTo(hostProcess3));
        assertTrue(0 < hostProcess3.compareTo(hostProcess1));
        assertTrue(0 < hostProcess4.compareTo(hostProcess3));
        assertTrue(0 < hostProcess3.compareTo(hostProcess5));
    }
}
