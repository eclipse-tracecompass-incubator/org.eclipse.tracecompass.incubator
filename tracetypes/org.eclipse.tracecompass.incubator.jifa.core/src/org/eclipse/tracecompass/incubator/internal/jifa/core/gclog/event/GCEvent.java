/********************************************************************************
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event;

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.KB2MB;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.GlobalDiagnoseInfo;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.CpuTime;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCCause;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCEventBooleanType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCEventLevel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCMemoryItem;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCPause;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.ReferenceGC;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.vo.GCEventVO;

/**
 * Garbage Collection event. Phases
 *
 * All info saved here should not be relevant to AnalysisConfig. Anything
 * related to the config should be saved in EventDiagnoseInfo
 */
public class GCEvent extends TimedEvent {
    private int gcid = Constant.UNKNOWN_INT;

    private CpuTime cpuTime;
    private ReferenceGC referenceGC;

    private GCEventType eventType = GCEventType.UNDEFINED;
    private GCCause cause;

    private GCMemoryItem[] memory;

    // phases may contain more detailed info about an event. For simplicity, we put subphase of an event as
    // a direct child in phases field rather than child of child. Use level field of EventType to check
    // if an event type should be a subphase.
    private List<GCEvent> phases;
    private BitSet booleans;

    private double pause = Constant.UNKNOWN_DOUBLE;
    private double interval = Constant.UNKNOWN_DOUBLE; // interval from last event with same type
    private double causeInterval = Constant.UNKNOWN_DOUBLE; // interval from last event with same type and cause

    private long promotion = Constant.UNKNOWN_INT;
    private long allocation = Constant.UNKNOWN_INT;
    private long reclamation = Constant.UNKNOWN_INT;

    public GCMemoryItem[] getMemoryItems() {
        return memory;
    }

    public GCMemoryItem getMemoryItem(MemoryArea area) {
        if (memory == null) {
            return null;
        }
        return memory[area.ordinal()];
    }

    public GCMemoryItem getMemoryItemOrEmptyObject(MemoryArea area) {
        GCMemoryItem result = getMemoryItem(area);
        if (result == null) {
            return new GCMemoryItem(area);
        }
        return result;
    }

    public void setMemoryItem(GCMemoryItem item) {
        setMemoryItem(item, false);
    }

    public void setMemoryItem(GCMemoryItem item, boolean force) {
        if (item == null || item.isEmpty()) {
            return;
        }
        if (memory == null) {
            memory = new GCMemoryItem[MemoryArea.values().length];
        }
        if (force || getMemoryItem(item.getArea()) == null) {
            memory[item.getArea().ordinal()] = item;
        }
    }

    public void setMemoryItems(GCMemoryItem[] memory) {
        this.memory = memory;
    }

    public List<GCEvent> getPhases() {
        return phases;
    }

    public GCEvent() {
        // do nothing
    }

    public void setGcid(int gcid) {
        this.gcid = gcid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public GCEvent getLastPhaseOfType(GCEventType type) {
        if (phases == null) {
            return null;
        }
        for (int i = phases.size() - 1; i >= 0; i--) {
            GCEvent phase = phases.get(i);
            if (phase.getEventType().equals(type)) {
                return phase;
            }
        }
        return null;
    }

    public boolean containPhase(GCEventType type) {
        return getLastPhaseOfType(type) != null;
    }

    public void setBoolean(GCEventBooleanType type, boolean value) {
        if (booleans == null) {
            booleans = new BitSet();
        }
        booleans.set(type.ordinal(), value);
    }

    public void setTrue(GCEventBooleanType type) {
        setBoolean(type, true);
    }

    public boolean isTrue(GCEventBooleanType type) {
        return booleans != null && booleans.get(type.ordinal());
    }

    public ReferenceGC getReferenceGC() {
        return referenceGC;
    }

    public void setReferenceGC(ReferenceGC referenceGC) {
        this.referenceGC = referenceGC;
    }

    public int getGcid() {
        return gcid;
    }

    public GCEventType getEventType() {
        return eventType;
    }

    public boolean isYoungGC() {
        return this.eventType != null && this.eventType.isYoungGC();
    }

    public boolean isOldGC() {
        return this.eventType != null && this.eventType.isOldGC();
    }

    public boolean isFullGC() {
        return this.eventType != null && this.eventType.isFullGC();
    }

    public GCCause getCause() {
        return cause;
    }

    public CpuTime getCpuTime() {
        return cpuTime;
    }

    public long getAllocation() {
        return allocation;
    }

    public long getReclamation() {
        return reclamation;
    }

    public void setPromotion(long promotion) {
        if (promotion < 0) {
            this.promotion = Constant.UNKNOWN_INT;
        } else {
            this.promotion = promotion;
        }
    }

    public void setAllocation(long allocation) {
        if (allocation < 0) {
            this.allocation = Constant.UNKNOWN_INT;
        } else {
            this.allocation = allocation;
        }
    }

    public void setReclamation(long reclamation) {
        if (reclamation < 0) {
            this.reclamation = Constant.UNKNOWN_INT;
        } else {
            this.reclamation = reclamation;
        }
    }

    public void setEventType(GCEventType eventType) {
        this.eventType = eventType;
    }

    public void setCause(String cause) {
        this.cause = GCCause.getCause(cause);
    }

    public void setCause(GCCause cause) {
        this.cause = cause;
    }

    public void setCpuTime(CpuTime cpuTime) {
        this.cpuTime = cpuTime;
    }

    public boolean hasPhases() {
        return phases != null;
    }

    public void addPhase(GCEvent phase) {
        if (phases == null) {
            phases = new ArrayList<>(2);
        }
        phases.add(phase);
    }

    public void setPhases(List<GCEvent> phases) {
        this.phases = phases;
    }

    public boolean hasPromotion() {
        return eventType != null && eventType.hasObjectPromotion();
    }

    public double getInterval() {
        return interval;
    }

    public void setInterval(double interval) {
        this.interval = interval;
    }

    public double getCauseInterval() {
        return causeInterval;
    }

    public void setCauseInterval(double causeInterval) {
        this.causeInterval = causeInterval;
    }

    public boolean isPause() {
        return getEventType().getPause() == GCPause.PAUSE;
    }

    public boolean isPartialConcurrent() {
        return getEventType().getPause() == GCPause.PARTIAL;
    }

    public boolean isConcurrent() {
        return getEventType().getPause() == GCPause.CONCURRENT;
    }

    public boolean isBadFullGC() {
        return isFullGC() && getCause().isBad();
    }

    private static final double GCTRACETIME_TRACECPUTIME_CLOSE_THRESHOLD = 10.0;

    public double getPause() {
        if (pause == Constant.UNKNOWN_DOUBLE) {
            switch (eventType.getPause()) {
                case PAUSE:
                    // In most cases, duration is more accurate than cputime because of rounding error.
                    // In very rare cases, cputime may be significantly larger than duration. In these cases
                    // cputime is more accurate value.
                    if (cpuTime != null) {
                        if (getDuration() != Constant.UNKNOWN_DOUBLE &&
                                Math.abs(cpuTime.getReal() - getDuration()) > GCTRACETIME_TRACECPUTIME_CLOSE_THRESHOLD) {
                            pause = getCpuTime().getReal();
                        } else {
                            pause = getDuration();
                        }
                    } else {
                        pause = getDuration();
                    }
                    break;
                case CONCURRENT:
                    pause = 0;
                    break;
                case PARTIAL:
                    pause = 0;
                    if (phases != null) {
                        for (GCEvent phase : phases) {
                            if (phase.getEventType().getPause() == GCPause.PAUSE && phase.getEventLevel() == GCEventLevel.PHASE) {
                                pause += phase.getPause();
                            }
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
        return pause;
    }

    public long getPromotion() {
        return promotion;
    }

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$

    protected void appendStartTimestamp(StringBuilder sb, double referenceTimestamp) {
        if (referenceTimestamp != Constant.UNKNOWN_DOUBLE && startTime != Constant.UNKNOWN_DOUBLE) {
            sb.append(TIMESTAMP_FORMAT.format((long) (referenceTimestamp + startTime))).append(" "); //$NON-NLS-1$
        }
    }

    protected void appendStartTime(StringBuilder sb) {
        if (startTime != Constant.UNKNOWN_DOUBLE) {
            sb.append(String.format("%.3f: ", getStartTime() / 1000)); //$NON-NLS-1$
        }
    }

    private void appendGCSpecialSituation(StringBuilder sb) {
        List<String> parts = new ArrayList<>();
        if (isTrue(GCEventBooleanType.INITIAL_MARK)) {
            parts.add("Initial Mark"); //$NON-NLS-1$
        }
        if (isTrue(GCEventBooleanType.PREPARE_MIXED)) {
            parts.add("Prepare Mixed"); //$NON-NLS-1$
        }
        if (isTrue(GCEventBooleanType.TO_SPACE_EXHAUSTED)) {
            parts.add("To-space Exhausted"); //$NON-NLS-1$
        }

        if (parts.isEmpty()) {
            return;
        }
        sb.append("("); //$NON-NLS-1$
        for (int i = 0; i < parts.size(); i++) {
            if (i != 0) {
                sb.append(", "); //$NON-NLS-1$
            }
            sb.append(parts.get(i));
        }
        sb.append(") "); //$NON-NLS-1$
    }

    private void appendEventType(StringBuilder sb) {
        sb.append(eventType).append(" "); //$NON-NLS-1$
    }

    protected void appendClassSpecificInfo(StringBuilder sb) {
        if (gcid != Constant.UNKNOWN_INT) {
            sb.append('(').append(gcid).append(") "); //$NON-NLS-1$
        }

        if (cause != null) {
            sb.append("(").append(cause).append(") "); //$NON-NLS-1$ //$NON-NLS-2$
        }

        appendGCSpecialSituation(sb);

        if (getDuration() != Constant.UNKNOWN_DOUBLE) {
            sb.append(String.format("%.3f", getDuration() / 1000)).append("s "); //$NON-NLS-1$ //$NON-NLS-2$
        }

        memoryItemDo(item -> sb.append("[").append(item).append("] ")); //$NON-NLS-1$ //$NON-NLS-2$

        boolean moreInfoAvailable = getEventLevel() == GCEventLevel.EVENT
                && (getPromotion() != Constant.UNKNOWN_INT || getInterval() != Constant.UNKNOWN_DOUBLE);
        if (moreInfoAvailable) {
            boolean first = true;
            sb.append("["); //$NON-NLS-1$
            if (getPromotion() != Constant.UNKNOWN_INT) {
                sb.append("promotion ").append(getPromotion() / (long) KB2MB).append(" K"); //$NON-NLS-1$ //$NON-NLS-2$
                first = false;
            }
            if (getInterval() != Constant.UNKNOWN_INT) {
                if (!first) {
                    sb.append(", "); //$NON-NLS-1$
                }
                sb.append("interval ").append(String.format("%.3f", getInterval() / 1000)).append(" s"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            sb.append("] "); //$NON-NLS-1$
        }
        if (cpuTime != null) {
            sb.append("[").append(cpuTime).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    // this function is used for printing while debugging. The result is never showed to user
    // reference format: 14.244: Full GC (1) (Ergonomics) 0.001s [Young: 2548K->0K(18944K)] [Old: 33595K->11813K(44032K)] [Total: 36143K->11813K(62976K)] [Metaspace: 19355K->19355K(1067008K)] [promotion 3000 K, interval 30 s]
    public String toDebugString(GCModel model) {
        StringBuilder sb = new StringBuilder();
        appendStartTimestamp(sb, model.getReferenceTimestamp());
        appendStartTime(sb);
        appendEventType(sb);
        appendClassSpecificInfo(sb);
        return sb.toString();
    }

    // This function is used for printing while debugging(mainly used in IDE). The result is nover showed to user.
    // toDebugString shows more info than this function
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendStartTime(sb);
        appendEventType(sb);
        appendClassSpecificInfo(sb);
        return sb.toString();
    }

    public GCEventLevel getEventLevel() {
        return eventType.getLevel();
    }

    public void pauseEventOrPhasesDo(Consumer<GCEvent> consumer) {
        if (getEventLevel() != GCEventLevel.EVENT || isTrue(GCEventBooleanType.IGNORE_PAUSE)) {
            return;
        }
        switch (getEventType().getPause()) {
            case PAUSE:
                consumer.accept(this);
                break;
            case PARTIAL:
                phasesDoDFS(phase -> {
                    if (phase.getEventType().getPause() == GCPause.PAUSE && phase.getEventType().getLevel() == GCEventLevel.PHASE) {
                        consumer.accept(phase);
                    }
                });
            //$FALL-THROUGH$
        case CONCURRENT:
        default:
            break;
        }
    }

    public void memoryItemDo(Consumer<GCMemoryItem> consumer) {
        if (memory == null) {
            return;
        }
        for (GCMemoryItem item : memory) {
            if (item != null && !item.isEmpty()) {
                consumer.accept(item);
            }
        }
    }

    public void phasesDoDFS(Consumer<GCEvent> consumer) {
        if (phases != null) {
            for (GCEvent phase : phases) {
                consumer.accept(phase);
            }
        }
    }

    private Map<String, GCMemoryItem> memoryToVO() {
        Map<String, GCMemoryItem> result = new HashMap<>();
        memoryItemDo(item -> result.put(item.getArea().getName(), item));
        return result;
    }

    @Override
    protected void fillInfoToVO(GCModel model, GCEventVO vo, GlobalDiagnoseInfo diagnose) {
        super.fillInfoToVO(model, vo, diagnose);
        vo.saveInfo("eventType", eventType.getName()); //$NON-NLS-1$
        vo.saveInfo("gcid", gcid); //$NON-NLS-1$
        vo.saveInfo("cputime", cpuTime); //$NON-NLS-1$
        vo.saveInfo("referenceGC", referenceGC); //$NON-NLS-1$
        vo.saveInfo("memory", memoryToVO()); //$NON-NLS-1$
        vo.saveInfo("pause", pause); //$NON-NLS-1$
        vo.saveInfo("interval", interval); //$NON-NLS-1$
        vo.saveInfo("causeInterval", causeInterval); //$NON-NLS-1$
        vo.saveInfo("promotion", promotion); //$NON-NLS-1$
        vo.saveInfo("reclamation", reclamation); //$NON-NLS-1$
        vo.saveInfo("allocation", allocation); //$NON-NLS-1$
        phasesDoDFS(phase -> vo.addPhase(phase.toEventVO(model, diagnose)));
        vo.saveInfo("diagnose", diagnose.getEventDiagnoseVO(this)); //$NON-NLS-1$
        for (GCEventBooleanType type : GCEventBooleanType.values()) {
            if (isTrue(type)) {
                vo.saveInfo(type.name(), true);
            }
        }
        if (cause != null) {
            vo.saveInfo("cause", cause.getName()); //$NON-NLS-1$
        }
    }
}
