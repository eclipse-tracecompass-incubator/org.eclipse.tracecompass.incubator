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

package org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.nebula.widgets.opal.duallist.DLItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Dialog box for listing and selecting a data provider for the
 * {@link MultiView}.
 *
 * @author Ivan Grinenko
 */
public class SetProviderDialog extends Dialog {

    private static final ProviderType[] TYPES = new ProviderType[2];
    static {
        TYPES[0] = ProviderType.TREE_TIME_XY;
        TYPES[1] = ProviderType.TIME_GRAPH;
    }

    private final ITmfTrace fTrace;
    private DualSelectionList fSelector;
    private List<IDataProviderDescriptor> fDescriptors = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param parentShell
     *            parent shell
     * @param trace
     *            trace to pick data providers' descriptors for
     * @param currentDescriptors
     *            descriptors that are selected
     */
    public SetProviderDialog(Shell parentShell, ITmfTrace trace, List<IDataProviderDescriptor> currentDescriptors) {
        super(parentShell);
        fTrace = trace;
        fDescriptors.addAll(currentDescriptors);
    }

    /**
     * Get descriptors selected by a user.
     *
     * @return List of IDataProviderDescriptor.
     */
    public List<IDataProviderDescriptor> getDescriptors() {
        return fDescriptors;
    }

    @Override
    protected Control createDialogArea(Composite p) {
        p.getShell().setText(Messages.Dialog_SetDataProviderName);
        Composite parent = (Composite) super.createDialogArea(p);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 12;
        parent.setLayout(layout);

        fSelector = new DualSelectionList(parent, SWT.BORDER);
        fSelector.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, true, 2, 1));
        fSelector.addSelectionChangeListener(e -> checkOK());
        populateProvidersList();

        return parent;
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control c = super.createButtonBar(parent);
        checkOK();
        return c;
    }

    @Override
    protected void okPressed() {
        checkOK();
        super.okPressed();
    }

    private void checkOK() {
        fDescriptors.clear();
        fDescriptors.addAll(fSelector.getSelected().stream().map(
                e -> (IDataProviderDescriptor) e.getData()).collect(Collectors.toList()));
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton == null) {
            return;
        }
        okButton.setEnabled(fDescriptors.size() > 0);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    private void populateProvidersList() {
        List<IDataProviderDescriptor> descriptors = DataProviderManager.getInstance().getAvailableProviders(fTrace);
        // Fill the left side
        fSelector.setItems(descriptors2DLItems(descriptors));
        // Fill the right side
        fSelector.setSelected(descriptors2DLItems(fDescriptors));
    }

    private static List<DLItem> descriptors2DLItems(List<IDataProviderDescriptor> descriptors) {
        return descriptors
                .stream()
                .filter(elem -> Arrays.asList(TYPES).contains(elem.getType()))
                .map(e -> descriptor2DLItem(e))
                .collect(Collectors.toList());
    }

    private static DLItem descriptor2DLItem(IDataProviderDescriptor e) {
        DLItem ret = new DLItem(String.format("%s (%s)", e.getName(), e.getType().name())); //$NON-NLS-1$
        ret.setData(e);
        // TODO: Add images here.
        return ret;
    }

}
