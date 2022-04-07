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

package org.eclipse.tracecompass.incubator.internal.ros2.core.model.messagelinks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.IRos2Model;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;

/**
 * Container for message causal links.
 *
 * @author Christophe Bedard
 */
public class Ros2MessageCausalLinksModel implements IRos2Model {

    private @NonNull Collection<@NonNull Ros2MessageCausalLink> fLinks;

    /**
     * Constructor
     */
    public Ros2MessageCausalLinksModel() {
        fLinks = new ArrayList<>();
    }

    /**
     * Add a new message link.
     *
     * @param subs
     *            the subscriptions
     * @param pubs
     *            the publishers
     * @param type
     *            the causal link type
     */
    public void addLink(Collection<@NonNull Ros2ObjectHandle> subs, Collection<@NonNull Ros2ObjectHandle> pubs, @NonNull Ros2MessageCausalLinkType type) {
        fLinks.add(new Ros2MessageCausalLink(subs, pubs, type));
    }

    /**
     * Get causal link(s) for a given publisher handle.
     *
     * @param publisherHandle
     *            the publisher handle
     * @return the message links
     */
    public Collection<@NonNull Ros2MessageCausalLink> getLinksForPub(@NonNull Ros2ObjectHandle publisherHandle) {
        return fLinks.stream().filter(l -> l.getPubs().contains(publisherHandle)).collect(Collectors.toUnmodifiableList());

    }

    /**
     * Get causal link(s) for a given subscription handle.
     *
     * @param subscriptionHandle
     *            the subscription handle
     * @return the message links
     */
    public Collection<@NonNull Ros2MessageCausalLink> getLinksForSub(@NonNull Ros2ObjectHandle subscriptionHandle) {
        return fLinks.stream().filter(l -> l.getSubs().contains(subscriptionHandle)).collect(Collectors.toUnmodifiableList());
    }
}
