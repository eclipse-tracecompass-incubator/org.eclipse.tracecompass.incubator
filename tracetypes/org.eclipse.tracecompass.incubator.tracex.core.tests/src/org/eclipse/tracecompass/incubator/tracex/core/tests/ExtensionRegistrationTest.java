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

package org.eclipse.tracecompass.incubator.tracex.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.junit.Test;

/**
 * Tests the TraceX core extension registrations.
 */
public class ExtensionRegistrationTest {

    /** Tests the trace type, analysis, and data-provider registrations. */
    @Test
    public void testCoreExtensions() {
        assertExtension("org.eclipse.linuxtools.tmf.core.tracetype", "type", //$NON-NLS-1$ //$NON-NLS-2$
                "org.eclipse.tracecompass.incubator.tracex.core.trace", "trace_type", //$NON-NLS-1$ //$NON-NLS-2$
                "org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace"); //$NON-NLS-1$
        assertExtension("org.eclipse.linuxtools.tmf.core.analysis", "module", //$NON-NLS-1$ //$NON-NLS-2$
                "org.eclipse.tracecompass.incubator.tracex.core.analysis", "analysis_module", //$NON-NLS-1$ //$NON-NLS-2$
                "org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXAnalysis"); //$NON-NLS-1$
        assertExtension("org.eclipse.tracecompass.tmf.core.dataprovider", "dataProviderFactory", //$NON-NLS-1$ //$NON-NLS-2$
                "org.eclipse.tracecompass.incubator.tracex.core.dataprovider", "class", //$NON-NLS-1$ //$NON-NLS-2$
                "org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXDataProviderFactory"); //$NON-NLS-1$
    }

    private static void assertExtension(String extensionPoint, String elementName, String id,
            String classAttribute, String expectedClass) {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        assertNotNull(registry);
        IConfigurationElement match = null;
        for (IConfigurationElement element : registry.getConfigurationElementsFor(extensionPoint)) {
            if (elementName.equals(element.getName()) && id.equals(element.getAttribute("id"))) { //$NON-NLS-1$
                match = element;
                break;
            }
        }
        assertNotNull("Missing extension " + id, match); //$NON-NLS-1$
        assertEquals(expectedClass, match.getAttribute(classAttribute));
    }
}
