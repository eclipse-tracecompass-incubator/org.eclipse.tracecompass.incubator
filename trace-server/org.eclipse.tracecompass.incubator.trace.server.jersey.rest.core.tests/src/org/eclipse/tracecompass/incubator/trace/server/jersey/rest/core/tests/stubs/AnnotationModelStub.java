/**********************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic Implementation of the serialized annotation model used by clients.
 *
 * @author Bernd Hufmann
 */
public class AnnotationModelStub implements Serializable{

    private static final long serialVersionUID = -8294862755091478069L;

    private final Map<String, Collection<AnnotationStub>> fAnnotations;

    /**
     * Constructor
     *
     * @param annotations
     *            Map of annotations per category
     */
    public AnnotationModelStub(@JsonProperty("annotations") Map<String, Collection<AnnotationStub>> annotations) {
        fAnnotations = annotations;
    }

    /**
     * Annotations for the model
     *
     * @return Map of annotations per category
     */
    public Map<String, Collection<AnnotationStub>> getAnnotations() {
        return fAnnotations;
    }}
