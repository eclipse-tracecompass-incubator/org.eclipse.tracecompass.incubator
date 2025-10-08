/*******************************************************************************
 * Copyright (c) 2025 Ericsson and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.webapp;

import org.junit.Before;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Abstract test class for testing serializers
 *
 * @author Bernd Hufmann
 */
public class AbstractSerializerTest {

    /**
     * A object mapper to be used for serialization / deserialization. A new
     * instance is created per test case.
     */
    protected ObjectMapper fMapper;

    /**
     * Test setup
     */
    @Before
    public void setup() {
        fMapper = JsonMapper.builder().configure(MapperFeature.ALLOW_COERCION_OF_SCALARS, false).build();
    }
}
