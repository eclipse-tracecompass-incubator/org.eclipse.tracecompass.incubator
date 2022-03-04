/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.flows;

/**
 * Interface to be implemented by the different "nodes" in the flows analysis.
 * These nodes are the locations, location groups and system tree nodes.
 *
 * @author Yoann Heitz
 */
public interface IFlowsNode {

    /**
     * In string for input flows related features
     */
    String INPUT = "In"; //$NON-NLS-1$

    /**
     * Out string for output flows related features
     */
    String OUTPUT = "Out"; //$NON-NLS-1$

    /**
     * Integer representing an integer ID
     */
    int UNKNOWN_ID = -1;

    /**
     * Gets the input quark associated to this node
     *
     * @return the input quark of the node implementing this interface
     */
    int getInputQuark();

    /**
     * Gets the output quark associated to this node
     *
     * @return the output quark of the node implementing this interface
     */
    int getOutputQuark();

    /**
     * Return the correct quark depending on a direction
     *
     * @param direction
     *            the direction of the communication. It should be the INPUT or
     *            OUTPUT string
     * @return the input or output quark of the node, or UNKNOWN_ID if the
     *         direction was not OUTPUT or INPUT
     */
    default int getQuark(String direction) {
        if (direction.equals(INPUT)) {
            return getInputQuark();
        } else if (direction.equals(OUTPUT)) {
            return getOutputQuark();
        } else {
            return UNKNOWN_ID;
        }
    }
}
