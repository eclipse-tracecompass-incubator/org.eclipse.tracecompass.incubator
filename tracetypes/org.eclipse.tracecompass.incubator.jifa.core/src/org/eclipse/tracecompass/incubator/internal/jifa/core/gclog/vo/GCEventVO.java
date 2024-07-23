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

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This class is used for interaction with front end.
 */
public class GCEventVO {
    private Map<String, Object> info = new HashMap<>();
    private List<GCEventVO> phases = new ArrayList<>();
    // todo: add diagnostic info

    public void saveInfo(String type, Object value) {
        info.put(type, value);
    }

    public void addPhase(GCEventVO phase) {
        phases.add(phase);
    }

    private String infoToString() {
        return info.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(infoToString());
        if (phases != null) {
            for (GCEventVO phase : phases) {
                sb.append("\n    ").append(phase.infoToString());
            }
        }
        return sb.toString();
    }
}
