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
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2MessagesUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messagelinks.Ros2MessageCausalLink;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackPublicationInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2MessageTransportInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2SubCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2CallbackType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * Publication message flow segment, between initial publish call and DDS writer
 * call.
 *
 * @author Christophe Bedard
 */
public class Ros2PublicationMessageFlowSegment extends Ros2MessageFlowSegment {

    private final @NonNull Ros2PubInstance fPubInstance;
    private final @NonNull Ros2PublisherObject fPublisher;

    /**
     * Constructor
     *
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @param pubInstance
     *            the corresponding publication instance
     * @param publisher
     *            the publisher
     */
    public Ros2PublicationMessageFlowSegment(long startTime, long endTime, @NonNull Ros2PubInstance pubInstance, @NonNull Ros2PublisherObject publisher) {
        super(startTime, endTime, publisher.getNodeHandle());
        fPubInstance = pubInstance;
        fPublisher = publisher;
    }

    /**
     * @return the corresponding publication instance
     */
    public @NonNull Ros2PubInstance getPubInstance() {
        return fPubInstance;
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
         * After a publication segment, the next segments are transport
         * segments.
         */
        Collection<@NonNull Ros2MessageFlowSegment> next = new ArrayList<>();
        Iterator<@NonNull Ros2MessageTransportInstance> transportLinks = Ros2MessagesUtil.getOutgoingTransportLinks(info.getMessagesSs(), getEndTime() + 1, getPublisher().getHandle()).iterator();
        while (transportLinks.hasNext()) {
            Ros2MessageTransportInstance transportInstance = transportLinks.next();
            Long sourceTimestamp = transportInstance.getSourceTimestamp();
            Long destinationTimestamp = transportInstance.getDestinationTimestamp();
            Ros2SubscriptionObject subObject = Ros2ObjectsUtil.getSubscriptionObjectFromHandle(info.getObjectsSs(), destinationTimestamp, transportInstance.getSubscriptionHandle());
            if (null != subObject) {
                next.add(new Ros2TransportMessageFlowSegment(sourceTimestamp, destinationTimestamp, transportInstance, getPublisher(), subObject));
            }
        }
        return next;
    }

    @Override
    public @NonNull Collection<@NonNull Ros2MessageFlowSegment> getDirectPredecessors(@NonNull Ros2MessageFlowBuildInfo info) {
        /**
         * Before a publication segment, the previous segments are:
         *
         * <pre>
         *   * a single callback-publication segment;
         *   * wait segments.
         * </pre>
         */
        Collection<@NonNull Ros2MessageFlowSegment> previous = new ArrayList<>();

        previous.addAll(getCallbackPublicationSegmentsBeforePublication(info));
        previous.addAll(getWaitSegmentsBeforePublication(info));

        return previous;
    }

    private Collection<@NonNull Ros2MessageFlowSegment> getCallbackPublicationSegmentsBeforePublication(@NonNull Ros2MessageFlowBuildInfo info) {
        Collection<@NonNull Ros2MessageFlowSegment> previous = new ArrayList<>();
        /**
         * Callback-publication.
         *
         * Should only get a single link or no link here, but it's simpler to
         * just use a for loop.
         */
        long publicationTimestamp = getStartTime();
        Iterator<@NonNull Ros2CallbackPublicationInstance> callbackPublicationLinks = Ros2MessagesUtil.getIncomingCallbackPublicationLinks(info.getMessagesSs(), getPublisher().getHandle(), publicationTimestamp).iterator();
        while (callbackPublicationLinks.hasNext()) {
            Ros2CallbackPublicationInstance callbackPublicationInstance = callbackPublicationLinks.next();
            Ros2ObjectHandle callbackOwnerHandle = callbackPublicationInstance.getCallbackOwnerHandle();
            long time = callbackPublicationInstance.getPublicationTimestamp();
            Ros2CallbackType callbackType = callbackPublicationInstance.getCallbackType();
            if (callbackType.equals(Ros2CallbackType.SUBSCRIPTION)) {
                Ros2SubscriptionObject subObject = Ros2ObjectsUtil.getSubscriptionObjectFromHandle(info.getObjectsSs(), time, callbackOwnerHandle);
                if (null != subObject) {
                    previous.add(new Ros2CallbackPubMessageFlowSegment(time, time, callbackPublicationInstance, subObject, getPublisher()));
                }
            } else if (callbackType.equals(Ros2CallbackType.TIMER)) {
                Ros2TimerObject timerObject = Ros2ObjectsUtil.getTimerObjectFromHandle(info.getObjectsSs(), time, callbackOwnerHandle);
                if (null != timerObject) {
                    previous.add(new Ros2CallbackPubMessageFlowSegment(time, time, callbackPublicationInstance, timerObject, getPublisher()));
                }
            } else {
                throw new IllegalStateException();
            }
        }
        return previous;
    }

    private Collection<@NonNull Ros2MessageFlowSegment> getWaitSegmentsBeforePublication(@NonNull Ros2MessageFlowBuildInfo info) {
        Collection<@NonNull Ros2MessageFlowSegment> previous = new ArrayList<>();
        /**
         * Wait link.
         *
         * Just find the last subscription callback for each subscription in the
         * link.
         */
        for (Ros2MessageCausalLink link : info.getMessageLinksModel().getLinksForPub(getPublisher().getHandle())) {
            for (Ros2ObjectHandle subHandle : link.getSubs()) {
                switch (link.getType()) {
                case PERIODIC_ASYNC:
                    getWaitPeriodicAsyncSegmentsBeforePublication(info, previous, subHandle);
                    break;
                case PARTIAL_SYNC:
                    getWaitPartialSyncSegmentsBeforePublication(info, previous, subHandle);
                    break;
                default:
                    throw new IllegalStateException();
                }
            }
        }
        return previous;
    }

    private void getWaitPeriodicAsyncSegmentsBeforePublication(@NonNull Ros2MessageFlowBuildInfo info, Collection<@NonNull Ros2MessageFlowSegment> previous, @NonNull Ros2ObjectHandle subHandle) {
        ITmfStateInterval previousSubCallbackInstanceInterval = Ros2MessagesUtil.getPreviousSubCallbackInstanceInterval(info.getMessagesSs(), info.getObjectsSs(), getStartTime(), subHandle);
        if (null == previousSubCallbackInstanceInterval) {
            return;
        }
        Ros2SubCallbackInstance previousSubCallbackInstance = (Ros2SubCallbackInstance) previousSubCallbackInstanceInterval.getValue();
        if (null == previousSubCallbackInstance) {
            return;
        }
        Ros2SubscriptionObject subObject = Ros2ObjectsUtil.getSubscriptionObjectFromHandle(info.getObjectsSs(), previousSubCallbackInstanceInterval.getEndTime(), subHandle);
        if (null == subObject) {
            return;
        }
        previous.add(new Ros2WaitMessageFlowSegment(previousSubCallbackInstanceInterval.getEndTime(), getStartTime(), previousSubCallbackInstance, getPubInstance(), subObject, getPublisher()));
    }

    private void getWaitPartialSyncSegmentsBeforePublication(@NonNull Ros2MessageFlowBuildInfo info, Collection<@NonNull Ros2MessageFlowSegment> previous, @NonNull Ros2ObjectHandle subHandle) {
        ITmfStateInterval previousSubCallbackInstanceInterval = Ros2MessagesUtil.getPreviousSubCallbackInstanceInterval(info.getMessagesSs(), info.getObjectsSs(), getStartTime(), subHandle);
        if (null == previousSubCallbackInstanceInterval) {
            return;
        }
        Ros2SubCallbackInstance previousSubCallbackInstance = (Ros2SubCallbackInstance) previousSubCallbackInstanceInterval.getValue();
        if (null == previousSubCallbackInstance) {
            return;
        }
        Ros2SubscriptionObject subObject = Ros2ObjectsUtil.getSubscriptionObjectFromHandle(info.getObjectsSs(), previousSubCallbackInstanceInterval.getEndTime(), subHandle);
        if (null == subObject) {
            return;
        }

        /**
         * If we already added a callback-publication link segment that links
         * the publication segment to the same callback instance, there is no
         * need to add a wait
         */
        boolean isCallbackPubLink = !previous.stream().filter(s -> {
            if (!(s instanceof Ros2CallbackPubMessageFlowSegment)) {
                return false;
            }
            Ros2CallbackPubMessageFlowSegment segment = (Ros2CallbackPubMessageFlowSegment) s;
            if (!(segment.getSource() instanceof Ros2SubscriptionObject)) {
                return false;
            }
            Ros2SubscriptionObject linkSubObject = (Ros2SubscriptionObject) segment.getSource();
            if (!linkSubObject.equals(subObject)) {
                return false;
            }
            long linkTime = segment.getCallbackPubInstance().getPublicationTimestamp();
            Ros2CallbackInstance callbackInstance = previousSubCallbackInstance.getCallbackInstance();
            return linkTime >= callbackInstance.getStartTime() && linkTime <= callbackInstance.getEndTime();
        }).collect(Collectors.toUnmodifiableList()).isEmpty();
        if (isCallbackPubLink) {
            return;
        }

        previous.add(new Ros2WaitMessageFlowSegment(previousSubCallbackInstanceInterval.getEndTime(), getStartTime(), previousSubCallbackInstance, getPubInstance(), subObject, getPublisher()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(fPubInstance, fPublisher, super.hashCode());
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
        if (!(obj instanceof Ros2PublicationMessageFlowSegment)) {
            return false;
        }
        Ros2PublicationMessageFlowSegment o = (Ros2PublicationMessageFlowSegment) obj;
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
                "Ros2PublicationMessageFlowSegment: %s, pubInstance=[%s], pub=[%s]", //$NON-NLS-1$
                super.toString(), fPubInstance.toString(), fPublisher.toString());
    }
}
