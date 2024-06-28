/**********************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis;

import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Represents every event handler to store those in maps and used across all
 * analysis to handle events.
 *
 * @author Arnaud Fiorini
 */
public interface IDpdkEventHandler {

    /**
     * @param ssb
     *            The state system builder
     * @param event
     *            The event to handle
     */
    public void handleEvent(ITmfStateSystemBuilder ssb, ITmfEvent event);

    /**
     * The text fields in Dpdk traces are arrays of 32 integers that each
     * represents a character each character is parsed into a separate field
     * value in Trace Compass. This function extracts and return the String from
     * those types of field.
     *
     * @param event
     *            The event that contain the text field
     * @param fieldName
     *            The field name of the text field
     * @return The resulting string
     */
    public static String getStringFieldValue(ITmfEvent event, String fieldName) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            String charFieldName = fieldName + "." + fieldName + "[" + String.valueOf(i) + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            Integer character = event.getContent().getFieldValue(Integer.class, charFieldName);
            if (character == null || Character.isWhitespace(character)) {
                break;
            }
            value.appendCodePoint(character);
        }
        return value.toString();
    }
}
