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

package org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2MessagesUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2SubCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * Wait message flow segment, representing unknown node/executor wait time due
 * to indirect message links.
 *
 * @author Christophe Bedard
 */
public class Ros2WaitMessageFlowSegment extends Ros2MessageFlowSegment {

    private final @NonNull Ros2SubCallbackInstance fSubCallbackInstance;
    private final @NonNull Ros2PubInstance fPubInstance;
    private final @NonNull Ros2SubscriptionObject fSubscription;
    private final @NonNull Ros2PublisherObject fPublisher;

    /**
     * Constructor
     *
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @param subCallbackInstance
     *            the corresponding subscription callback instance
     * @param pubInstance
     *            the corresponding publication instance
     * @param subscription
     *            the subscription
     * @param publisher
     *            the publisher
     */
    public Ros2WaitMessageFlowSegment(long startTime, long endTime, @NonNull Ros2SubCallbackInstance subCallbackInstance, @NonNull Ros2PubInstance pubInstance, @NonNull Ros2SubscriptionObject subscription, @NonNull Ros2PublisherObject publisher) {
        super(startTime, endTime, null);
        fSubCallbackInstance = subCallbackInstance;
        fPubInstance = pubInstance;
        fSubscription = subscription;
        fPublisher = publisher;
    }

    /**
     * @return the corresponding subscription callback instance
     */
    public @NonNull Ros2SubCallbackInstance getSubCallbackInstance() {
        return fSubCallbackInstance;
    }

    /**
     * @return the corresponding publication instance
     */
    public @NonNull Ros2PubInstance getPubInstance() {
        return fPubInstance;
    }

    /**
     * @return the subscription
     */
    public @NonNull Ros2SubscriptionObject getSubscription() {
        return fSubscription;
    }

    /**
     * @return the publisher
     */
    public @NonNull Ros2PublisherObject getPublisher() {
        return fPublisher;
    }

    @Override
    public @NonNull Collection<@NonNull Ros2MessageFlowSegment> getDirectSuccessors(@NonNull Ros2MessageFlowBuildInfo info) {
        /**
         * After a wait segment, the next segment is a single publication
         * segment.
         */
        ITmfStateInterval pubInstanceInterval = Ros2MessagesUtil.getPubInstanceInterval(info.getMessagesSs(), info.getObjectsSs(), getEndTime() + 1, getPublisher().getHandle());
        if (null == pubInstanceInterval) {
            return Collections.emptyList();
        }

        long startTime = pubInstanceInterval.getStartTime();
        long endTime = pubInstanceInterval.getEndTime();
        Ros2PubInstance pubInstance = (Ros2PubInstance) pubInstanceInterval.getValue();
        if (null == pubInstance) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new Ros2PublicationMessageFlowSegment(startTime, endTime, pubInstance, getPublisher()));
    }

    @Override
    public @NonNull Collection<@NonNull Ros2MessageFlowSegment> getDirectPredecessors(@NonNull Ros2MessageFlowBuildInfo info) {
        /**
         * Before a wait segment, the previous segment is a single subscription
         * callback segment.
         */
        ITmfStateInterval subCallbackInstanceInterval = Ros2MessagesUtil.getSubCallbackInstanceInterval(info.getMessagesSs(), info.getObjectsSs(), getStartTime() - 1, getSubscription().getHandle());
        if (null == subCallbackInstanceInterval) {
            return Collections.emptyList();
        }

        long startTime = subCallbackInstanceInterval.getStartTime();
        long endTime = subCallbackInstanceInterval.getEndTime();
        Ros2SubCallbackInstance subCallbackInstance = (Ros2SubCallbackInstance) subCallbackInstanceInterval.getValue();
        if (null == subCallbackInstance) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new Ros2SubCallbackMessageFlowSegment(startTime, endTime, subCallbackInstance, getSubscription()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(fSubCallbackInstance, fPubInstance, fSubscription, fPublisher, super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (null == obj) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof Ros2WaitMessageFlowSegment)) {
            return false;
        }
        Ros2WaitMessageFlowSegment o = (Ros2WaitMessageFlowSegment) obj;
        if (!fPubInstance.equals(o.fPubInstance)) {
            return false;
        }
        if (!fPublisher.equals(o.fPublisher)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format(
                "Ros2WaitMessageFlowSegment: %s, subCallbackInstance=[%s], pubInstance=[%s], sub=[%s], pub=[%s]", //$NON-NLS-1$
                super.toString(), fSubCallbackInstance.toString(), fPubInstance.toString(), fSubscription.toString(), fPublisher.toString());
    }
}
