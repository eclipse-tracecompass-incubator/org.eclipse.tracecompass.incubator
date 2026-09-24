/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.core.trace;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Immutable semantic metadata for one normalized TraceX event ID. */
public final class TraceXEventMetadata {

    /** Immutable alias of one raw information word. */
    public static final class Alias {
        private final int fWord;
        private final String fName;
        private final boolean fAddress;

        Alias(int word, String name, boolean address) {
            fWord = word;
            fName = name;
            fAddress = address;
        }

        /** @return one-based raw information-word index */
        public int getWord() {
            return fWord;
        }

        /** @return semantic field name */
        public String getName() {
            return fName;
        }

        /** @return whether the raw word represents an address */
        public boolean isAddress() {
            return fAddress;
        }
    }

    private final int fId;
    private final String fName;
    private final TraceXEventCategory fCategory;
    private final List<Alias> fAliases;

    TraceXEventMetadata(int id, String name, TraceXEventCategory category,
            Alias... aliases) {
        fId = id;
        fName = name;
        fCategory = category;
        fAliases = Collections.unmodifiableList(Arrays.asList(aliases.clone()));
    }

    /** @return normalized low-16-bit event ID */
    public int getId() {
        return fId;
    }

    /** @return stable semantic event name */
    public String getName() {
        return fName;
    }

    /** @return stable component category */
    public TraceXEventCategory getCategory() {
        return fCategory;
    }

    /** @return semantic aliases ordered by their raw information word */
    public List<Alias> getAliases() {
        return fAliases;
    }
}
