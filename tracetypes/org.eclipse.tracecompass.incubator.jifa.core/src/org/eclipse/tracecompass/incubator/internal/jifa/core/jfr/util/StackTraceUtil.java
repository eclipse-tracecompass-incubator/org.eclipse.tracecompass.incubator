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
package org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.Frame;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.JavaFrame;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.JavaMethod;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedClass;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedFrame;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedMethod;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedStackTrace;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.StackTrace;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.SymbolBase;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.SymbolTable;

public class StackTraceUtil {
    public static final RecordedStackTrace DUMMY_STACK_TRACE = StackTraceUtil.newDummyStackTrace("", "", "NO Frame");

    // FIXME: need cache
    public static StackTrace build(RecordedStackTrace stackTrace, SymbolTable<SymbolBase> symbols) {
        StackTrace result = new StackTrace();
        result.setTruncated(stackTrace.isTruncated());

        DescriptorUtil util = new DescriptorUtil();
        List<RecordedFrame> srcFrames = stackTrace.getFrames();
        Frame[] dstFrames = new Frame[srcFrames.size()];
        for (int i = 0; i < srcFrames.size(); i++) {
            RecordedFrame frame = srcFrames.get(i);
            Frame dstFrame;
            if (frame.isJavaFrame()) {
                dstFrame = new JavaFrame();
                ((JavaFrame) dstFrame).setJavaFrame(frame.isJavaFrame());
                ((JavaFrame) dstFrame).setType(JavaFrame.Type.typeOf(frame.getType()));
                ((JavaFrame) dstFrame).setBci(frame.getBytecodeIndex());
            } else {
                dstFrame = new Frame();
            }

            RecordedMethod method = frame.getMethod();
            JavaMethod dstMethod = new JavaMethod();
            dstMethod.setPackageName(method.getType().getPackageName());
            dstMethod.setType(method.getType().getName());
            dstMethod.setName(method.getName());
            dstMethod.setDescriptor(util.decodeMethodArgs(method.getDescriptor()));

            dstMethod.setModifiers(method.getModifiers());
            dstMethod.setHidden(method.isHidden());
            if (symbols.isContains(dstMethod)) {
                dstMethod = (JavaMethod) symbols.get(dstMethod);
            } else {
                symbols.put(dstMethod);
            }

            dstFrame.setMethod(dstMethod);
            dstFrame.setLine(frame.getLineNumber());
            if (symbols.isContains(dstFrame)) {
                dstFrame = (Frame) symbols.get(dstFrame);
            } else {
                symbols.put(dstFrame);
            }

            dstFrames[i] = dstFrame;
        }

        result.setFrames(dstFrames);
        if (symbols.isContains(result)) {
            result = (StackTrace) symbols.get(result);
        } else {
            symbols.put(result);
        }

        return result;
    }

    public static RecordedStackTrace newDummyStackTrace(String packageName, String className, String methodName) {
        RecordedStackTrace st = new RecordedStackTrace();
        List<RecordedFrame> list = new ArrayList<>();
        RecordedFrame f = new RecordedFrame();
        RecordedMethod m = new RecordedMethod();
        RecordedClass c = new RecordedClass();
        c.setPackageName(packageName);
        c.setName(className);
        m.setType(c);
        f.setMethod(m);
        m.setName(methodName);
        list.add(f);
        st.setFrames(list);
        return st;
    }
}
