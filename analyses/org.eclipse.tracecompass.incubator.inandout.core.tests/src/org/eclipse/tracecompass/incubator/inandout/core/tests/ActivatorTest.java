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

package org.eclipse.tracecompass.incubator.inandout.core.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.tracecompass.common.core.TraceCompassActivator;
import org.eclipse.tracecompass.incubator.internal.inandout.core.Activator;
import org.junit.Test;

/**
 * Test the activator name. This class is there mostly to create a non empty
 * test plugin.
 */
public class ActivatorTest extends Plugin {

    /**
     * Test the Activator instance's id.
     */
    @Test
    public void testActivator() {
        TraceCompassActivator instance = Activator.getInstance();
        assertEquals("org.eclipse.tracecompass.incubator.inandout.core", instance.getPluginId());
    }
}
