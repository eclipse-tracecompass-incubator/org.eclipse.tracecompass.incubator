/**********************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp;

import java.io.IOException;

import org.eclipse.tracecompass.tmf.core.model.IAxisDomain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Custom Jackson serializer for {@link IAxisDomain}.
 * <p>
 * This serializer outputs a JSON representation including the {@code "type"}
 * discriminator and the appropriate fields depending on the concrete subtype.
 * </p>
 *
 * Example JSON output:
 * <pre>
 * {
 *   "type": "categorical",
 *   "categories": ["foo", "bar"]
 * }
 *
 * {
 *   "type": "timeRange",
 *   "start": 0,
 *   "end": 100
 * }
 * </pre>
 *
 * @author Siwei Zhang
 * @since 10.2
 */
public class AxisDomainSerializer extends JsonSerializer<IAxisDomain> {

    private static final String TYPE = "type"; //$NON-NLS-1$

    @Override
    public void serialize(IAxisDomain value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        if (value instanceof IAxisDomain.Categorical categorical) {
            gen.writeStringField(TYPE, "categorical"); //$NON-NLS-1$
            gen.writeObjectField("categories", categorical.categories()); //$NON-NLS-1$
        } else if (value instanceof IAxisDomain.Range timeRange) {
            gen.writeStringField(TYPE, "timeRange"); //$NON-NLS-1$
            gen.writeNumberField("start", timeRange.start()); //$NON-NLS-1$
            gen.writeNumberField("end", timeRange.end()); //$NON-NLS-1$
        } else {
            throw new IllegalArgumentException("Unsupported AxisDomain implementation: " + value.getClass()); //$NON-NLS-1$
        }

        gen.writeEndObject();
    }
}
