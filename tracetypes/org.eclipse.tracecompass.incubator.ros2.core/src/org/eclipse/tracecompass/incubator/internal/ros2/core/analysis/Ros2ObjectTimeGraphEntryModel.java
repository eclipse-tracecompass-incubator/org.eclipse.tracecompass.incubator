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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis;

import java.text.DecimalFormat;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ClientObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2NodeObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ServiceObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;

import com.google.common.collect.Multimap;

/**
 * Time graph entry model that represents a trace or a ROS 2 object, like a
 * node, publisher, subscription, or timer.
 *
 * @author Christophe Bedard
 */
public class Ros2ObjectTimeGraphEntryModel extends TimeGraphEntryModel {

    /** Object type */
    public static final String KEY_OBJECT_TYPE = "object_type"; //$NON-NLS-1$
    /** Object */
    public static final String KEY_OBJECT = "object"; //$NON-NLS-1$

    private final @NonNull Ros2ObjectTimeGraphEntryModelType fType;
    private final @NonNull Object fObject;

    /**
     * Constructor
     *
     * @param id
     *            Entry ID
     * @param parentId
     *            Parent ID
     * @param startTime
     *            Start time
     * @param endTime
     *            End time
     * @param type
     *            the type
     * @param object
     *            the corresponding object
     */
    public Ros2ObjectTimeGraphEntryModel(long id, long parentId, long startTime, long endTime, @NonNull Ros2ObjectTimeGraphEntryModelType type, @NonNull Object object) {
        super(id, parentId, Objects.requireNonNull(getEntryModelName(type, object)), startTime, endTime, true);
        fType = type;
        fObject = object;
    }

    private static String getEntryModelName(Ros2ObjectTimeGraphEntryModelType type, Object object) {
        switch (type) {
        case TRACE:
            return ((Ros2NodeObject) object).getTraceName();
        case NODE:
            return ((Ros2NodeObject) object).getNodeName();
        case PUBLISHER:
            return ((Ros2PublisherObject) object).getTopicName();
        case SUBSCRIPTION:
            return ((Ros2SubscriptionObject) object).getTopicName();
        case CLIENT:
            return ((Ros2ClientObject) object).getTopicName();
        case SERVICE:
            return ((Ros2ServiceObject) object).getTopicName();
        case TIMER:
            return getTimerPeriodAsString((Ros2TimerObject) object);
        default:
            throw new IllegalStateException();
        }
    }

    private static String getTimerPeriodAsString(Ros2TimerObject timerObject) {
        DecimalFormat df = new DecimalFormat("0"); //$NON-NLS-1$
        df.setMaximumFractionDigits(10);
        return df.format(Long.valueOf(timerObject.getPeriod()) / 1000000.0) + " ms"; //$NON-NLS-1$
    }

    /**
     * @return the entry model type
     */
    public @NonNull Ros2ObjectTimeGraphEntryModelType getType() {
        return fType;
    }

    /**
     * @return whether the object wrapped by this entry model is a leaf object,
     *         i.e., a publisher, subscription, or timer
     */
    public boolean isLeafObject() {
        return fType.equals(Ros2ObjectTimeGraphEntryModelType.PUBLISHER) || fType.equals(Ros2ObjectTimeGraphEntryModelType.SUBSCRIPTION) || fType.equals(Ros2ObjectTimeGraphEntryModelType.TIMER);
    }

    /**
     * @return the corresponding object
     */
    public @NonNull Object getObject() {
        return fObject;
    }

    @Override
    public Multimap<@NonNull String, @NonNull Object> getMetadata() {
        Multimap<@NonNull String, @NonNull Object> metadata = super.getMetadata();
        metadata.put(KEY_OBJECT_TYPE, fType);
        metadata.put(KEY_OBJECT, fObject);
        return metadata;
    }

    @Override
    public int hashCode() {
        return com.google.common.base.Objects.hashCode(fType, fObject);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        Ros2ObjectTimeGraphEntryModel o = (Ros2ObjectTimeGraphEntryModel) obj;
        return fType.equals(o.fType) && fObject.equals(o.fObject);
    }
}
