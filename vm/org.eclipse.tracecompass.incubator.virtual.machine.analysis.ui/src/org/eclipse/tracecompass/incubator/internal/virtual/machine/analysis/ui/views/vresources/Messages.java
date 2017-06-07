/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vresources;

import org.eclipse.osgi.util.NLS;

/**
 * Messages for the Virtual Resources view
 *
 * @author Cédric Biancheri
 */
@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$

    public static String FusedVMView_TooltipVirtualMachine;
    public static String FusedVMView_TooltipVirtualCpu;
    public static String FusedVMView_TooltipRecVtid;
    public static String FusedVMView_TooltipRecContainer;

    public static String FusedVMView_stateTypeName;
    public static String FusedVMView_multipleStates;
    public static String FusedVMView_nextResourceActionNameText;
    public static String FusedVMView_nextResourceActionToolTipText;
    public static String FusedVMView_previousResourceActionNameText;
    public static String FusedVMView_previousResourceActionToolTipText;
    public static String FusedVMView_attributeCpuName;
    public static String FusedVMView_attributeIrqName;
    public static String FusedVMView_attributeSoftIrqName;
    public static String FusedVMView_attributeHoverTime;
    public static String FusedVMView_attributeTidName;
    public static String FusedVMView_attributeProcessName;
    public static String FusedVMView_attributeSyscallName;
    public static String FusedVMView_selectMachineText;
    public static String FusedVMView_ButtonMachineSelected;
    public static String FusedVMView_ButtonCPUSelected;
    public static String FusedVMView_ButtonProcessSelected;
    public static String FusedVMView_ButtonHoverProcessSelectedTID;
    public static String FusedVMView_SelectMachineActionNameText;
    public static String FusedVMView_SelectMachineActionToolTipText;
    public static String FusedVMView_ButtonContainerSelected;
    public static String FusedVMView_ContainersEntry;
    public static String FusedVMView_VirtualMachinesEntry;
    public static String FusedVMView_PhysicalCpusEntry;

    public static String SelectMachineDialog_Title;
    public static String SelectMachineDialog_SelectMachineMessage;
    public static String SelectMachineDialog_CpuText;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
