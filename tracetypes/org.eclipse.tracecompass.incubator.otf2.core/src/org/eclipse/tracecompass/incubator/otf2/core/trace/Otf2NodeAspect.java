/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.otf2.core.trace;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;

/**
 * Generic aspect used to resolve the name of a node in the system tree
 *
 * @author Yoann Heitz
 */
public abstract class Otf2NodeAspect implements ITmfEventAspect<String> {

    private final Map<Long, String> fLocationIdToNodeName;

    /**
     * Constructor for this aspect
     */
    public Otf2NodeAspect() {
        fLocationIdToNodeName = new HashMap<>();
    }

    /**
     * Fills the map with locations ids that point to nodenames because these
     * locations represent those nodes.
     *
     * @param locationId
     *            the ID of the location for which a new entry will be filled
     * @param nodeName
     *            the name of the node on which the location is located
     */
    public void addNodeName(Long locationId, String nodeName) {
        fLocationIdToNodeName.put(locationId, nodeName);
    }

    @Override
    public @Nullable String resolve(ITmfEvent event) {
        Long locationId = event.getContent().getFieldValue(Long.class, IOtf2Fields.OTF2_LOCATION_ID);
        return fLocationIdToNodeName.get(locationId);
    }
}
