/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.agent.trace.core.event;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Enum representing known Agent Trace event types.
 *
 * @author Eya Matkho
 */
public enum AgentTraceEventType {

    /** Agent session started */
    AGENT_START("agent_start"), //$NON-NLS-1$

    /** Agent session ended */
    AGENT_END("agent_end"), //$NON-NLS-1$

    /** Tool invocation */
    TOOL_CALL("tool_call"), //$NON-NLS-1$

    /** Tool result returned */
    TOOL_RESULT("tool_result"), //$NON-NLS-1$

    /** LLM request sent */
    LLM_REQUEST("llm_request"), //$NON-NLS-1$

    /** LLM response received */
    LLM_RESPONSE("llm_response"), //$NON-NLS-1$

    /** User message */
    USER_MESSAGE("user_message"), //$NON-NLS-1$

    /** Assistant message */
    ASSISTANT_MESSAGE("assistant_message"), //$NON-NLS-1$

    /** Error event */
    ERROR("error"), //$NON-NLS-1$

    /** Custom event */
    CUSTOM("custom"); //$NON-NLS-1$

    private static final Map<String, AgentTraceEventType> LOOKUP_MAP = new HashMap<>();

    static {
        for (AgentTraceEventType type : values()) {
            LOOKUP_MAP.put(type.fTypeName, type);
        }
    }

    private final String fTypeName;

    AgentTraceEventType(String typeName) {
        fTypeName = typeName;
    }

    /**
     * Get the string representation of this event type.
     *
     * @return the type name string
     */
    public String getTypeName() {
        return fTypeName;
    }

    /**
     * Look up an event type by its string name.
     *
     * @param name
     *            the type name to look up
     * @return the matching {@link AgentTraceEventType}, or {@code null} if not
     *         found
     */
    public static @Nullable AgentTraceEventType fromString(@Nullable String name) {
        if (name == null) {
            return null;
        }
        return LOOKUP_MAP.get(name);
    }

    /**
     * Check if a given string is a known agent trace event type.
     *
     * @param name
     *            the type name to check
     * @return {@code true} if the name matches a known type
     */
    public static boolean isKnownType(@Nullable String name) {
        return name != null && LOOKUP_MAP.containsKey(name);
    }
}
