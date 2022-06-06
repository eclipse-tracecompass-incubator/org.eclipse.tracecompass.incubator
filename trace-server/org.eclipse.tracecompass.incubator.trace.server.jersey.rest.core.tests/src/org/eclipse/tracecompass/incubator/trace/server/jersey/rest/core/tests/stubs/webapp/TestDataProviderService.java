/**********************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.webapp;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.TreeModelWrapper;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.TimeGraphEntryModelSerializer;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

/**
 * Custom data provider service used for testing
 */
@SuppressWarnings("null")
public class TestDataProviderService extends DataProviderService {

    /** Data provider to test {@link TimeGraphEntryModelSerializer} */
    public static final String INVALID_ENTRY_METADATA = "rest.core.test.valid.entry.metadata.dp";
    /** Metadata Key for bytes */
    public static final String VALID_TEST_KEY_BYTE = "test-key-byte";
    /** Metadata Key for shorts */
    public static final String VALID_TEST_KEY_SHORT = "test-key-short";
    /** Metadata Key for integers */
    public static final String VALID_TEST_KEY_INT = "test-key-int";
    /** Metadata Key for longs */
    public static final String VALID_TEST_KEY_LONG = "test-key-long";
    /** Metadata Key for floats */
    public static final String VALID_TEST_KEY_FLOAT = "test-key-float";
    /** Metadata Key for doubles */
    public static final String VALID_TEST_KEY_DOUBLE = "test-key-double";
    /** Metadata Key for strings */
    public static final String VALID_TEST_KEY_STRING = "test-key-string";
    /** Metadata Key for unsupported object */
    public static final String INVALID_TEST_KEY = "invalid-test-key";
    /** Entry name for entry with metadata */
    public static final String ENTRY_NAME_WITH_METADATA = "test-with-metadata";
    /** Entry name for entry without metadata */
    public static final String ENTRY_NAME_WITHOUT_METADATA = "test-without-metadata";

    @Override
    public Response getTimeGraphTree(UUID expUUID, String outputId, QueryParameters queryParameters) {
        if (outputId.equals(INVALID_ENTRY_METADATA)) {
            TestTimeGraphEntryModel entry = new TestTimeGraphEntryModel(1, 0, ENTRY_NAME_WITH_METADATA, 0, 100, true);
            @NonNull List<@NonNull ITmfTreeDataModel> list = new ArrayList<>();
            list.add(entry);
            entry = new TestTimeGraphEntryModel(1, 0, ENTRY_NAME_WITHOUT_METADATA, 0, 100, false);
            list.add(entry);
            TmfTreeModel<@NonNull ITmfTreeDataModel> model = new TmfTreeModel<>(ImmutableList.of("test"), list);
            return Response.ok(new TmfModelResponse<>(new TreeModelWrapper(model), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED)).build();
        }
        return super.getTimeGraphTree(expUUID, outputId, queryParameters);
    }

    private static class TestTimeGraphEntryModel extends TimeGraphEntryModel {
        private final boolean fWithMetadata;

        public TestTimeGraphEntryModel(long id, long parentId, String name, long startTime, long endTime, boolean withMetadata) {
            super(id, parentId, name, startTime, endTime);
            fWithMetadata = withMetadata;
        }

        @Override
        public @NonNull Multimap<@NonNull String, @NonNull Object> getMetadata() {
            Multimap<@NonNull String, @NonNull Object> metadata = HashMultimap.create();
            if (fWithMetadata) {
                for (byte i = 0; i < 3; i++) {
                    metadata.put(VALID_TEST_KEY_BYTE, Byte.valueOf(i));
                    metadata.put(VALID_TEST_KEY_SHORT, Short.valueOf(i));
                    metadata.put(VALID_TEST_KEY_INT, Integer.valueOf(i));
                    metadata.put(VALID_TEST_KEY_LONG, Long.valueOf(i));
                    metadata.put(VALID_TEST_KEY_FLOAT, Float.valueOf(i));
                    metadata.put(VALID_TEST_KEY_DOUBLE, Double.valueOf(i));
                    metadata.put(VALID_TEST_KEY_STRING, String.valueOf(i));
                    metadata.put(INVALID_TEST_KEY, TmfTimestamp.fromSeconds(i));
                }
            }
            return metadata;
        }
    }
}
