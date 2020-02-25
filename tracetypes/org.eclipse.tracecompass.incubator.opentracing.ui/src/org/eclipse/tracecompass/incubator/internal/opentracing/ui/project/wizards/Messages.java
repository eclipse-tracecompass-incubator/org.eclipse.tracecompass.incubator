/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.project.wizards;

import org.eclipse.osgi.util.NLS;

/**
 * Message bundle for dialog messages.
 */
@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.opentracing.ui.project.wizards.messages"; //$NON-NLS-1$
    public static String FetchJaegerTracesWizardPage_importNotAvailable;
    public static String FetchJaegerTracesWizardPage_apiBaseUrlLabel;
    public static String FetchJaegerTracesWizardPage_deselectAllButton;
    public static String FetchJaegerTracesWizardPage_fetchJaegerShellTitle;
    public static String FetchJaegerTracesWizardPage_jaegerConfigGroup;
    public static String FetchJaegerTracesWizardPage_jaegerFetchButton;
    public static String FetchJaegerTracesWizardPage_lookbackLabel;
    public static String FetchJaegerTracesWizardPage_maxDurationLabel;
    public static String FetchJaegerTracesWizardPage_minDurationLabel;
    public static String FetchJaegerTracesWizardPage_nbSpansColumnName;
    public static String FetchJaegerTracesWizardPage_nbTracesLimitLabel;
    public static String FetchJaegerTracesWizardPage_selectAllButton;
    public static String FetchJaegerTracesWizardPage_serviceNameLabel;
    public static String FetchJaegerTracesWizardPage_spanNameColumnName;
    public static String FetchJaegerTracesWizardPage_servicesColumnName;
    public static String FetchJaegerTracesWizardPage_tagsLabel;
    public static String FetchJaegerTracesWizardPage_traceName;
    public static String FetchJaegerTracesWizardPage_traceIdColumnName;
    public static String FetchJaegerTracesWizardPage_tracesGroup;
    public static String FetchJaegerTracesWizardPage_errorApiConnection;
    public static String FetchJaegerTracesWizardPage_errorFetchTraces;
    public static String FetchJaegerTracesWizardPage_errorFileName;
    public static String FetchJaegerTracesWizardPage_errorNoTracesFound;
    public static String FetchJaegerTracesWizardPage_importDestinationLabel;
    public static String FetchJaegerTracesWizardPage_wizardDescriptionMessage;
    public static String FetchJaegerTracesWizardPage_wizardPageName;
    public static String FetchJaegerTraceWizard_wizardTitle;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
