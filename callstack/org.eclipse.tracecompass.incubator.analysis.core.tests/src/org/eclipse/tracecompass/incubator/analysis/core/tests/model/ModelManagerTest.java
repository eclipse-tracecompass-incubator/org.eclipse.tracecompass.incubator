/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.tests.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.junit.Test;

/**
 * Test the {@link ModelManager} class
 *
 * @author Geneviève Bastien
 */
public class ModelManagerTest {

    /**
     * Test the retrieved model from a host
     */
    @Test
    public void testGetModelFor() {
        String host1 = "host1";
        String host2 = "host2";

        IHostModel model1 = ModelManager.getModelFor(host1);
        IHostModel model2 = ModelManager.getModelFor(host1);
        IHostModel model3 = ModelManager.getModelFor(host2);

        assertEquals(model1, model2);
        assertTrue(model1 == model2);
        assertNotEquals(model1, model3);
    }
}
