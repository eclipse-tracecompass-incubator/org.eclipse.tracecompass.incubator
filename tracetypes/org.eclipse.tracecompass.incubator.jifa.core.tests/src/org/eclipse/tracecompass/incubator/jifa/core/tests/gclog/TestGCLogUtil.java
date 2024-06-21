/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.tracecompass.incubator.jifa.core.tests.gclog;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.DoubleData;
import org.junit.Assert;
import org.junit.Test;

public class TestGCLogUtil {
    @Test
    public void testDoubleData() {
        DoubleData doubleData = new DoubleData(true);
        doubleData.add(1);
        doubleData.add(2);
        doubleData.add(3);
        doubleData.add(4);
        Assert.assertEquals(doubleData.getPercentile(0.99), 0.03 * 3 + 0.97 * 4, Constant.EPS);
        Assert.assertEquals(doubleData.getPercentile(0.75), 0.75 * 3 + 0.25 * 4, Constant.EPS);
        doubleData.add(0);
        Assert.assertEquals(doubleData.getMedian(), 2, Constant.EPS);
        Assert.assertEquals(doubleData.average(), 2, Constant.EPS);
        Assert.assertEquals(doubleData.getMax(), 4, Constant.EPS);
        Assert.assertEquals(doubleData.getMin(), 0, Constant.EPS);
        Assert.assertEquals(doubleData.getN(), 5, Constant.EPS);
    }
}
