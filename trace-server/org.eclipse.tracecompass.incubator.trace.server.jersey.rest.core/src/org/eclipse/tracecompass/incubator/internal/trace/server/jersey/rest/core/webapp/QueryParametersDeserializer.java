/**********************************************************************
 * Copyright (c) 2021 Ericsson
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.Filter;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.annotations.Annotation;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.annotations.IAnnotation.AnnotationType;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Deserializer for query parameters {@link QueryParameters}
 *
 * @author Patrick Tasse
 */
public class QueryParametersDeserializer extends StdDeserializer<QueryParameters> {


    private static final long serialVersionUID = 4511735530369955327L;

    private interface ElementType {
        String STATE = "state"; //$NON-NLS-1$
        String ANNOTATION = "annotation"; //$NON-NLS-1$
        String ARROW = "arrow"; //$NON-NLS-1$
    }
    private static final String DESTINATION_ID = "destinationId"; //$NON-NLS-1$
    private static final String DURATION = "duration"; //$NON-NLS-1$
    private static final String ELEMENT_TYPE = "elementType"; //$NON-NLS-1$
    private static final String ENTRY_ID = "entryId"; //$NON-NLS-1$
    private static final String FILTERS = "filters"; //$NON-NLS-1$
    private static final String PARAMETERS = "parameters"; //$NON-NLS-1$
    private static final String TIME = "time"; //$NON-NLS-1$

    private static final @NonNull OutputElementStyle EMPTY_STYLE = new OutputElementStyle(null, Collections.emptyMap());

    /**
     * Constructor
     */
    protected QueryParametersDeserializer() {
        super(QueryParameters.class);
    }

    @Override
    public QueryParameters deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec codec = jp.getCodec();
        JsonNode node = codec.readTree(jp);

        Map<String, Object> parameters = new HashMap<>();

        JsonNode parametersNode = node.get(PARAMETERS);
        if (parametersNode != null) {
            JsonParser parametersParser = parametersNode.traverse(codec);
            parametersParser.nextToken();
            parameters = ctxt.readValue(parametersParser, Map.class);

            /* Replace default deserialized map with the correct element object */
            parameters.computeIfPresent(DataProviderParameterUtils.REQUESTED_ELEMENT_KEY, (k, v) -> {
                if (v instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) v;
                    Object elementType = map.get(ELEMENT_TYPE);
                    long time = ((Number) map.getOrDefault(TIME, 0L)).longValue();
                    long duration = ((Number) map.getOrDefault(DURATION, 0L)).longValue();
                    if (ElementType.STATE.equals(elementType)) {
                        return new TimeGraphState(time, duration, null, null);
                    } else if (ElementType.ANNOTATION.equals(elementType)) {
                        long entryId = ((Number) map.getOrDefault(ENTRY_ID, -1L)).longValue();
                        return new Annotation(time, duration, entryId, AnnotationType.CHART, null, EMPTY_STYLE);
                    } else if (ElementType.ARROW.equals(elementType)) {
                        long sourceId = ((Number) map.getOrDefault(ENTRY_ID, -1L)).longValue();
                        long destinationId = ((Number) map.getOrDefault(DESTINATION_ID, -1L)).longValue();
                        return new TimeGraphArrow(sourceId, destinationId, time, duration, EMPTY_STYLE);
                    }
                }
                return null;
            });
        }
        List<Filter> filters = null;
        JsonNode filtersNode = node.get(FILTERS);
        if (filtersNode != null) {
            JsonParser filtersParser = filtersNode.traverse(codec);
            filtersParser.nextToken();
            filters = ctxt.readValue(filtersParser, List.class);
        }

        return new QueryParameters(parameters, filters);
    }
}
