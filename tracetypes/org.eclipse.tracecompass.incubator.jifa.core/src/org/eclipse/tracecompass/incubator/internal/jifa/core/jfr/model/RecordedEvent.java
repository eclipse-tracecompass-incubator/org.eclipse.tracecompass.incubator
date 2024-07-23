/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IScalarAffineTransform;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.LabeledIdentifier;

public class RecordedEvent {
    private static final long NANOS_PER_SECOND = 1000_000_000L;

    private final IItem item;

    private long fStartTime;
    private long fEndTime = -1;
    private RecordedStackTrace stackTrace;
    private RecordedThread thread;
    /**
     * @return the thread
     */
    public RecordedThread getThread() {
        return thread;
    }

    /**
     * @param thread the thread to set
     */
    public void setThread(RecordedThread thread) {
        this.thread = thread;
    }

    /**
     * @return the eventType
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * @param eventType the eventType to set
     */
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    /**
     * @return the activeSetting
     */
    public ActiveSetting getActiveSetting() {
        return activeSetting;
    }

    /**
     * @param activeSetting the activeSetting to set
     */
    public void setActiveSetting(ActiveSetting activeSetting) {
        this.activeSetting = activeSetting;
    }

    private EventType eventType;
    private ActiveSetting activeSetting = null;

    public static RecordedEvent newInstance(IItem item, SymbolTable<SymbolBase> symbols) {
        RecordedEvent event = new RecordedEvent(item);
        event.init(symbols);
        return event;
    }

    private RecordedEvent(IItem item) {
        this.item = item;
    }

    private void init(SymbolTable<SymbolBase> symbols) {
        IMCThread imcThread = getValue("eventThread");
        if (imcThread == null) {
            imcThread = getValue("sampledThread");
        }

        if (imcThread != null) {
            thread = new RecordedThread(imcThread);
        }

        Object value = getValue("startTime");
        if (value instanceof IQuantity) {
            IQuantity v = (IQuantity) value;
            fStartTime = toNanos(v, UnitLookup.EPOCH_NS);
        }

        IType<IItem> itemType = ItemToolkit.getItemType(item);
        String itemTypeId = itemType.getIdentifier();

        // fix for JDK Mission Control lib
        if ((itemTypeId.startsWith(EventConstant.EXECUTION_SAMPLE) && !itemTypeId.equals(EventConstant.EXECUTION_SAMPLE))) {
            itemTypeId = EventConstant.EXECUTION_SAMPLE;
        } else if (itemTypeId.startsWith(EventConstant.OBJECT_ALLOCATION_OUTSIDE_TLAB)
                && !itemTypeId.equals(EventConstant.OBJECT_ALLOCATION_OUTSIDE_TLAB)) {
            itemTypeId = EventConstant.OBJECT_ALLOCATION_OUTSIDE_TLAB;
        } else if (itemTypeId.startsWith(EventConstant.OBJECT_ALLOCATION_IN_NEW_TLAB)
                && !itemTypeId.equals(EventConstant.OBJECT_ALLOCATION_IN_NEW_TLAB)) {
            itemTypeId = EventConstant.OBJECT_ALLOCATION_IN_NEW_TLAB;
        }

        this.eventType = new EventType(itemTypeId);

        IMCStackTrace s = getValue("stackTrace");
        if (s != null) {
            List<? extends IMCFrame> frames = s.getFrames();
            RecordedStackTrace st = new RecordedStackTrace();
            List<RecordedFrame> list = new ArrayList<>();
            frames.forEach(frame -> {
                IMCMethod method = frame.getMethod();

                RecordedMethod m = new RecordedMethod();
                m.setDescriptor(method.getFormalDescriptor());
                m.setModifiers(method.getModifier() == null ? 0 : method.getModifier());

                IMCType type = method.getType();
                RecordedClass c = new RecordedClass();
                c.setName(type.getTypeName());
                c.setPackageName(type.getPackage().getName());
                if (symbols.isContains(c)) {
                    c = (RecordedClass) symbols.get(c);
                } else {
                    symbols.put(c);
                }
                m.setType(c);
                m.setName(method.getMethodName());
                if (symbols.isContains(m)) {
                    m = (RecordedMethod) symbols.get(m);
                } else {
                    symbols.put(m);
                }

                RecordedFrame f = new RecordedFrame();
                f.setMethod(m);
                f.setBytecodeIndex(frame.getBCI());
                f.setType(frame.getType().getName());

                if (symbols.isContains(f)) {
                    f = (RecordedFrame) symbols.get(f);
                } else {
                    symbols.put(f);
                }

                list.add(f);
            });
            st.setFrames(list);
            if (symbols.isContains(st)) {
                st = (RecordedStackTrace) symbols.get(st);
            } else {
                symbols.put(st);
            }
            stackTrace = st;
        }

        if (EventConstant.ACTIVE_SETTING.equals(itemType.getIdentifier())) {
            String eventName = null;
            long eventId = -1;
            String settingName = null;
            for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> entry : itemType.getAccessorKeys().entrySet()) {
                if (entry.getKey().getIdentifier().equals("settingFor")) {
                    IMemberAccessor<?, IItem> accessor = itemType.getAccessor(entry.getKey());
                    LabeledIdentifier id = Objects.requireNonNull((LabeledIdentifier) accessor.getMember(item));
                    eventName = id.getInterfaceId();
                    eventId = id.getImplementationId();
                    continue;
                }
                if (entry.getKey().getIdentifier().equals("name")) {
                    IMemberAccessor<?, IItem> accessor = itemType.getAccessor(entry.getKey());
                    settingName = (String) accessor.getMember(item);
                }
                if (eventName != null && settingName != null && eventId >= 0) {
                    break;
                }
            }
            if (eventName != null && settingName != null && eventId >= 0) {
                this.activeSetting = new ActiveSetting(eventName, eventId, settingName);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public final <T> T getValue(String name) {
        IType<IItem> itemType = ItemToolkit.getItemType(item);
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> entry : itemType.getAccessorKeys().entrySet()) {
            IMemberAccessor<?, IItem> accessor = itemType.getAccessor(entry.getKey());
            if (entry.getKey().getIdentifier().equals(name)) {
                return (T) accessor.getMember(item);
            }
        }
        return null;
    }

    public Duration getDuration() {
        return Duration.ofNanos(getDurationNano());
    }

    public long getDurationNano() {
        return getEndTimeNanos() - fStartTime;
    }

    public String getString(String name) {
        return getValue(name);
    }

    public int getInt(String name) {
        Number n = getValue(name);
        if (n != null) {
            return n.intValue();
        }
        return 0;
    }

    public float getFloat(String name) {
        Number n = getValue(name);
        if (n != null) {
            return n.floatValue();
        }
        return 0;
    }

    public long getLong(String name) {
        Number n = getValue(name);
        if (n != null) {
            return n.longValue();
        }
        return 0;
    }

    public RecordedThread getThread(String key) {
        IMCThread imcThread = getValue(key);
        return imcThread == null ? null : new RecordedThread(imcThread);
    }

    public Instant getStartTime() {
        return Instant.ofEpochSecond(fStartTime / NANOS_PER_SECOND, fStartTime % NANOS_PER_SECOND);
    }

    public Instant getEndTime() {
        long endTime = getEndTimeNanos();
        return Instant.ofEpochSecond(endTime / NANOS_PER_SECOND, endTime % NANOS_PER_SECOND);
    }

    public long getStartTimeNanos() {
        return fStartTime;
    }

    private long getEndTimeNanos() {
        if (fEndTime < 0) {
            Object value = getValue("duration");
            if (value instanceof IQuantity) {
                fEndTime = fStartTime + toNanos((IQuantity) value, UnitLookup.NANOSECOND);
            } else {
                throw new RuntimeException("should not reach here");
            }
        }

        return fEndTime;
    }

    private static long toNanos(IQuantity value, IUnit targetUnit) {
        IScalarAffineTransform t = value.getUnit().valueTransformTo(targetUnit);
        return t.targetValue(value.longValue());
    }

    /**
     * @return the stackTrace
     */
    public RecordedStackTrace getStackTrace() {
        return stackTrace;
    }
}