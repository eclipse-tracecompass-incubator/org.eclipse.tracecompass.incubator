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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;

import com.google.common.base.Objects;

/**
 * Complex causal link between subscriptions and publishers.
 *
 * @author Christophe Bedard
 */
public class Ros2MessageCausalLink {

    private final Set<@NonNull Ros2ObjectHandle> fSubs;
    private final Set<@NonNull Ros2ObjectHandle> fPubs;
    private final @NonNull Ros2MessageCausalLinkType fType;

    /**
     * Constructor
     *
     * @param subs
     *            the subscriptions
     * @param pubs
     *            the publishers
     * @param type
     *            the causal link type
     */
    public Ros2MessageCausalLink(Collection<@NonNull Ros2ObjectHandle> subs, Collection<@NonNull Ros2ObjectHandle> pubs, @NonNull Ros2MessageCausalLinkType type) {
        fSubs = new HashSet<>(subs);
        fPubs = new HashSet<>(pubs);
        fType = type;
    }

    /**
     * @return the subscriptions
     */
    public Set<@NonNull Ros2ObjectHandle> getSubs() {
        return fSubs;
    }

    /**
     * @return the publishers
     */
    public Set<@NonNull Ros2ObjectHandle> getPubs() {
        return fPubs;
    }

    /**
     * @return the message link type
     */
    public @NonNull Ros2MessageCausalLinkType getType() {
        return fType;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fSubs, fPubs, fType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Ros2MessageCausalLink o = (Ros2MessageCausalLink) obj;
        return fSubs.equals(o.fSubs) && fPubs.equals(o.fPubs) && fType.equals(o.fType);
    }

    @Override
    public String toString() {
        return String.format("Ros2MessageCausalLink: subs=%s, pubs=%s, type=%s", fSubs.toString(), fPubs.toString(), fType.toString()); //$NON-NLS-1$
    }
}
