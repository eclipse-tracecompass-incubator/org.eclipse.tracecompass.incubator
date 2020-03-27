/**********************************************************************
 * Copyright (c) 2020 Draeger, Auriga
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.timegraph;

import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.tracecompass.internal.tmf.core.markers.MarkerConfigXmlParser;
import org.eclipse.tracecompass.internal.tmf.core.markers.MarkerSet;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.markers.MarkerUtils;
import org.eclipse.tracecompass.tmf.core.signal.TmfMarkerEventSourceUpdatedSignal;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.Messages;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * {@link BaseDataProviderTimeGraphMultiViewer} with additional context menu
 * items. These items are actions from the toolbar of
 * {@link BaseDataProviderTimeGraphView} which are applicable only for the
 * viewer not the whole view. For example, visibility of labels or grid lines.
 *
 * @author Ivan Grinenko
 *
 */
@SuppressWarnings("restriction")
public class ActionsDataProviderTimeGraphMultiViewer extends BaseDataProviderTimeGraphMultiViewer {

    private static final String HIDE_LABELS_KEY = "hide.labels"; //$NON-NLS-1$

    /** The marker set menu */
    private MenuManager fMarkerSetMenu;

    /**
     * Constructor.
     *
     * @param parent
     *            parent composite
     * @param pres
     *            presentation provider
     * @param site
     *            workbench site
     * @param providerId
     *            data provider's ID
     */
    public ActionsDataProviderTimeGraphMultiViewer(Composite parent, ITimeGraphPresentationProvider pres,
            IWorkbenchPartSite site, String providerId) {
        super(parent, pres, site, providerId);
    }

    @Override
    protected void fillTimeEventContextMenu(@NonNull IMenuManager menuManager) {
        TimeGraphViewer timeGraphViewer = getTimeGraphViewer();
        menuManager.add(timeGraphViewer.getShowFilterDialogAction());
        menuManager.add(timeGraphViewer.getShowLegendAction());
        menuManager.add(new Separator());
        menuManager.add(timeGraphViewer.getGridlinesMenu());
        menuManager.add(getShowLabelsAction());
        menuManager.add(timeGraphViewer.getMarkersMenu());
        menuManager.add(getMarkerSetMenu());
        super.fillTimeEventContextMenu(menuManager);
    }

    /**
     * Get the marker set menu
     *
     * @return the menu manager object
     */
    protected MenuManager getMarkerSetMenu() {
        if (fMarkerSetMenu != null) {
            return fMarkerSetMenu;
        }
        fMarkerSetMenu = new MenuManager(Messages.AbstractTimeGraphView_MarkerSetMenuText);
        fMarkerSetMenu.setRemoveAllWhenShown(true);
        fMarkerSetMenu.addMenuListener(mgr -> {
            Action noneAction = new MarkerSetAction(null);
            MarkerSet defaultMarkerSet = MarkerUtils.getDefaultMarkerSet();
            String defaultMarkerSetId = (defaultMarkerSet == null) ? null : defaultMarkerSet.getId();
            noneAction.setChecked(defaultMarkerSetId == null);
            mgr.add(noneAction);
            List<MarkerSet> markerSets = MarkerConfigXmlParser.getMarkerSets();
            for (MarkerSet markerSet : markerSets) {
                Action action = new MarkerSetAction(markerSet);
                action.setChecked(markerSet.getId().equals(defaultMarkerSetId));
                mgr.add(action);
            }
            mgr.add(new Separator());
            mgr.add(new Action(Messages.AbstractTimeGraphView_MarkerSetEditActionText) {
                @Override
                public void run() {
                    MarkerConfigXmlParser.initMarkerSets();
                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    IFileStore fileStore = EFS.getLocalFileSystem().getStore(MarkerConfigXmlParser.MARKER_CONFIG_PATH);
                    try {
                        IDE.openEditorOnFileStore(page, fileStore);
                    } catch (PartInitException e) {
                        Activator.getDefault().logError("Error opening editor on " + MarkerConfigXmlParser.MARKER_CONFIG_PATH, e); //$NON-NLS-1$
                    }
                }
            });
        });
        return fMarkerSetMenu;
    }

    /**
     * Returns an action that toggles the display of labels
     *
     * @return the action
     * @since 5.3
     */
    protected Action getShowLabelsAction() {
        final Action showLabelsAction = new Action(Messages.AbstractTimeGraphView_ShowLabelsActionText, IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                boolean showLabels = isChecked();
                getTimeGraphViewer().setLabelsVisible(showLabels);
                redraw();
                IDialogSettings dialogSettings = getDialogSettings(true);
                dialogSettings.put(HIDE_LABELS_KEY, !showLabels);
            }
        };
        boolean showLabels = true;
        IDialogSettings dialogSettings = getDialogSettings(false);
        if (dialogSettings != null) {
            showLabels = !dialogSettings.getBoolean(HIDE_LABELS_KEY);
        }
        showLabelsAction.setChecked(showLabels);
        getTimeGraphViewer().setLabelsVisible(showLabels);
        return showLabelsAction;
    }

    /**
     * Get the dialog settings for this view
     *
     * @param force
     *            true to create the section if it doesn't exist
     * @return the dialog settings
     */
    private IDialogSettings getDialogSettings(boolean force) {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        String sectionName = getSite().getId();
        IDialogSettings section = settings.getSection(sectionName);
        if (section == null && force) {
            section = settings.addNewSection(sectionName);
        }
        return section;
    }

    private class MarkerSetAction extends Action {

        private MarkerSet fMarkerSet;

        public MarkerSetAction(MarkerSet markerSet) {
            super(markerSet == null ? Messages.AbstractTimeGraphView_MarkerSetNoneActionText : markerSet.getName(), IAction.AS_RADIO_BUTTON);
            fMarkerSet = markerSet;
        }

        @Override
        public void runWithEvent(Event event) {
            if (isChecked()) {
                MarkerUtils.setDefaultMarkerSet(fMarkerSet);
                broadcast(new TmfMarkerEventSourceUpdatedSignal(ActionsDataProviderTimeGraphMultiViewer.this));
            }
        }
    }

}
