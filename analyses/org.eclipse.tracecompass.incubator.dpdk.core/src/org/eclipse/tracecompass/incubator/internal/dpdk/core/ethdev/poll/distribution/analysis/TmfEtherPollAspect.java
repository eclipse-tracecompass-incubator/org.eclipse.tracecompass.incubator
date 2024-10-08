/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.poll.distribution.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;

/**
 * Aspect representing a poll issued by a PMD thread for a specific Ethernet queue, at the timestamp of the
 * event.
 *
 * @author Adel Belkhiri
 */
public class TmfEtherPollAspect implements ITmfEventAspect<Integer> {

    private static final Map<String, TmfEtherPollAspect> instances = new HashMap<>();

    private final String fName;

    /**
     * Constructor
     *
     * @param name
     *      The name of this aspect, which is the name of the queue associated to the Ethernet port.
     */
    public TmfEtherPollAspect(String name) {
        fName = name;
    }

    @Override
    public String getName() {
        return fName;
    }

    /**
     * Factory method to create new instances
     * @param event
     *      ITmfEvent instance
     * @return
     *      An instance of an existing {@link TmfEtherPollAspect} or the newly created one
     */
    public static synchronized TmfEtherPollAspect getInstance(ITmfEvent event) {
        Integer portId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldPortId());
        Integer queueId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldQueueId());
        String name = "P" + Objects.requireNonNull(portId).toString() + "/Q" + Objects.requireNonNull(queueId).toString();  //$NON-NLS-1$//$NON-NLS-2$

        if (!instances.containsKey(name)) {
            instances.put(name, new TmfEtherPollAspect(name));
        }
        return instances.get(name);
    }

    /**
     * Gets all the aspects instances
     * @return
     *      A collection of {@link TmfEtherPollAspect}
     */
    public static Collection<TmfEtherPollAspect> getAllInstances() {
        return instances.values();
    }

    @Override
    public @NonNull String getHelpText() {
        return Messages.getMessage(Messages.AspectHelpText_PortQueueName);
    }

    @Override
    public @Nullable Integer resolve(ITmfEvent event) {
        Integer nbRxPkts = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbRxPkts());
        return Objects.requireNonNull(nbRxPkts);
    }
}
