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

import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostInfo;
import org.junit.Test;

/**
 * Tests for {@link HostInfo}
 *
 * @author Christophe Bedard
 */
public class HostInfoTest {

    HostInfo hostInfo1 = new HostInfo("hostid-a", "hostname-a");
    HostInfo hostInfo2 = new HostInfo("hostid-a", "hostname-a");
    HostInfo hostInfo3 = new HostInfo("hostid-a", "hostname-b");
    HostInfo hostInfo4 = new HostInfo("hostid-b", "hostname-b");
    HostInfo hostInfo5 = new HostInfo("hostid-b", "hostname-a");

    /**
     * Test equality.
     */
    @Test
    public void testEquals() {
        assertEquals(hostInfo1, hostInfo1);
        assertEquals(hostInfo1, hostInfo2);

        // Only host ID is used for equality
        assertEquals(hostInfo1, hostInfo3);
        assertEquals(hostInfo2, hostInfo3);

        assertNotEquals(hostInfo1, hostInfo4);
        assertNotEquals(hostInfo1, hostInfo5);
    }

    /**
     * Test comparison.
     */
    @Test
    public void testCompareTo() {
        assertEquals(0, hostInfo1.compareTo(hostInfo1));
        assertEquals(0, hostInfo1.compareTo(hostInfo2));
        assertEquals(0, hostInfo2.compareTo(hostInfo1));

        // Only hostname is used for comparison
        assertEquals(0, hostInfo1.compareTo(hostInfo5));
        assertEquals(0, hostInfo5.compareTo(hostInfo1));
        assertEquals(0, hostInfo3.compareTo(hostInfo4));
        assertEquals(0, hostInfo4.compareTo(hostInfo3));

        assertTrue(0 > hostInfo1.compareTo(hostInfo3));
        assertTrue(0 > hostInfo1.compareTo(hostInfo4));
        assertTrue(0 < hostInfo3.compareTo(hostInfo1));
        assertTrue(0 < hostInfo4.compareTo(hostInfo1));
    }
}
