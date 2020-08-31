/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.kernel.core.io.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.kernel.core.io.IoAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateIntervalStub;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateSystemTestUtils;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestHelper;
import org.junit.Test;

/**
 * Test the IO state system content
 *
 * @author Geneviève Bastien
 */
public class IoStateProviderTest extends AbstractTestInputOutput {

    private static final String STATE_SYSTEM_PATH = "testfiles/stateSystem/expectedIoStateProvider";

    /**
     * Test that the analysis executes without problems
     *
     * @throws IOException
     *             Exception thrown by test files
     */
    @Test
    public void testAnalysisExecution() throws IOException {
        IoAnalysis module = getModule();
        /* Make sure the analysis hasn't run yet */
        assertNull(module.getStateSystem());

        /* Execute the analysis */
        assertTrue(TmfTestHelper.executeAnalysis(module));
        ITmfStateSystem stateSystem = module.getStateSystem();
        assertNotNull(stateSystem);

        List<String> expectedStrings = Files.readAllLines(Paths.get(STATE_SYSTEM_PATH));
        int startTime = (int) stateSystem.getStartTime();
        int endTime = (int) stateSystem.getCurrentEndTime();
        for (int i = 0; i < expectedStrings.size(); i++) {
            // Prepare the attributes
            String expectedString = expectedStrings.get(i);
            String[] split = expectedString.split(":");

            assertEquals(expectedString, 3, split.length);
            String attributePath = split[0];
            String dataType = split[1];
            String valuesString = split[2];

            String[] attributes = attributePath.split(",");
            String[] values = valuesString.split(",");
            assertTrue(values.length % 2 == 0);

            ITmfStateInterval last = null;
            List<ITmfStateInterval> intervals = new ArrayList<>();
            for (int j = 0; j < values.length; j = j + 2) {
                String timeString = values[j];
                String valueString = values[j + 1];
                int intervalStart = Integer.parseInt(timeString);
                // Add a null interval at the beginning if necessary
                if (last == null && intervalStart != startTime) {
                    intervals.add(new StateIntervalStub(startTime, intervalStart - 1, (Object) null));
                }
                int intervalEnd = (j + 2 < values.length) ? Integer.parseInt(values[j + 2]) - 1 : endTime;
                last = new StateIntervalStub(intervalStart, intervalEnd, convertData(valueString, dataType));
                intervals.add(last);
            }

            StateSystemTestUtils.testIntervalForAttributes(stateSystem, intervals, attributes);
        }
    }

    private static @Nullable Object convertData(String valueString, String dataType) {
        if (valueString.equals("null")) {
            return null;
        }
        switch (dataType) {
        case "long":
            return Long.parseLong(valueString);
        case "int":
            return Integer.parseInt(valueString);
        case "string":
            return valueString;
        default:
            fail("Unknown data type: " + dataType);
            break;
        }
        return null;
    }

}
