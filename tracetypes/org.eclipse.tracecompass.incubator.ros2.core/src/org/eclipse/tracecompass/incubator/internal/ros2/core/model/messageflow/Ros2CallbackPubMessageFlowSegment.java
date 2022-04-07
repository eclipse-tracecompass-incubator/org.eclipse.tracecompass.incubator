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
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackPublicationInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2SubCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2TimerCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2CallbackType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2Object;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * Callback-publication message flow segment, between callback instance and
 * publication instance.
 *
 * @author Christophe Bedard
 */
public class Ros2CallbackPubMessageFlowSegment extends Ros2MessageFlowSegment {

    private final @NonNull Ros2CallbackPublicationInstance fCallbackPubInstance;
    private final @NonNull Ros2Object<@NonNull Ros2ObjectHandle> fSource;
    private final @NonNull Ros2PublisherObject fPublisher;

    /**
     * Constructor
     *
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @param callbackPubInstance
     *            the corresponding callback-publication instance
     * @param source
     *            the source subscription or timer object
     * @param publisher
     *            the publisher
     */
    public Ros2CallbackPubMessageFlowSegment(long startTime, long endTime, @NonNull Ros2CallbackPublicationInstance callbackPubInstance, @NonNull Ros2Object<@NonNull Ros2ObjectHandle> source, @NonNull Ros2PublisherObject publisher) {
        super(startTime, endTime, null);
        fCallbackPubInstance = callbackPubInstance;
        fSource = source;
        fPublisher = publisher;
    }

    /**
     * @return the corresponding callback-publication instance
     */
    public @NonNull Ros2CallbackPublicationInstance getCallbackPubInstance() {
        return fCallbackPubInstance;
    }

    /**
     * @return the source subscription or timer object
     */
    public @NonNull Ros2Object<@NonNull Ros2ObjectHandle> getSource() {
        return fSource;
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
         * After a publication link, the next segment is a single publication
         * segment.
         */
        ITmfStateInterval pubInstanceInterval = Ros2MessagesUtil.getPubInstanceInterval(info.getMessagesSs(), info.getObjectsSs(), getCallbackPubInstance().getPublicationTimestamp(), getCallbackPubInstance().getPublisherHandle());
        if (null == pubInstanceInterval) {
            return Collections.emptyList();
        }

        long startTime = pubInstanceInterval.getStartTime();
        long endTime = pubInstanceInterval.getEndTime();
        Ros2PubInstance pubInstance = (Ros2PubInstance) pubInstanceInterval.getValue();
        if (null == pubInstance) {
            return Collections.emptyList();
        }
        Ros2PublisherObject pubObject = Ros2ObjectsUtil.getPublisherObjectFromHandle(info.getObjectsSs(), startTime, pubInstance.getPublisherHandle());
        if (null == pubObject) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new Ros2PublicationMessageFlowSegment(startTime, endTime, pubInstance, pubObject));
    }

    @Override
    public @NonNull Collection<@NonNull Ros2MessageFlowSegment> getDirectPredecessors(@NonNull Ros2MessageFlowBuildInfo info) {
        /**
         * Before a callback-publication segment, the previous segment is a
         * single timer or subscription callback segment.
         */
        long time = getCallbackPubInstance().getPublicationTimestamp();
        Ros2CallbackType callbackType = getCallbackPubInstance().getCallbackType();
        // Depending on the callback type, get the callback instance
        if (callbackType.equals(Ros2CallbackType.SUBSCRIPTION)) {
            Ros2SubscriptionObject subObject = (Ros2SubscriptionObject) getSource();
            ITmfStateInterval subCallbackInstanceInterval = Ros2MessagesUtil.getSubCallbackInstanceInterval(info.getMessagesSs(), info.getObjectsSs(), time, subObject.getHandle());
            if (null == subCallbackInstanceInterval) {
                return Collections.emptyList();
            }
            long startTime = subCallbackInstanceInterval.getStartTime();
            long endTime = subCallbackInstanceInterval.getEndTime();
            Ros2SubCallbackInstance subCallbackInstance = (Ros2SubCallbackInstance) subCallbackInstanceInterval.getValue();
            if (null == subCallbackInstance) {
                return Collections.emptyList();
            }
            return Collections.singletonList(new Ros2SubCallbackMessageFlowSegment(startTime, endTime, subCallbackInstance, subObject));
        } else if (callbackType.equals(Ros2CallbackType.TIMER)) {
            Ros2TimerObject timerObject = (Ros2TimerObject) getSource();
            ITmfStateInterval timerCallbackInstanceInterval = Ros2MessagesUtil.getTimerCallbackInstanceInterval(info.getMessagesSs(), info.getObjectsSs(), time, timerObject.getHandle());
            if (null == timerCallbackInstanceInterval) {
                return Collections.emptyList();
            }
            long startTime = timerCallbackInstanceInterval.getStartTime();
            long endTime = timerCallbackInstanceInterval.getEndTime();
            Ros2TimerCallbackInstance timerCallbackInstance = (Ros2TimerCallbackInstance) timerCallbackInstanceInterval.getValue();
            if (null == timerCallbackInstance) {
                return Collections.emptyList();
            }
            Ros2CallbackInstance callbackInstance = timerCallbackInstance.getCallbackInstance();
            return Collections.singletonList(new Ros2TimerCallbackMessageFlowSegment(startTime, endTime, callbackInstance, timerObject));
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(fCallbackPubInstance, fSource, fPublisher, super.hashCode());
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
        if (!(obj instanceof Ros2CallbackPubMessageFlowSegment)) {
            return false;
        }
        Ros2CallbackPubMessageFlowSegment o = (Ros2CallbackPubMessageFlowSegment) obj;
        if (!fCallbackPubInstance.equals(o.fCallbackPubInstance)) {
            return false;
        }
        if (!fSource.equals(o.fSource)) {
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
                "Ros2CallbackPubMessageFlowSegment: %s, callbackPubInstance=[%s], source=[%s], pub=[%s]", //$NON-NLS-1$
                super.toString(), fCallbackPubInstance.toString(), fSource.toString(), fPublisher.toString());
    }
}
