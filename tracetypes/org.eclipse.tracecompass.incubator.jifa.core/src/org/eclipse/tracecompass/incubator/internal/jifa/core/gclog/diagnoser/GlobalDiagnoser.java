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

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser;

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.AbnormalType.ALLOCATION_STALL;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.AbnormalType.FREQUENT_YOUNG_GC;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.AbnormalType.HEAP_MEMORY_FULL_GC;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.AbnormalType.LONG_CMS_REMARK;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.AbnormalType.LONG_G1_REMARK;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.AbnormalType.LONG_YOUNG_GC_PAUSE;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.AbnormalType.METASPACE_FULL_GC;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.TimedEvent.newByStartEnd;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.CMS_FINAL_REMARK;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.FULL_GC;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.G1_REMARK;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_DOUBLE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.TimedEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCCause;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.ZGCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.DoubleData;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.I18nStringView;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Key2ValueListMap;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.vo.TimeRange;

/**
 * To diagnose abnormal in gclog, we mainly try to analyze 3 things: 1. what's
 * going wrong 2. why it is going wrong 3. how to deal with it Currently, we
 * have just implemented finding global serious and definite problem and give
 * general suggestions based on phenomenon without analyzing cause specific
 * cause. In the future, we will 1. do local diagnose on each event, find
 * abnormal of event info, explain its cause and give appropriate suggestion if
 * necessary. 2. Try to find accurate cause and give "the best" suggestion for
 * those serious based on local diagnose.
 */
public class GlobalDiagnoser {
    private GCModel model;
    private AnalysisConfig config;

    private Key2ValueListMap<String, Double> allProblems = new Key2ValueListMap<>();
    private List<AbnormalPoint> mostSeriousProblemList = new ArrayList<>();
    private List<AbnormalPoint> mergedMostSeriousProblemList = new ArrayList<>();
    private AbnormalPoint mostSerious = AbnormalPoint.LEAST_SERIOUS;

    public GlobalDiagnoser(GCModel model, AnalysisConfig config) {
        this.model = model;
        this.config = config;
    }

    public GlobalAbnormalInfo diagnose() {
        findAllAbnormalPoints();
        mergeTimeRanges();
        return generateVo();
    }

    private void findAllAbnormalPoints() {
        for (Method rule : globalDiagnoseRules) {
            try {
                rule.invoke(this);
            } catch (Exception e) {
                throw new IllegalStateException();
            }
        }
    }

    // Extend the start time forward by 2.5 min so that user can see what
    // happened before the problem.
    // Extend the end time backward by 2.5 min so adjacent events can be merged.
    private static long EXTEND_TIME = 150 * 1000;

    // allow changing this value for testing
    public static void setExtendTime(long extendTime) {
        EXTEND_TIME = extendTime;
    }

    private void mergeTimeRanges() {
        if (mostSerious == AbnormalPoint.LEAST_SERIOUS) {
            return;
        }
        AbnormalPoint first = mostSeriousProblemList.get(0);
        mostSeriousProblemList.sort(Comparator.comparingDouble(ab -> ab.getSite().getStartTime()));
        double start = UNKNOWN_DOUBLE;
        double end = UNKNOWN_DOUBLE;
        for (AbnormalPoint ab : mostSeriousProblemList) {
            if (start == UNKNOWN_DOUBLE) {
                start = ab.getSite().getStartTime();
                end = Math.max(ab.getSite().getStartTime(), ab.getSite().getEndTime());
            } else if (ab.getSite().getStartTime() - end <= 2 * EXTEND_TIME) {
                end = Math.max(Math.max(ab.getSite().getStartTime(), ab.getSite().getEndTime()), end);
            } else {
                AbnormalPoint merged = new AbnormalPoint(first.getType(), newByStartEnd(start, end));
                mergedMostSeriousProblemList.add(merged);
                start = ab.getSite().getStartTime();
                end = Math.max(ab.getSite().getStartTime(), ab.getSite().getEndTime());
            }
        }
        if (start != UNKNOWN_DOUBLE) {
            AbnormalPoint merged = new AbnormalPoint(first.getType(), newByStartEnd(start, end));
            mergedMostSeriousProblemList.add(merged);
        }
    }

    private GlobalAbnormalInfo generateVo() {
        MostSeriousProblemSummary summary = null;
        if (mostSerious != AbnormalPoint.LEAST_SERIOUS) {
            AbnormalPoint first = mergedMostSeriousProblemList.get(0);
            first.generateDefaultSuggestions(model);
            summary = new MostSeriousProblemSummary(
                    mergedMostSeriousProblemList.stream()
                            .sorted((ab1, ab2) -> Double.compare(ab2.getSite().getDuration(), ab1.getSite().getDuration()))
                            .limit(3)
                            .sorted(Comparator.comparingDouble(ab -> ab.getSite().getStartTime()))
                            .map(ab -> new TimeRange(
                                    Math.max(ab.getSite().getStartTime() - EXTEND_TIME, config.getTimeRange().getStart()),
                                    Math.min(ab.getSite().getEndTime() + EXTEND_TIME, config.getTimeRange().getEnd())))
                            .collect(Collectors.toList()),
                    first.getType().toI18nStringView(),
                    first.getDefaultSuggestions());
        }
        return new GlobalAbnormalInfo(summary, allProblems.getInnerMap());
    }

    private static List<Method> globalDiagnoseRules = new ArrayList<>();

    static {
        initializeRules();
    }

    private static void initializeRules() {
        Method[] methods = GlobalDiagnoser.class.getDeclaredMethods();
        for (Method method : methods) {
            if (!Objects.isNull(method.getAnnotation(GlobalDiagnoseRule.class))) {
                method.setAccessible(true);
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod) || Modifier.isFinal(mod) ||
                        !(Modifier.isPublic(mod) || Modifier.isProtected(mod))) {
                    throw new IllegalStateException("Illegal method modifier: " + method);
                }
                globalDiagnoseRules.add(method);
            }
        }
    }

    @GlobalDiagnoseRule
    protected void longGCPause() {
        model.iterateEventsWithinTimeRange(model.getAllEvents(), config.getTimeRange(), event -> {
            event.pauseEventOrPhasesDo(pauseEvent -> {
                if (pauseEvent.getPause() <= config.getLongPauseThreshold()) {
                    return;
                }
                if (pauseEvent.isYoungGC()) {
                    addAbnormalPoint(new AbnormalPoint(LONG_YOUNG_GC_PAUSE, pauseEvent));
                }
            });
        });
    }

    @GlobalDiagnoseRule
    protected void allocationStall() {
        if (model.getCollectorType() != GCCollectorType.ZGC) {
            return;
        }
        ZGCModel zModel = (ZGCModel) model;
        model.iterateEventsWithinTimeRange(zModel.getAllocationStalls(), config.getTimeRange(), stall -> {
            addAbnormalPoint(new AbnormalPoint(ALLOCATION_STALL, stall));
        });
    }

    @GlobalDiagnoseRule
    protected void outOfMemory() {
        model.iterateEventsWithinTimeRange(model.getOoms(), config.getTimeRange(), oom -> {
            addAbnormalPoint(new AbnormalPoint(AbnormalType.OUT_OF_MEMORY, oom));
        });
    }

    @GlobalDiagnoseRule
    protected void longRemark() {
        model.iterateEventsWithinTimeRange(model.getAllEvents(), config.getTimeRange(), remark -> {
            GCEventType type = remark.getEventType();
            if (remark.getPause() < config.getLongPauseThreshold()) {
                return;
            }
            if (type == CMS_FINAL_REMARK) {
                addAbnormalPoint(new AbnormalPoint(LONG_CMS_REMARK, remark));
            } else if (type == G1_REMARK) {
                addAbnormalPoint(new AbnormalPoint(LONG_G1_REMARK, remark));
            }
        });
    }

    @GlobalDiagnoseRule
    protected void frequentYoungGC() {
        DoubleData interval = new DoubleData();
        model.iterateEventsWithinTimeRange(model.getGcEvents(), config.getTimeRange(), event -> {
            if (event.isYoungGC() && event.getInterval() != UNKNOWN_DOUBLE) {
                interval.add(event.getInterval());
            }
        });
        if (interval.getN() > 0 && interval.average() < config.getYoungGCFrequentIntervalThreshold()) {
            addAbnormalPoint(new AbnormalPoint(FREQUENT_YOUNG_GC, TimedEvent.fromTimeRange(config.getTimeRange())));
        }
    }

    @GlobalDiagnoseRule
    protected void fullGC() {
        boolean shouldAvoidFullGC = model.shouldAvoidFullGC();
        model.iterateEventsWithinTimeRange(model.getGcEvents(), config.getTimeRange(), event -> {
            if (event.getEventType() != FULL_GC) {
                return;
            }
            GCCause cause = event.getCause();
            if (cause != null) {
                if (cause.isMetaspaceFullGCCause()) {
                    addAbnormalPoint(new AbnormalPoint(METASPACE_FULL_GC, event));
                } else if (shouldAvoidFullGC && cause.isHeapMemoryTriggeredFullGCCause()) {
                    addAbnormalPoint(new AbnormalPoint(HEAP_MEMORY_FULL_GC, event));
                } else if (cause == GCCause.SYSTEM_GC) {
                    addAbnormalPoint(new AbnormalPoint(AbnormalType.SYSTEM_GC, event));
                }
            }
        });
    }

    private void addAbnormalPoint(AbnormalPoint point) {
        allProblems.put(point.getType().getName(), point.getSite().getStartTime());
        int compare = AbnormalPoint.compareByImportance.compare(point, mostSerious);
        if (compare < 0) {
            mostSeriousProblemList.clear();
            mostSerious = point;
        }
        if (compare <= 0) {
            mostSeriousProblemList.add(point);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface GlobalDiagnoseRule {
    }

    public static class GlobalAbnormalInfo {
        public GlobalAbnormalInfo(MostSeriousProblemSummary summary, Map<String, List<Double>> innerMap) {
            mostSeriousProblem = summary;
            seriousProblems = innerMap;
        }

        private MostSeriousProblemSummary mostSeriousProblem;
        private Map<String, List<Double>> seriousProblems;

        /**
         * @return the mostSeriousProblem
         */
        public MostSeriousProblemSummary getMostSeriousProblem() {
            return mostSeriousProblem;
        }

        /**
         * @param mostSeriousProblem
         *            the mostSeriousProblem to set
         */
        public void setMostSeriousProblem(MostSeriousProblemSummary mostSeriousProblem) {
            this.mostSeriousProblem = mostSeriousProblem;
        }

        /**
         * @return the seriousProblems
         */
        public Map<String, List<Double>> getSeriousProblems() {
            return seriousProblems;
        }

        /**
         * @param seriousProblems
         *            the seriousProblems to set
         */
        public void setSeriousProblems(Map<String, List<Double>> seriousProblems) {
            this.seriousProblems = seriousProblems;
        }
    }

    public static class MostSeriousProblemSummary {
        public MostSeriousProblemSummary(@NonNull List<@NonNull TimeRange> collect, I18nStringView i18nStringView, List<I18nStringView> defaultSuggestions) {
            sites = collect;
            problem = i18nStringView;
            suggestions = defaultSuggestions;
        }

        private List<TimeRange> sites;
        private I18nStringView problem;
        private List<I18nStringView> suggestions;

        /**
         * @return the sites
         */
        public List<TimeRange> getSites() {
            return sites;
        }

        /**
         * @param sites
         *            the sites to set
         */
        public void setSites(List<TimeRange> sites) {
            this.sites = sites;
        }

        /**
         * @return the problem
         */
        public I18nStringView getProblem() {
            return problem;
        }

        /**
         * @param problem
         *            the problem to set
         */
        public void setProblem(I18nStringView problem) {
            this.problem = problem;
        }

        /**
         * @return the suggestions
         */
        public List<I18nStringView> getSuggestions() {
            return suggestions;
        }

        /**
         * @param suggestions
         *            the suggestions to set
         */
        public void setSuggestions(List<I18nStringView> suggestions) {
            this.suggestions = suggestions;
        }
    }
}
