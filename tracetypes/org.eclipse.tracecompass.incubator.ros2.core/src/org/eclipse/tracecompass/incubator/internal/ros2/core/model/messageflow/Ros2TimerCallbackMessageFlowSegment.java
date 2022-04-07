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
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2MessagesUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackPublicationInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;

/**
 * Timer callback message flow segment.
 *
 * @author Christophe Bedard
 */
public class Ros2TimerCallbackMessageFlowSegment extends Ros2MessageFlowSegment {

    private final @NonNull Ros2CallbackInstance fCallbackInstance;
    private final @NonNull Ros2TimerObject fTimer;

    /**
     * Constructor
     *
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @param callbackInstance
     *            the corresponding timer callback instance
     * @param timer
     *            the timer
     */
    public Ros2TimerCallbackMessageFlowSegment(long startTime, long endTime, @NonNull Ros2CallbackInstance callbackInstance, @NonNull Ros2TimerObject timer) {
        super(startTime, endTime, timer.getNodeHandle());
        fCallbackInstance = callbackInstance;
        fTimer = timer;
    }

    /**
     * @return the corresponding timer callback instance
     */
    public @NonNull Ros2CallbackInstance getCallbackInstance() {
        return fCallbackInstance;
    }

    /**
     * @return the timer
     */
    public @NonNull Ros2TimerObject getTimer() {
        return fTimer;
    }

    @Override
    public @NonNull Collection<@NonNull Ros2MessageFlowSegment> getDirectSuccessors(@NonNull Ros2MessageFlowBuildInfo info) {
        /**
         * After a timer callback segment, the next segments are
         * callback-publication segments (linking to a publication segment).
         */
        Collection<@NonNull Ros2MessageFlowSegment> next = new ArrayList<>();

        Iterator<@NonNull Ros2CallbackPublicationInstance> callbackPublicationLinks = Ros2MessagesUtil
                .getOutgoingCallbackPublicationLinks(info.getMessagesSs(), getCallbackInstance().getOwnerHandle(), getStartTime(), getEndTime()).iterator();
        while (callbackPublicationLinks.hasNext()) {
            Ros2CallbackPublicationInstance callbackPublicationInstance = callbackPublicationLinks.next();
            long time = callbackPublicationInstance.getPublicationTimestamp();
            Ros2TimerObject timerObject = Ros2ObjectsUtil.getTimerObjectFromHandle(info.getObjectsSs(), time, callbackPublicationInstance.getCallbackOwnerHandle());
            Ros2PublisherObject pubObject = Ros2ObjectsUtil.getPublisherObjectFromHandle(info.getObjectsSs(), time, callbackPublicationInstance.getPublisherHandle());
            if (null == timerObject || null == pubObject) {
                continue;
            }
            next.add(new Ros2CallbackPubMessageFlowSegment(time, time, callbackPublicationInstance, timerObject, pubObject));
        }
        return next;
    }

    @Override
    public @NonNull Collection<@NonNull Ros2MessageFlowSegment> getDirectPredecessors(@NonNull Ros2MessageFlowBuildInfo info) {
        /**
         * There is nothing before a timer callback, since they're only
         * triggered periodically by an actual timer.
         */
        return Collections.emptyList();
    }

    @Override
    public int hashCode() {
        return Objects.hash(fCallbackInstance, fTimer, super.hashCode());
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
        if (!(obj instanceof Ros2TimerCallbackMessageFlowSegment)) {
            return false;
        }
        Ros2TimerCallbackMessageFlowSegment o = (Ros2TimerCallbackMessageFlowSegment) obj;
        if (!fCallbackInstance.equals(o.fCallbackInstance)) {
            return false;
        }
        if (!fTimer.equals(o.fTimer)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format(
                "Ros2TimerCallbackMessageFlowSegment: %s, callbackInstance=[%s], timer=[%s]", //$NON-NLS-1$
                super.toString(), fCallbackInstance.toString(), fTimer.toString());
    }
}
