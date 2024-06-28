/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo.GCLogStyle;

public class GCLogParsingMetadata {
    private GCCollectorType collector;
    private GCLogStyle style;

    public GCLogParsingMetadata(GCCollectorType unknown, GCLogStyle unknown2) {
        collector = unknown;
        style = unknown2;
    }

    /**
     * @return the collector
     */
    public GCCollectorType getCollector() {
        return collector;
    }

    /**
     * @param collector
     *            the collector to set
     */
    public void setCollector(GCCollectorType collector) {
        this.collector = collector;
    }

    /**
     * @return the style
     */
    public GCLogStyle getStyle() {
        return style;
    }

    /**
     * @param style
     *            the style to set
     */
    public void setStyle(GCLogStyle style) {
        this.style = style;
    }
}
