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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic Implementation of the serialized annotation category used by clients.
 *
 * @author Bernd Hufmann
 */
public class AnnotationCategoriesModelStub implements Serializable{

    private static final long serialVersionUID = -7146696718088222398L;
    private final List<String> fAnnotationCategories;

    /**
     * Constructor
     *
     * @param annotationCategories
     *            List of categories
     */
    public AnnotationCategoriesModelStub(@JsonProperty("annotationCategories") List<String> annotationCategories) {
        fAnnotationCategories = annotationCategories;
    }

    /**
     * Annotation categories for the model
     *
     * @return List of categories
     */
    public List<String> getAnnotationCategories() {
        return fAnnotationCategories;
    }
}
