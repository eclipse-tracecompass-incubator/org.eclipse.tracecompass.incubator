/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.views;

import org.eclipse.tracecompass.incubator.internal.opentracing.ui.view.spanlife.SpanLifeView;
import org.eclipse.tracecompass.tmf.ui.project.wizards.NewTmfProjectWizard;
import org.eclipse.tracecompass.tmf.ui.views.histogram.HistogramView;
import org.eclipse.tracecompass.tmf.ui.views.statistics.TmfStatisticsView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Perspective for open tracing traces
 *
 * @author Katherine Nadeau
 *
 */
public class OpenTracingPerspectiveFactory implements IPerspectiveFactory {

    /** Perspective ID */
    public static final String ID = "org.eclipse.tracecompass.incubator.opentracing.ui.perspective"; //$NON-NLS-1$

    // Open tracing views
    private static final String SPAN_LIFE_VIEW_ID = SpanLifeView.ID;
    private static final String HISTOGRAM_VIEW_ID = HistogramView.ID;
    private static final String STATISTICS_VIEW_ID = TmfStatisticsView.ID;

    // Standard Eclipse views
    private static final String PROJECT_VIEW_ID = IPageLayout.ID_PROJECT_EXPLORER;
    private static final String PROPERTIES_VIEW_ID = IPageLayout.ID_PROP_SHEET;

    @Override
    public void createInitialLayout(IPageLayout layout) {

        layout.setEditorAreaVisible(true);

        // Create the top left folder
        IFolderLayout topLeftFolder = layout.createFolder(
                "topLeftFolder", IPageLayout.LEFT, 0.20f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
        topLeftFolder.addView(PROJECT_VIEW_ID);

        // Create the top right folder
        IFolderLayout topRightFolder = layout.createFolder(
                "topRightFolder", IPageLayout.TOP, 0.40f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
        topRightFolder.addView(SPAN_LIFE_VIEW_ID);
        topRightFolder.addView(STATISTICS_VIEW_ID);

        // Create the bottom right folder
        IFolderLayout bottomRightFolder = layout.createFolder(
                "bottomRightFolder", IPageLayout.BOTTOM, 0.50f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
        bottomRightFolder.addView(HISTOGRAM_VIEW_ID);
        bottomRightFolder.addView(PROPERTIES_VIEW_ID);

        layout.addNewWizardShortcut(NewTmfProjectWizard.ID);
    }

}
