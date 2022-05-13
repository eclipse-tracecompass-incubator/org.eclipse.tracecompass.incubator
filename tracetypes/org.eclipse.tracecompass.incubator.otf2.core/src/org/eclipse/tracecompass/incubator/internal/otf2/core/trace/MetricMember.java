/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.trace;

import static org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants.OTF2_UNKNOWN_STRING;
import static org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants.UNKNOWN_STRING;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.counters.core.CounterType;
import org.eclipse.tracecompass.incubator.internal.otf2.core.Activator;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * A class representing a metric member. It is used to represent a metric. It
 * contains all the needed information to describe the metric (name,
 * description, unit, source, ...)
 *
 * @author Yoann Heitz
 */
public class MetricMember {

    private final long fMetricMemberId;
    private final String fName;
    private final String fDescription;
    private final int fMetricType;
    private final int fMetricMode;
    private final int fValueTypeCode;
    private CounterType fCounterType = CounterType.LONG;
    private final int fBase;
    private final long fExponent;
    private double fFactor;
    private final String fUnit;

    /**
     * Constructor for this class
     *
     * @param event
     *            an event representing a metric member
     * @param stringIds
     *            the map associating the string IDs to their values for this
     *            trace
     */
    public MetricMember(ITmfEvent event, Map<Integer, String> stringIds) {
        ITmfEventField content = event.getContent();
        Long selfId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_SELF);
        fMetricMemberId = selfId != null ? selfId : IOtf2Constants.OTF2_UNKNOWN_METRIC_MEMBER;

        Long nameId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_NAME);
        nameId = nameId != null ? nameId : OTF2_UNKNOWN_STRING;
        String name = stringIds.get(nameId.intValue());
        name = name != null ? name : UNKNOWN_STRING;
        fName = name;

        Long descriptionId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_DESCRIPTION);
        descriptionId = descriptionId != null ? descriptionId : OTF2_UNKNOWN_STRING;
        String description = stringIds.get(descriptionId.intValue());
        description = description != null ? description : UNKNOWN_STRING;
        fDescription = description;

        Integer metricType = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_METRIC_TYPE);
        fMetricType = metricType != null ? metricType : IOtf2Constants.OTF2_UNKNOWN_METRIC_TYPE;

        Integer metricMode = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_METRIC_MODE);
        fMetricMode = metricMode != null ? metricMode : IOtf2Constants.OTF2_UNKNOWN_METRIC_MODE;

        Integer valueType = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_VALUE_TYPE);
        fValueTypeCode = valueType != null ? valueType : IOtf2Constants.OTF2_UNKNOWN_VALUE_TYPE;

        Integer base = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_BASE);
        fBase = base != null ? base : IOtf2Constants.OTF2_UNKNOWN_BASE;

        Long exponent = content.getFieldValue(Long.class, IOtf2Fields.OTF2_EXPONENT);
        fExponent = exponent != null ? exponent : IOtf2Constants.OTF2_UNKNOWN_EXPONENT;

        setCounterType();
        computeAndSetFactor();

        Long unitId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_UNIT);
        unitId = unitId != null ? unitId : OTF2_UNKNOWN_STRING;
        String unit = stringIds.get(unitId.intValue());
        unit = unit != null ? unit : UNKNOWN_STRING;
        fUnit = unit;
    }

    /**
     * Gets the ID of this metric member
     *
     * @return the ID of this metric member
     */
    public long getId() {
        return fMetricMemberId;
    }

    /**
     * Gets the name of this metric member
     *
     * @return the name of this metric member
     */
    public String getName() {
        return fName;
    }

    private void computeAndSetFactor() {
        double factor;
        switch (fBase) {
        case IOtf2Constants.BINARY_BASE_CODE:
            factor = 2.0;
            break;
        case IOtf2Constants.DECIMAL_BASE_CODE:
            factor = 10.0;
            break;
        default:
            factor = 0.0;
            break;
        }
        fFactor = Math.pow(factor, fExponent);
    }

    /**
     * Converts the value passed as argument (that has been read from a metric
     * event) to the unit defined in fUnit field.
     *
     * @param rawValue
     *            the raw value of the metric that was read from the event
     *
     * @return the value converted to the unit defined in the fUnit field, or 0
     *         if the counter type is different than long or double.
     */
    public Number computeValueWithDefinedUnit(Number rawValue) {
        switch (fCounterType) {
        case DOUBLE:
            return rawValue.doubleValue() * fFactor;
        case LONG:
            return rawValue.longValue() * (long) fFactor;
        default:
            return 0;
        }
    }

    private void setCounterType() {
        // If exponent is negative then metrics are stored as double since the
        // factor will be a double.
        if (fExponent < 0) {
            fCounterType = CounterType.DOUBLE;
            return;
        }

        // If exponent is not negative then we still need to check if the
        // metrics are stored as double or integers in the events
        switch (fValueTypeCode) {
        case IOtf2Constants.OTF2_TYPE_INT64:
        case IOtf2Constants.OTF2_TYPE_UINT64:
            fCounterType = CounterType.LONG;
            break;
        case IOtf2Constants.OTF2_TYPE_DOUBLE:
            fCounterType = CounterType.DOUBLE;
            break;
        case IOtf2Constants.OTF2_UNKNOWN_VALUE_TYPE:
        default:
            Activator.getInstance().logWarning("The counter type for this OTF2 trace is not supported."); //$NON-NLS-1$
            break;
        }
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MetricMember)) {
            return false;
        }

        MetricMember otherMetricMember = (MetricMember) other;
        return fName.equals(otherMetricMember.fName) && fMetricMode == otherMetricMember.fMetricMode
                && fMetricType == otherMetricMember.fMetricType && fFactor == otherMetricMember.fFactor
                && fUnit.equals(otherMetricMember.fUnit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fName.hashCode(), fMetricMode, fMetricType, fFactor, fUnit.hashCode());
    }

    /**
     * Gets the type of the values for this metric member
     *
     * @return the type of the values for this metric member
     */
    public int getValueType() {
        return fValueTypeCode;
    }

    /**
     * Gets the description for this metric
     *
     * @return the description for this metric
     */
    public String getDescription() {
        return fDescription;
    }

    /**
     * Gets the unit for this metric
     *
     * @return the unit for this metric
     */
    public String getUnit() {
        return fUnit;
    }

    /**
     * Gets the mode for this metric
     *
     * @return the mode for this metric
     */
    public int getMode() {
        return fMetricMode;
    }

    /**
     * Gets this counter type
     *
     * @return this counter type
     */
    public CounterType getCounterType() {
        return fCounterType;
    }
}
