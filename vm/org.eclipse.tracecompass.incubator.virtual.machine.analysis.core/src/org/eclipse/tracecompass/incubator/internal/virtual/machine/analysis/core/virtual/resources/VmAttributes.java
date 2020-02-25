/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Mohamad Gebai - Initial API and implementation
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources;

/**
 * Attributes used by the VM Analysis
 *
 * @author Mohamad Gebai
 */
@SuppressWarnings({"nls"})
public interface VmAttributes {

    /** First-level attributes */
    String VIRTUAL_MACHINES = "Virtual Machines";

    /** Sub-attributes for virtual CPUs */
    String STATUS = "Status";

}
