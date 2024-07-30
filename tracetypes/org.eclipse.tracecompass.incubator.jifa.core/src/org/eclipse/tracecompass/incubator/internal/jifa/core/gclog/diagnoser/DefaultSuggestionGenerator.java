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

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.CHECK_CLASS_UNLOADING;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.CHECK_CPU_TIME;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.CHECK_FAST_OBJECT_ALLOCATION;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.CHECK_FAST_PROMOTION;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.CHECK_LIVE_OBJECTS;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.CHECK_MEMORY_LEAK;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.CHECK_METASPACE;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.CHECK_REFERENCE_GC;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.CHECK_RESCAN;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.CHECK_SYSTEM_GC;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.DISABLE_SYSTEM_GC;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.ENLARGE_METASPACE;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.INCREASE_CONC_GC_THREADS;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.SuggestionType.INCREASE_Z_ALLOCATION_SPIKE_TOLERANCE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.I18nStringView;

// This class generates common suggestions when we can not find the exact cause of problem.
public class DefaultSuggestionGenerator extends SuggestionGenerator {
    private AbnormalPoint ab;

    public DefaultSuggestionGenerator(GCModel model, AbnormalPoint ab) {
        super(model);
        this.ab = ab;
    }

    private static Map<AbnormalType, Method> rules = new HashMap<>();

    static {
        initializeRules();
    }

    private static void initializeRules() {
        Method[] methods = DefaultSuggestionGenerator.class.getDeclaredMethods();
        for (Method method : methods) {
            GeneratorRule annotation = method.getAnnotation(GeneratorRule.class);
            if (!Objects.isNull(annotation)) {
                method.setAccessible(true);
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod) || Modifier.isFinal(mod)) {
                    throw new IllegalStateException("Illegal method modifier: " + method);
                }
                rules.put(AbnormalType.getType(annotation.value()), method);
            }
        }
    }

    @GeneratorRule("metaspaceFullGC")
    private void metaspaceFullGC() {
        addSuggestion(CHECK_METASPACE);
        addSuggestion(ENLARGE_METASPACE);
        fullGCSuggestionCommon();
    }

    @GeneratorRule("systemGC")
    private void systemGC() {
        addSuggestion(CHECK_SYSTEM_GC);
        addSuggestion(DISABLE_SYSTEM_GC);
        suggestOldSystemGC();
        fullGCSuggestionCommon();
    }

    @GeneratorRule("outOfMemory")
    private void outOfMemory() {
        addSuggestion(CHECK_MEMORY_LEAK);
        suggestEnlargeHeap(false);
    }

    @GeneratorRule("allocationStall")
    private void allocationStall() {
        addSuggestion(CHECK_MEMORY_LEAK);
        suggestEnlargeHeap(true);
        addSuggestion(INCREASE_CONC_GC_THREADS);
        addSuggestion(INCREASE_Z_ALLOCATION_SPIKE_TOLERANCE);
    }

    @GeneratorRule("heapMemoryFullGC")
    private void heapMemoryFullGC() {
        addSuggestion(CHECK_MEMORY_LEAK);
        addSuggestion(CHECK_FAST_PROMOTION);
        suggestStartOldGCEarly();
        fullGCSuggestionCommon();
    }

    @GeneratorRule("longYoungGCPause")
    private void longYoungGCPause() {
        addSuggestion(CHECK_LIVE_OBJECTS);
        addSuggestion(CHECK_CPU_TIME);
        addSuggestion(CHECK_REFERENCE_GC);
        suggestCheckEvacuationFailure();
        suggestShrinkYoungGen();
        suggestUseMoreDetailedLogging();
    }

    @GeneratorRule("frequentYoungGC")
    private void frequentYoungGC() {
        suggestExpandYoungGen();
        addSuggestion(CHECK_FAST_OBJECT_ALLOCATION);
    }

    @GeneratorRule("longG1Remark")
    private void longG1Remark() {
        addSuggestion(CHECK_REFERENCE_GC);
        addSuggestion(CHECK_CLASS_UNLOADING);
        suggestUseMoreDetailedLogging();
    }

    @GeneratorRule("longCMSRemark")
    private void longCMSRemark() {
        addSuggestion(CHECK_RESCAN);
        addSuggestion(CHECK_REFERENCE_GC);
        addSuggestion(CHECK_CLASS_UNLOADING);
        suggestUseMoreDetailedLogging();
    }

    public List<I18nStringView> generate() {
        if (ab.getType() == null) {
            return result;
        }
        Method rule = rules.getOrDefault(ab.getType(), null);
        if (rule != null) {
            try {
                rule.invoke(this);
            } catch (Exception e) {
                throw new IllegalStateException();
            }
        }
        return result;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface GeneratorRule {
        String value();
    }
}
