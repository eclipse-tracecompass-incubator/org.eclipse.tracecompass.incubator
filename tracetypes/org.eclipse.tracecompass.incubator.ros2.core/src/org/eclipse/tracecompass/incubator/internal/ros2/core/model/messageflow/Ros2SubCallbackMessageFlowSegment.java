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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2MessagesUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messagelinks.Ros2MessageCausalLink;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackPublicationInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2MessageTransportInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2SubCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * Subscription callback message flow segment, from subscription message taking
 * until the end of the callback instance.
 *
 * @author Christophe Bedard
 */
public class Ros2SubCallbackMessageFlowSegment extends Ros2MessageFlowSegment {

    private final @NonNull Ros2SubCallbackInstance fCallbackInstance;
    private final @NonNull Ros2SubscriptionObject fSubscription;

    /**
     * Constructor
     *
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @param callbackInstance
     *            the corresponding subscription callback instance
     * @param subscription
     *            the subscription
     */
    public Ros2SubCallbackMessageFlowSegment(long startTime, long endTime, @NonNull Ros2SubCallbackInstance callbackInstance, @NonNull Ros2SubscriptionObject subscription) {
        super(startTime, endTime, subscription.getNodeHandle());
        fCallbackInstance = callbackInstance;
        fSubscription = subscription;
    }

    /**
     * @return the corresponding subscription callback instance
     */
    public @NonNull Ros2SubCallbackInstance getCallbackInstance() {
        return fCallbackInstance;
    }

    /**
     * @return the subscription
     */
    public @NonNull Ros2SubscriptionObject getSubscription() {
        return fSubscription;
    }

    @Override
    public @NonNull Collection<@NonNull Ros2MessageFlowSegment> getDirectSuccessors(@NonNull Ros2MessageFlowBuildInfo info) {
        /**
         * After a subscription callback segment, the next segments are:
         *
         * <pre>
         *   * callback publication segments (linking to a publication segment);
         *   * wait segments.
         * </pre>
         */
        Collection<@NonNull Ros2MessageFlowSegment> next = new ArrayList<>();

        // Callback-publication
        Iterator<@NonNull Ros2CallbackPublicationInstance> callbackPublicationLinks = Ros2MessagesUtil.getOutgoingCallbackPublicationLinks(info.getMessagesSs(), getSubscription().getHandle(), getStartTime(), getEndTime()).iterator();
        while (callbackPublicationLinks.hasNext()) {
            Ros2CallbackPublicationInstance callbackPublicationInstance = callbackPublicationLinks.next();
            long time = callbackPublicationInstance.getPublicationTimestamp();
            Ros2PublisherObject pubObject = Ros2ObjectsUtil.getPublisherObjectFromHandle(info.getObjectsSs(), time, callbackPublicationInstance.getPublisherHandle());
            if (null == pubObject) {
                continue;
            }
            next.add(new Ros2CallbackPubMessageFlowSegment(time, time, callbackPublicationInstance, getSubscription(), pubObject));
        }

        // Wait link
        ITmfStateInterval nextSubCallbackInstanceInterval = Ros2MessagesUtil.getNextSubCallbackInstanceInterval(info.getMessagesSs(), info.getObjectsSs(), getEndTime() + 1, getSubscription().getHandle());
        if (null == nextSubCallbackInstanceInterval) {
            return next;
        }
        long nextSubCallbackInstanceStartTime = nextSubCallbackInstanceInterval.getStartTime();
        for (@NonNull
        Ros2MessageCausalLink link : info.getMessageLinksModel().getLinksForSub(getSubscription().getHandle())) {
            for (Ros2ObjectHandle pubHandle : link.getPubs()) {
                switch (link.getType()) {
                case PERIODIC_ASYNC:
                    getWaitPeriodicAsyncSegmentsAfterSubCallback(info, next, pubHandle, nextSubCallbackInstanceStartTime);
                    break;
                case PARTIAL_SYNC:
                    getWaitPartialSyncSegmentsAfterSubCallback(info, next, pubHandle);
                    break;
                default:
                    throw new IllegalStateException();
                }
            }
        }
        return next;
    }

    private void getWaitPeriodicAsyncSegmentsAfterSubCallback(@NonNull Ros2MessageFlowBuildInfo info, Collection<@NonNull Ros2MessageFlowSegment> next, @NonNull Ros2ObjectHandle pubHandle,
            long nextSubCallbackInstanceStartTime) {
        /**
         * Find all next publications for each publisher in the link before the
         * next subscription callback for the current subscription.
         */
        long currentSubCallbackInstanceEndTime = getCallbackInstance().getCallbackInstance().getEndTime();
        for (@NonNull
        ITmfStateInterval nextPubInstanceInterval : Ros2MessagesUtil.getPubInstanceIntervalsBetween(info.getMessagesSs(), info.getObjectsSs(), currentSubCallbackInstanceEndTime, nextSubCallbackInstanceStartTime, pubHandle)) {
            Ros2PubInstance nextPubInstance = (Ros2PubInstance) nextPubInstanceInterval.getValue();
            if (null != nextPubInstance) {
                Ros2PublisherObject pubObject = Ros2ObjectsUtil.getPublisherObjectFromHandle(info.getObjectsSs(), nextPubInstanceInterval.getStartTime(), pubHandle);
                if (null != pubObject) {
                    next.add(new Ros2WaitMessageFlowSegment(getEndTime(), nextPubInstanceInterval.getStartTime(), getCallbackInstance(), nextPubInstance, getSubscription(), pubObject));
                }
            }
        }
    }

    private void getWaitPartialSyncSegmentsAfterSubCallback(@NonNull Ros2MessageFlowBuildInfo info, Collection<@NonNull Ros2MessageFlowSegment> next, @NonNull Ros2ObjectHandle pubHandle) {
        /**
         * Find the next publication for each publisher in the link. Find the
         * next subscription callback instance for the given subscription before
         * the next publication. Only create a wait link if there are no other
         * subscription callback instances before the next publication,
         * otherwise the message received in the current callback simply gets
         * overwritten in the cache.
         */
        long currentSubCallbackInstanceEndTime = getCallbackInstance().getCallbackInstance().getEndTime();
        // There is no link if there is no next publication
        ITmfStateInterval nextPubInstanceInterval = Ros2MessagesUtil.getNextPubInstanceInterval(info.getMessagesSs(), info.getObjectsSs(), currentSubCallbackInstanceEndTime + 1, pubHandle);
        if (null == nextPubInstanceInterval) {
            return;
        }
        Ros2PubInstance nextPubInstance = (Ros2PubInstance) nextPubInstanceInterval.getValue();
        if (null == nextPubInstance) {
            return;
        }
        ITmfStateInterval nextSubCallbackInstanceInterval = Ros2MessagesUtil.getNextSubCallbackInstanceInterval(info.getMessagesSs(), info.getObjectsSs(), currentSubCallbackInstanceEndTime + 1, getSubscription().getHandle());
        if (null == nextSubCallbackInstanceInterval || nextPubInstanceInterval.getStartTime() < nextSubCallbackInstanceInterval.getStartTime()) {
            Ros2PublisherObject pubObject = Ros2ObjectsUtil.getPublisherObjectFromHandle(info.getObjectsSs(), nextPubInstanceInterval.getStartTime(), pubHandle);
            if (null != pubObject) {
                next.add(new Ros2WaitMessageFlowSegment(getEndTime(), nextPubInstanceInterval.getStartTime(), getCallbackInstance(), nextPubInstance, getSubscription(), pubObject));
            }
        }
    }

    @Override
    public @NonNull Collection<@NonNull Ros2MessageFlowSegment> getDirectPredecessors(@NonNull Ros2MessageFlowBuildInfo info) {
        /**
         * Before a subscription callback segment, the next segment is a single
         * transport segment.
         */

        /**
         * Should only get a single link or no link here, but it's simpler to
         * just use a for loop.
         */
        Collection<@NonNull Ros2MessageFlowSegment> previous = new ArrayList<>();
        Iterator<@NonNull Ros2MessageTransportInstance> transportLinks = Ros2MessagesUtil.getIncomingTransportLinks(info.getMessagesSs(), getCallbackInstance().getTakeInstance().getStartTime(), getCallbackInstance().getSubscriptionHandle())
                .iterator();
        while (transportLinks.hasNext()) {
            Ros2MessageTransportInstance transportInstance = transportLinks.next();
            Long sourceTimestamp = transportInstance.getSourceTimestamp();
            Long destinationTimestamp = transportInstance.getDestinationTimestamp();
            Ros2PublisherObject pubObject = Ros2ObjectsUtil.getPublisherObjectFromHandle(info.getObjectsSs(), sourceTimestamp, transportInstance.getPublisherHandle());
            if (null != pubObject) {
                previous.add(new Ros2TransportMessageFlowSegment(sourceTimestamp, destinationTimestamp, transportInstance, pubObject, getSubscription()));
            }
        }
        return previous;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fCallbackInstance, fSubscription, super.hashCode());
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
        if (!(obj instanceof Ros2SubCallbackMessageFlowSegment)) {
            return false;
        }
        Ros2SubCallbackMessageFlowSegment o = (Ros2SubCallbackMessageFlowSegment) obj;
        if (!fCallbackInstance.equals(o.fCallbackInstance)) {
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
                "Ros2SubCallbackMessageFlowSegment: %s, callbackInstance=[%s], sub=[%s]", //$NON-NLS-1$
                super.toString(), fCallbackInstance.toString(), fSubscription.toString());
    }
}
