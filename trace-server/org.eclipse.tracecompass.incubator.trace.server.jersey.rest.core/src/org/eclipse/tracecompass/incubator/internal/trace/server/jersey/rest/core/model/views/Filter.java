/**********************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views;

/**
 * Filter that is received by the server to be applied on the response.
 *
 * @author Simon Delisle
 */
public class Filter {
    private long fId;
    private String fName;
    private long fStartTime;
    private long fEndTime;
    private String fExpression;
    private int fTags;

    /**
     * Constructor.
     *
     * @param id
     *            Filter ID
     * @param name
     *            Human readable name
     * @param startTime
     *            Filter start time
     * @param endTime
     *            Filter end time
     * @param expression
     *            Filter expression (eg. regex)
     * @param tags
     *            Tags to be apply on responses (with the filter does)
     */
    public Filter(long id, String name, long startTime, long endTime, String expression, int tags) {
        super();
        fId = id;
        fName = name;
        fStartTime = startTime;
        fEndTime = endTime;
        fExpression = expression;
        fTags = tags;
    }

    /**
     * Getter for filter ID
     *
     * @return Filter ID
     */
    public long getId() {
        return fId;
    }

    /**
     * Getter for filter name
     *
     * @return Human readable name
     */
    public String getName() {
        return fName;
    }

    /**
     * Getter for filter start time
     *
     * @return Filter start time
     */
    public long getStartTime() {
        return fStartTime;
    }

    /**
     * Getter for filter end time
     *
     * @return Filter end time
     */
    public long getEndTime() {
        return fEndTime;
    }

    /**
     * Getter for filter expression
     *
     * @return Filter expression
     */
    public String getExpression() {
        return fExpression;
    }

    /**
     * Getter for filter tags
     *
     * @return Tags to apply
     */
    public int getTags() {
        return fTags;
    }
}
