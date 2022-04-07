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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2MessageTransportInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2SubCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * Transport message flow segment, between DDS write and message taking.
 *
 * @author Christophe Bedard
 */
public class Ros2TransportMessageFlowSegment extends Ros2MessageFlowSegment {

    private final @NonNull Ros2MessageTransportInstance fTransportInstance;
    private final @NonNull Ros2PublisherObject fPublisher;
    private final @NonNull Ros2SubscriptionObject fSubscription;

    /**
     * Constructor
     *
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @param transportInstance
     *            the corresponding message transport instance
     * @param publisher
     *            the source publisher
     * @param subscription
     *            the destination subscription
     */
    public Ros2TransportMessageFlowSegment(long startTime, long endTime, @NonNull Ros2MessageTransportInstance transportInstance, @NonNull Ros2PublisherObject publisher, @NonNull Ros2SubscriptionObject subscription) {
        super(startTime, endTime, null);
        fTransportInstance = transportInstance;
        fPublisher = publisher;
        fSubscription = subscription;
    }

    /**
     * @return the corresponding message transport instance
     */
    public @NonNull Ros2MessageTransportInstance getTransportInstance() {
        return fTransportInstance;
    }

    /**
     * @return the source publisher
     */
    public @NonNull Ros2PublisherObject getPublisher() {
        return fPublisher;
    }

    /**
     * @return the destination subscription
     */
    public @NonNull Ros2SubscriptionObject getSubscription() {
        return fSubscription;
    }

    @Override
    public @NonNull Collection<@NonNull Ros2MessageFlowSegment> getDirectSuccessors(@NonNull Ros2MessageFlowBuildInfo info) {
        /**
         * After a transport segment, the next segment is a single subscription
         * callback segment.
         */
        ITmfStateInterval subCallbackInstanceInterval = Ros2MessagesUtil.getSubCallbackInstanceInterval(info.getMessagesSs(), info.getObjectsSs(), getTransportInstance().getDestinationTimestamp(), getTransportInstance().getSubscriptionHandle());
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
    public @NonNull Collection<@NonNull Ros2MessageFlowSegment> getDirectPredecessors(@NonNull Ros2MessageFlowBuildInfo info) {
        /**
         * Before a transport segment, the previous segment is a single
         * publication segment.
         */
        ITmfStateInterval pubInstanceInterval = Ros2MessagesUtil.getPubInstanceInterval(info.getMessagesSs(), info.getObjectsSs(), getTransportInstance().getSourceTimestamp() - 1, getTransportInstance().getPublisherHandle());
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
    public int hashCode() {
        return Objects.hash(fTransportInstance, fPublisher, fSubscription, super.hashCode());
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
        if (!(obj instanceof Ros2TransportMessageFlowSegment)) {
            return false;
        }
        Ros2TransportMessageFlowSegment o = (Ros2TransportMessageFlowSegment) obj;
        if (!fTransportInstance.equals(o.fTransportInstance)) {
            return false;
        }
        if (!fPublisher.equals(o.fPublisher)) {
            return false;
        }
        if (!fSubscription.equals(o.fSubscription)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format(
                "Ros2TransportMessageFlowSegment: %s, transportInstance=[%s], pub=[%s], sub=[%s]", //$NON-NLS-1$
                super.toString(), fTransportInstance.toString(), fPublisher.toString(), fSubscription.toString());
    }
}
