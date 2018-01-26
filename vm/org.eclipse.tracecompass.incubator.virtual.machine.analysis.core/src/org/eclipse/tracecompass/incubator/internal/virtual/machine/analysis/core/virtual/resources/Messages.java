/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Externalized message strings from the Fused VM Analysis
 *
 * @author Cédric Biancheri
 */
@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$

    public static @Nullable String VirtualMachineCPUAnalysis_Help;
    public static @Nullable String FusedVirtualMachineAnalysis_Help;

    public static @Nullable String FusedVMView_stateTypeName;
    public static @Nullable String FusedVMView_multipleStates;
    public static @Nullable String FusedVMView_nextResourceActionNameText;
    public static @Nullable String FusedVMView_nextResourceActionToolTipText;
    public static @Nullable String FusedVMView_previousResourceActionNameText;
    public static @Nullable String FusedVMView_previousResourceActionToolTipText;
    public static @Nullable String FusedVMView_attributeCpuName;
    public static @Nullable String FusedVMView_attributeIrqName;
    public static @Nullable String FusedVMView_attributeSoftIrqName;
    public static @Nullable String FusedVMView_attributeHoverTime;
    public static @Nullable String FusedVMView_attributeTidName;
    public static @Nullable String FusedVMView_attributeProcessName;
    public static @Nullable String FusedVMView_attributeSyscallName;
    public static @Nullable String FusedVMView_attributeVirtualMachine;
    public static @Nullable String FusedVMView_attributeVirtualCpu;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

    /**
     * Helper method to expose externalized strings as non-null objects.
     */
    public static String getMessage(@Nullable String msg) {
        if (msg == null) {
            return ""; //$NON-NLS-1$
        }
        return msg;
    }
}
