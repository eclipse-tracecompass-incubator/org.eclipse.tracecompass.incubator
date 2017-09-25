/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.ui.handlers;

import org.eclipse.osgi.util.NLS;

/**
 * Externalized messages for the XaF analysis module package
 *
 * @author Raphaël Beamonte
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$

    /**
     * Title of the parameters dialog for the eXtended Analysis Feature
     */
    public static String ManageXaFParametersDialog_XaFParameters;

    /**
     * Text of the radio button used to provide a model
     */
    public static String ManageXaFParametersDialog_ButtonProvideModel;

    /**
     * Text of the radio button used to select a model
     */
    public static String ManageXaFParametersDialog_ButtonGenerateModel;

    /**
     * Label of the file chooser
     */
    public static String ManageXaFParametersDialog_FileChooserLabel;

    /**
     * Title of the file chooser dialog
     */
    public static String ManageXaFParametersDialog_FileChooserDialogTitle;

    /**
     * Title of the group in which we select the variable types to use
     */
    public static String ManageXaFParametersDialog_VariablesGroup;

    /**
     * Default text on the right of the variable group
     */
    public static String ManageXaFParametersDialog_VariablesLabelDefault;

    /**
     * Text that appears in the variable group when the
     * deadline type is selected
     */
    public static String ManageXaFParametersDialog_VariablesLabelDeadline;

    /**
     * Text that appears in the variable group when the
     * preempt type is selected
     */
    public static String ManageXaFParametersDialog_VariablesLabelPreempt;

    /**
     * Text that appears in the variable group when the
     * syscalls type is selected
     */
    public static String ManageXaFParametersDialog_VariablesLabelSyscalls;

    /**
     * Text that appears in the variable group when the
     * cputime type is selected
     */
    public static String ManageXaFParametersDialog_VariablesLabelCputime;

    /**
     * Title of the group in which we select the time ranges to use
     */
    public static String ManageXaFParametersDialog_TimeRangesGroup;

    /**
     * Label of the starting time range
     */
    public static String ManageXaFParametersDialog_TimeRangesLabelFrom;

    /**
     * Label of the ending time range
     */
    public static String ManageXaFParametersDialog_TimeRangesLabelTo;

    /**
     * Label of the button to click in order to add the time range
     */
    public static String ManageXaFParametersDialog_TimeRangesButtonAdd;

    /**
     * Label of the button to click in order to remove the
     * selected time ranges
     */
    public static String ManageXaFParametersDialog_TimeRangesButtonRemove;

    /**
     * Label of the button to click in order to clear all the time ranges
     */
    public static String ManageXaFParametersDialog_TimeRangesButtonClear;

    /**
     * Error message shown when adding an invalid interval
     */
    public static String ManageXaFParametersDialog_TimeRangesErrorInvalidInterval;

    /**
     * Error message shown when adding an interval that's already in
     * the list
     */
    public static String ManageXaFParametersDialog_TimeRangesErrorIntervalAlreadyAdded;

    /**
     * Error message shown when a model location is not provided
     * while the provide model radio button is selected
     */
    public static String ManageXaFParametersDialog_ProvideModelErrorNoModelLocation;

    /**
     * Error message shown when a model location is not valid while
     * the provide model radio button is selected
     */
    public static String ManageXaFParametersDialog_ProvideModelErrorModelLocationDoesNotExist;

    /**
     * Confirmation dialog message shown when a model location is not
     * provided while the generate model radio button is selected
     */
    public static String ManageXaFParametersDialog_GenerateModelErrorNoModelLocationDialog;

    /**
     * Error message shown when a model location is not provided and a
     * temporary file could not be created while the generate model radio
     * button is selected
     */
    public static String ManageXaFParametersDialog_GenerateModelErrorCannotCreateTempFile;

    /**
     * Error message shown when a model location is a directory and thus
     * cannot be written to as a file while the generate model radio
     * button is selected
     */
    public static String ManageXaFParametersDialog_GenerateModelErrorModelLocationIsDir;

    /**
     * Error message shown when a model location is not writeable while
     * the generate model radio button is selected
     */
    public static String ManageXaFParametersDialog_GenerateModelErrorModelLocationNotWriteable;

    /**
     * Error message shown when a model location already exists and will
     * be overwritten while the generate model radio button is selected
     */
    public static String ManageXaFParametersDialog_GenerateModelErrorOverwriteFileDialog;

    /**
     * Text of the check button used to consider all instances as valid
     */
    public static String ManageXaFParametersDialog_ButtonAllInstancesValid;

    /**
     * Text of the check button used to check a model
     */
    public static String ManageXaFParametersDialog_ButtonCheckModel;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
