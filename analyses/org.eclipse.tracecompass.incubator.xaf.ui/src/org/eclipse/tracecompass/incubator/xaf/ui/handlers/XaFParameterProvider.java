/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.ui.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisParamProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/**
 * Parameter provider for the eXtended Analysis Feature
 *
 * @author Raphaël Beamonte
 */
public class XaFParameterProvider extends TmfAbstractAnalysisParamProvider {

    private static final String CONFIG_FILENAME = "xaf.properties"; //$NON-NLS-1$

    /**
     * Separator used when a property has multiple entries
     */
    public static final @NonNull String PROPERTY_SEPARATOR = ";"; //$NON-NLS-1$

    /**
     * Name of the key used to store the model provided property
     */
    public static final @NonNull String PROPERTY_MODEL_PROVIDED = "ModelProvided"; //$NON-NLS-1$

    /**
     * Name of the key used to store the model location property
     */
    public static final @NonNull String PROPERTY_MODEL_LOCATION = "ModelLocation"; //$NON-NLS-1$

    /**
     * Name of the key used to store the model location history property
     */
    public static final @NonNull String PROPERTY_MODEL_LOCATION_HISTORY = "ModelLocationHistory"; //$NON-NLS-1$

    /**
     * Name of the key used to store the selected variables property
     */
    public static final @NonNull String PROPERTY_SELECTED_VARIABLES = "SelectedVariables"; //$NON-NLS-1$

    /**
     * Name of the key used to store the all instances valid property
     */
    public static final @NonNull String PROPERTY_ALL_INSTANCES_VALID = "AllInstancesValid"; //$NON-NLS-1$

    /**
     * Name of the key used to store the selected time ranges property
     */
    public static final @NonNull String PROPERTY_SELECTED_TIMERANGES = "SelectedTimeRanges"; //$NON-NLS-1$

    /**
     * Separator used between start and end time of a selected time range
     */
    public static final @NonNull String PROPERTY_SELECTED_TIMERANGES_SEPARATOR = ","; //$NON-NLS-1$

    /**
     * Name of the key used to store the check model property
     */
    public static final @NonNull String PROPERTY_CHECK_MODEL = "CheckModel"; //$NON-NLS-1$

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    private static @Nullable File getConfigFile() {
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace == null) {
            return null;
        }
        String directory = TmfTraceManager.getSupplementaryFileDir(trace);
        File configFile = new File(directory + CONFIG_FILENAME);
        return configFile;
    }

    private static Properties getProperties() {
        Properties prop = new Properties();

        File configFile = getConfigFile();
        if (configFile != null) {
            try (InputStream inputStream = new FileInputStream(configFile)) {
                prop.load(inputStream);
                return prop;
            } catch (IOException e) {
            }
        }

        return null;
    }

    private static void saveProperties(Properties prop) {
        File configFile = getConfigFile();
        if (configFile != null) {
            try (OutputStream output = new FileOutputStream(configFile)) {
                prop.store(output, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Simple class to store a dialog in order for it to be accessible outside
     * of Runnable objects
     *
     * @author Raphaël Beamonte
     */
    public static class StoreDialog {
        /**
         * The dialog stored in this StoreDialog
         */
        public Dialog dialog;
    }

    @Override
    public Object getParameter(String name) {
        if (name.equals("parameters")) { //$NON-NLS-1$
            final StoreDialog storeDialog = new StoreDialog();
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    storeDialog.dialog = new ManageXaFParametersDialog(Display.getDefault().getActiveShell(), getProperties());
                    if (storeDialog.dialog.open() == IDialogConstants.OK_ID) {
                        saveProperties(((ManageXaFParametersDialog) storeDialog.dialog).getParameters());
                    }
                }
            });

            return ((ManageXaFParametersDialog) storeDialog.dialog).getParameters();
        }
        return null;
    }

    @Override
    public boolean appliesToTrace(ITmfTrace trace) {
        return false;
    }

}
