/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.event;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages
 *
 * @author Katherine Nadeau
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.opentracing.core.event.messages"; //$NON-NLS-1$
    /**
     * Tags
     */
    public static @Nullable String OpenTracingAspects_Tags;
    /**
     * Tags Description
     */
    public static @Nullable String OpenTracingAspects_TagsD;
    /**
     * ID
     */
    public static @Nullable String OpenTracingAspects_SpanId;
    /**
     * ID Description
     */
    public static @Nullable String OpenTracingAspects_SpanIdD;
    /**
     * Name
     */
    public static @Nullable String OpenTracingAspects_Name;
    /**
     * Name Description
     */
    public static @Nullable String OpenTracingAspects_NameD;
    /**
     * Duration
     */
    public static @Nullable String OpenTracingAspects_Duration;
    /**
     * Duration Description
     */
    public static @Nullable String OpenTracingAspects_DurationD;
    /**
     * Process Id
     */
    public static @Nullable String OpenTracingAspects_Process;
    /**
     * Process Id Description
     */
    public static @Nullable String OpenTracingAspects_ProcessD;
    /**
     * Process Tags
     */
    public static @Nullable String OpenTracingAspects_ProcessTags;
    /**
     * Process Tags Description
     */
    public static @Nullable String OpenTracingAspects_ProcessTagsD;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
