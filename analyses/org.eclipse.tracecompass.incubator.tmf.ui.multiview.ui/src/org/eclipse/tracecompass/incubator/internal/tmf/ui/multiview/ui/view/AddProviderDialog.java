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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
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
 *
 */
public class AddProviderDialog extends Dialog {

    private static final ProviderType[] TYPES = new ProviderType[2];
    static {
        TYPES[0] = ProviderType.TREE_TIME_XY;
        TYPES[1] = ProviderType.TIME_GRAPH;
    }

    private final ITmfTrace fTrace;
    private java.util.List<String> fIds = new ArrayList<>();
    private List fList;
    private String fProviderId;
    private ProviderType fProviderType;

    /**
     * Constructor.
     *
     * @param parentShell
     *            parent shell
     * @param trace
     *            trace to pick data provider for
     */
    public AddProviderDialog(Shell parentShell, ITmfTrace trace) {
        super(parentShell);
        fTrace = trace;
    }

    /**
     * Retrieves the ID selected by the user.
     *
     * @return the ID of the selected provider
     */
    @Nullable
    public String getProviderId() {
        return fProviderId;
    }

    public ProviderType getProviderType() {
        return fProviderType;
    }

    @Override
    protected Control createDialogArea(Composite p) {
        p.getShell().setText(Messages.Dialog_AddDataProviderName);
        Composite parent = (Composite) super.createDialogArea(p);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 12;
        parent.setLayout(layout);

        Label label = new Label(parent, SWT.NONE);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        label.setText(Messages.Dialog_ListLabel);

        Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Can add SWT.MULTI here
        fList = new List(parent, SWT.BORDER | SWT.V_SCROLL);
        fList.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
        fList.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> checkOK()));
        populateCombo(combo);
        populateProvidersList(TYPES[0]);
        fProviderType = TYPES[0];

        return parent;
    }

    private void checkOK() {
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton == null) {
            return;
        }
        if (fList.getSelectionCount() > 0) {
            fProviderId = fIds.get(fList.getSelectionIndices()[0]);
            okButton.setEnabled(true);
        } else {
            fProviderId = null;
            okButton.setEnabled(false);
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    private void populateCombo(Combo combo) {
        combo.add(Messages.ProviderType_XY);
        combo.add(Messages.ProviderType_TimeGraph);
        combo.setText(Messages.ProviderType_XY);
        combo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            populateProvidersList(TYPES[combo.getSelectionIndex()]);
            fProviderType = TYPES[combo.getSelectionIndex()];
        }));
    }

    private void populateProvidersList(ProviderType type) {
        fList.removeAll();
        fIds.clear();
        java.util.List<IDataProviderDescriptor> descriptors = DataProviderManager.getInstance().getAvailableProviders(fTrace);
        for (IDataProviderDescriptor descriptor : descriptors) {
            if (!descriptor.getType().equals(type)) {
                continue;
            }
            fList.add(String.format("%s (%s)", descriptor.getName(), descriptor.getType().name())); //$NON-NLS-1$
            fIds.add(descriptor.getId());
        }
        checkOK();
    }

}
