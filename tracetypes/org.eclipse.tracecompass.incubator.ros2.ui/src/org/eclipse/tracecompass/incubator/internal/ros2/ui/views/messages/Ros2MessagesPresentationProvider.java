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

package org.eclipse.tracecompass.incubator.internal.ros2.ui.views.messages;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.Ros2ObjectTimeGraphEntryModel;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.Ros2ObjectTimeGraphEntryModelType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2CallbackTimeGraphState;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2MessagesDataProvider.ArrowType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2NodeTimeGraphState;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2PubTimeGraphState;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2TakeTimeGraphState;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2TakeInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2NodeObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.presentation.IPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.presentation.RotatingPaletteProvider;
import org.eclipse.tracecompass.tmf.ui.colors.RGBAUtil;
import org.eclipse.tracecompass.tmf.ui.views.FormatTimeUtils;
import org.eclipse.tracecompass.tmf.ui.views.FormatTimeUtils.Resolution;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * ROS 2 Messages presentation provider.
 *
 * @author Christophe Bedard
 */
public class Ros2MessagesPresentationProvider extends TimeGraphPresentationProvider {

    private static final int NUM_STATES_NON_COLORS = 2;
    private static final int NUM_STATES_COLORS = 360;
    private static final StateItem[] STATE_TABLE;
    private static final IPaletteProvider PALETTE = new RotatingPaletteProvider.Builder().setNbColors(NUM_STATES_COLORS).build();
    private static final HashFunction fHasher = Hashing.sha512();

    static {
        // First NUM_STATES_NON_COLORS elements are states for arrows
        // Other elements are colors
        STATE_TABLE = new StateItem[NUM_STATES_NON_COLORS + 3 * NUM_STATES_COLORS];
        STATE_TABLE[0] = new StateItem(State.ARROW_TRANSPORT.rgb, State.ARROW_TRANSPORT.toString());
        STATE_TABLE[1] = new StateItem(State.ARROW_PUB.rgb, State.ARROW_PUB.toString());
        int i = 0;
        for (RGBAColor color : PALETTE.get()) {
            STATE_TABLE[NUM_STATES_NON_COLORS + i] = new StateItem(RGBAUtil.fromRGBAColor(color).rgb, color.toString());
            // Same color but lower height for take instances
            STATE_TABLE[NUM_STATES_NON_COLORS + NUM_STATES_COLORS + i] = new StateItem(RGBAUtil.fromRGBAColor(color).rgb, color.toString());
            STATE_TABLE[NUM_STATES_NON_COLORS + NUM_STATES_COLORS + i].getStyleMap().put(StyleProperties.HEIGHT, 0.7f);
            // Same color but even lower height for nodes
            STATE_TABLE[NUM_STATES_NON_COLORS + 2 * NUM_STATES_COLORS + i] = new StateItem(RGBAUtil.fromRGBAColor(color).rgb, color.toString());
            STATE_TABLE[NUM_STATES_NON_COLORS + 2 * NUM_STATES_COLORS + i].getStyleMap().put(StyleProperties.HEIGHT, 0.4f);
            i++;
        }
    }

    private enum State {
        ARROW_TRANSPORT(new RGB(0x26, 0x26, 0x26)), ARROW_PUB(new RGB(0x80, 0x80, 0x80));

        private final RGB rgb;

        private State(RGB rgb) {
            this.rgb = rgb;
        }
    }

    /** The event cache */
    protected final LoadingCache<NamedTimeEvent, Optional<String>> fTimeEventNames = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(new CacheLoader<NamedTimeEvent, Optional<String>>() {
                @SuppressWarnings("null")
                @Override
                public Optional<String> load(NamedTimeEvent event) {
                    return Optional.ofNullable(event.getLabel());
                }
            });

    /**
     * Constructor
     */
    public Ros2MessagesPresentationProvider() {
        super(StringUtils.EMPTY);
    }

    @Override
    public StateItem[] getStateTable() {
        return STATE_TABLE;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof TimeEvent) {
            TimeEvent timeEvent = (TimeEvent) event;
            if (timeEvent.getValue() == ArrowType.TRANSPORT.getId()) {
                return State.ARROW_TRANSPORT.ordinal();
            } else if (timeEvent.getValue() == ArrowType.CALLBACK_PUB.getId()) {
                return State.ARROW_PUB.ordinal();
            }
        }
        if (event instanceof NamedTimeEvent) {
            TimeGraphEntry entry = (TimeGraphEntry) event.getEntry();
            ITmfTreeDataModel model = entry.getEntryModel();
            if (model instanceof Ros2ObjectTimeGraphEntryModel) {
                return getObjectEntryModelIndex(event, model);
            }
        }
        return INVISIBLE;
    }

    private static int getObjectEntryModelIndex(ITimeEvent event, ITmfTreeDataModel model) {
        NamedTimeEvent namedEvent = (NamedTimeEvent) event;
        long hash = namedEvent.getLabel().hashCode();

        Ros2ObjectTimeGraphEntryModel messagesModel = (Ros2ObjectTimeGraphEntryModel) model;
        Ros2ObjectTimeGraphEntryModelType type = messagesModel.getType();
        boolean isTakeState = false;
        boolean isNodeState = false;

        /**
         * For nodes, publishers, and subscriptions, base color on node/topic
         * name. For timers, base color on period.
         */
        if (Ros2ObjectTimeGraphEntryModelType.NODE == type) {
            Ros2NodeObject nodeObject = (Ros2NodeObject) messagesModel.getObject();
            hash = fHasher.hashUnencodedChars(nodeObject.getNodeName()).asLong();
            isNodeState = true;
        } else if (Ros2ObjectTimeGraphEntryModelType.PUBLISHER == type) {
            Ros2PublisherObject publisherObject = (Ros2PublisherObject) messagesModel.getObject();
            hash = fHasher.hashUnencodedChars(publisherObject.getTopicName()).asLong();
        } else if (Ros2ObjectTimeGraphEntryModelType.SUBSCRIPTION == type) {
            Ros2SubscriptionObject subscriptionObject = (Ros2SubscriptionObject) messagesModel.getObject();
            hash = fHasher.hashUnencodedChars(subscriptionObject.getTopicName()).asLong();

            Iterator<@NonNull Object> stateDataIterator = event.getMetadata().get(Ros2TakeTimeGraphState.KEY_DATA).iterator();
            isTakeState = stateDataIterator.hasNext() && stateDataIterator.next() instanceof Ros2TakeInstance;
        } else if (Ros2ObjectTimeGraphEntryModelType.TIMER == type) {
            /**
             * Hashing the period as a string seems to result in fewer color
             * collisions.
             */
            Ros2TimerObject timerObject = (Ros2TimerObject) messagesModel.getObject();
            hash = fHasher.hashUnencodedChars(Long.toString(timerObject.getPeriod())).asLong();
        }

        int hashFloor = Math.floorMod(hash, PALETTE.get().size());
        int stateTypeOffset = isTakeState || isNodeState ? (isNodeState ? 2 : 1) * NUM_STATES_COLORS : 0;
        return NUM_STATES_NON_COLORS + hashFloor + stateTypeOffset;
    }

    @Override
    public @Nullable String getEventName(ITimeEvent event) {
        ITimeGraphEntry entry = event.getEntry();
        if (!(entry instanceof TimeGraphEntry)) {
            return null;
        }

        ITmfTreeDataModel model = ((TimeGraphEntry) entry).getEntryModel();
        if (!(model instanceof Ros2ObjectTimeGraphEntryModel)) {
            return null;
        }

        Ros2ObjectTimeGraphEntryModel messagesModel = (Ros2ObjectTimeGraphEntryModel) model;
        Ros2ObjectTimeGraphEntryModelType type = messagesModel.getType();
        if (Ros2ObjectTimeGraphEntryModelType.NODE == type) {
            return "node lifetime"; //$NON-NLS-1$
        } else if (Ros2ObjectTimeGraphEntryModelType.PUBLISHER == type) {
            return "message publication"; //$NON-NLS-1$
        } else if (Ros2ObjectTimeGraphEntryModelType.SUBSCRIPTION == type) {
            return "subscription callback/take"; //$NON-NLS-1$
        } else if (Ros2ObjectTimeGraphEntryModelType.TIMER == type) {
            return "timer callback"; //$NON-NLS-1$
        }

        return null;
    }

    @Override
    public String getStateTypeName(ITimeGraphEntry entry) {
        if (!(entry instanceof TimeGraphEntry)) {
            return null;
        }

        ITmfTreeDataModel model = ((TimeGraphEntry) entry).getEntryModel();
        if (!(model instanceof Ros2ObjectTimeGraphEntryModel)) {
            return null;
        }

        Ros2ObjectTimeGraphEntryModel messagesModel = (Ros2ObjectTimeGraphEntryModel) model;
        Ros2ObjectTimeGraphEntryModelType type = messagesModel.getType();
        if (Ros2ObjectTimeGraphEntryModelType.NODE == type) {
            return "Node name"; //$NON-NLS-1$
        } else if (Ros2ObjectTimeGraphEntryModelType.PUBLISHER == type) {
            return "Publisher topic"; //$NON-NLS-1$
        } else if (Ros2ObjectTimeGraphEntryModelType.SUBSCRIPTION == type) {
            return "Subscription topic"; //$NON-NLS-1$
        } else if (Ros2ObjectTimeGraphEntryModelType.TIMER == type) {
            return "Timer period"; //$NON-NLS-1$
        }
        return null;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();

        ITimeGraphEntry entry = event.getEntry();
        if (!(entry instanceof TimeGraphEntry)) {
            return builder.build();
        }

        ITmfTreeDataModel model = ((TimeGraphEntry) entry).getEntryModel();
        if (!(model instanceof Ros2ObjectTimeGraphEntryModel)) {
            return builder.build();
        }

        Multimap<@NonNull String, @NonNull Object> metadata = event.getMetadata();
        Ros2ObjectTimeGraphEntryModel messagesModel = (Ros2ObjectTimeGraphEntryModel) model;
        Ros2ObjectTimeGraphEntryModelType type = messagesModel.getType();
        if (Ros2ObjectTimeGraphEntryModelType.NODE == type) {
            Ros2NodeObject nodeObject = (Ros2NodeObject) metadata.get(Ros2NodeTimeGraphState.KEY_DATA).iterator().next();
            builder.put("PID", Long.toString(nodeObject.getHandle().getPid())); //$NON-NLS-1$
        } else if (Ros2ObjectTimeGraphEntryModelType.PUBLISHER == type) {
            Ros2PubInstance pub = (Ros2PubInstance) metadata.get(Ros2PubTimeGraphState.KEY_DATA).iterator().next();
            builder.put("Message pointer", toHex(pub.getMessage().getPointer())); //$NON-NLS-1$
            builder.put("Source timestamp", FormatTimeUtils.formatTimeAbs(pub.getSourceTimestamp(), Resolution.NANOSEC)); //$NON-NLS-1$
            builder.put("PID", Long.toString(pub.getPublisherHandle().getPid())); //$NON-NLS-1$
            builder.put("TID", Long.toString(pub.getTid())); //$NON-NLS-1$
        } else if (Ros2ObjectTimeGraphEntryModelType.SUBSCRIPTION == type) {
            Object takeOrCallback = metadata.get(Ros2TakeTimeGraphState.KEY_DATA).iterator().next();
            if (takeOrCallback instanceof Ros2TakeInstance) {
                Ros2TakeInstance take = (Ros2TakeInstance) takeOrCallback;
                builder.put("Message pointer", toHex(take.getMessage().getPointer())); //$NON-NLS-1$
                builder.put("Source timestamp", FormatTimeUtils.formatTimeAbs(take.getSourceTimestamp(), Resolution.NANOSEC)); //$NON-NLS-1$
                builder.put("PID", Long.toString(take.getSubscriptionHandle().getPid())); //$NON-NLS-1$
                builder.put("TID", Long.toString(take.getTid())); //$NON-NLS-1$
            } else if (takeOrCallback instanceof Ros2CallbackInstance) {
                Ros2CallbackInstance callback = (Ros2CallbackInstance) takeOrCallback;
                builder.put("Intra-process", Boolean.toString(callback.isIntraProcess())); //$NON-NLS-1$
                builder.put("PID", Long.toString(callback.getOwnerHandle().getPid())); //$NON-NLS-1$
                builder.put("TID", Long.toString(callback.getTid())); //$NON-NLS-1$
            }
        } else if (Ros2ObjectTimeGraphEntryModelType.TIMER == type) {
            Ros2CallbackInstance callback = (Ros2CallbackInstance) metadata.get(Ros2CallbackTimeGraphState.KEY_DATA).iterator().next();
            builder.put("Intra-process", Boolean.toString(callback.isIntraProcess())); //$NON-NLS-1$
            builder.put("PID", Long.toString(callback.getOwnerHandle().getPid())); //$NON-NLS-1$
            builder.put("TID", Long.toString(callback.getTid())); //$NON-NLS-1$
        }

        return builder.build();
    }

    private static String toHex(long handle) {
        return "0x" + Long.toHexString(handle); //$NON-NLS-1$
    }
}
