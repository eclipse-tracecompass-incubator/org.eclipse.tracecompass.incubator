/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.tracex.ui.swtbot.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Tests TraceX UI bundle activation and view registration.
 */
public class TraceXUiExtensionTest {

    private static final String BUNDLE_ID = "org.eclipse.tracecompass.incubator.tracex.ui"; //$NON-NLS-1$
    private static final String VIEW_ID = "org.eclipse.tracecompass.incubator.tracex.ui.view"; //$NON-NLS-1$

    /** Tests that the UI bundle and its execution view are discoverable. */
    @Test
    public void testViewExtension() throws BundleException {
        Bundle bundle = Platform.getBundle(BUNDLE_ID);
        assertNotNull(bundle);
        bundle.start(Bundle.START_TRANSIENT);
        assertEquals(Bundle.ACTIVE, bundle.getState());

        IConfigurationElement match = null;
        for (IConfigurationElement element : Platform.getExtensionRegistry()
                .getConfigurationElementsFor("org.eclipse.ui.views")) { //$NON-NLS-1$
            if ("view".equals(element.getName()) && VIEW_ID.equals(element.getAttribute("id"))) { //$NON-NLS-1$ //$NON-NLS-2$
                match = element;
                break;
            }
        }
        assertNotNull("Missing view extension " + VIEW_ID, match); //$NON-NLS-1$
        assertEquals("org.eclipse.tracecompass.incubator.internal.tracex.ui.view.TraceXView", //$NON-NLS-1$
                match.getAttribute("class")); //$NON-NLS-1$
    }
}
