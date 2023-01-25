/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.inandout.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.incubator.internal.inandout.core.analysis.InAndOutAnalysisModule;
import org.eclipse.tracecompass.incubator.internal.inandout.core.analysis.SegmentSpecifier;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.dialog.TmfFileDialogFactory;

/**
 * Configuration dialog
 *
 * @author Matthew Khouzam
 */
public class InAndOutConfigDialog extends Dialog {

    private final List<@NonNull SegmentSpecifier> fData = new ArrayList<>();
    private ListViewer fSpecifiers = null;
    private Button fAdd = null;
    private File fPath = null;
    private ITmfTrace fTrace;
    private boolean fChanged = false;
    private static final String PREF_SAVED_OPEN_CONFIG_LOCATION = "PREF_LAST_OPEN_CONFIG_LOCATION"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param parentShell
     *            the parent shell, or null to create a top-level shell
     * @param supplementalPath
     *            the path of the trace folder
     * @param trace
     *            the trace
     */
    protected InAndOutConfigDialog(Shell parentShell, File supplementalPath, ITmfTrace trace) {
        super(parentShell);
        fPath = supplementalPath;
        fTrace = trace;
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
    }

    @Override
    protected Control createDialogArea(@Nullable Composite parent) {
        if (parent == null) {
            return null;
        }
        File file = new File(fPath.getAbsolutePath() + File.separator + InAndOutAnalysisModule.ID + InAndOutAnalysisModule.JSON);
        List<@NonNull SegmentSpecifier> read = InAndOutAnalysisModule.read(file);
        fData.addAll(read);
        Shell shell = getShell();
        shell.setText("Configure In and Out analysis"); //$NON-NLS-1$
        shell.addControlListener(resizeLayouter(shell));
        parent.addControlListener(resizeLayouter(parent));
        Composite localParent = (Composite) super.createDialogArea(parent);
        localParent.addControlListener(resizeLayouter(localParent));
        localParent.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
        localParent.setLayoutData(GridDataFactory.fillDefaults().hint(400, 340).grab(true, true).create());

        fSpecifiers = new ListViewer(localParent);
        fSpecifiers.setLabelProvider(new LabelProvider());
        fSpecifiers.setContentProvider(new ArrayContentProvider());
        fSpecifiers.setInput(fData);
        Control list = fSpecifiers.getControl();
        list.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        fSpecifiers.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(@Nullable DoubleClickEvent event) {
                edit(fSpecifiers.getSelection());
            }
        });
        Composite controls = new Composite(localParent, SWT.NONE);
        controls.setLayout(new GridLayout());
        controls.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).create());

        fAdd = new Button(controls, SWT.PUSH);
        fAdd.setEnabled(!fData.contains(InAndOutAnalysisModule.REFERENCE));
        fAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(@Nullable SelectionEvent e) {
                add(fSpecifiers);
            }
        });
        fAdd.setText("Add"); //$NON-NLS-1$

        Button edit = new Button(controls, SWT.PUSH);
        edit.setEnabled(false);
        fSpecifiers.addSelectionChangedListener(event -> edit.setEnabled(!fSpecifiers.getSelection().isEmpty()));
        edit.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(@Nullable SelectionEvent e) {
                edit(fSpecifiers.getSelection());
            }
        });
        edit.setText("Edit"); //$NON-NLS-1$

        Button remove = new Button(controls, SWT.PUSH);
        remove.setEnabled(false);
        fSpecifiers.addSelectionChangedListener(event -> remove.setEnabled(!event.getSelection().isEmpty() && fData.size() != 1));
        remove.addSelectionListener(specifierAdapter(fSpecifiers, specifier -> {
            fData.remove(specifier);
            fSpecifiers.refresh();
            fAdd.setEnabled(!fData.contains(InAndOutAnalysisModule.REFERENCE));
        }));
        remove.setText("Remove"); //$NON-NLS-1$

        Button reset = new Button(controls, SWT.PUSH);
        reset.setEnabled(false);
        fSpecifiers.addSelectionChangedListener(event -> reset.setEnabled(fData.size() > 1 || !fData.contains(InAndOutAnalysisModule.REFERENCE)));
        reset.addSelectionListener(specifierAdapter(fSpecifiers, specifier -> {
            fData.clear();
            add(fSpecifiers);
        }));
        reset.setText("Reset"); //$NON-NLS-1$

        Button importButton = new Button(controls, SWT.PUSH);
        importButton.setEnabled(true);
        importButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(@Nullable SelectionEvent e) {
                importFile();
            }
        });
        importButton.setText("Import..."); //$NON-NLS-1$

        Button exportButton = new Button(controls, SWT.PUSH);
        exportButton.setEnabled(true);
        exportButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(@Nullable SelectionEvent e) {
                exportFile();
            }
        });
        exportButton.setText("Export..."); //$NON-NLS-1$

        Button up = new Button(controls, SWT.PUSH);
        up.setEnabled(false);
        fSpecifiers.addSelectionChangedListener(event -> up.setEnabled(!event.getSelection().isEmpty() && fData.size() != 1));
        up.addSelectionListener(specifierAdapter(fSpecifiers, specifier -> {
            int index = fData.indexOf(specifier);
            @NonNull
            SegmentSpecifier temp = fData.get(index - 1);
            fData.set(index - 1, specifier);
            fData.set(index, temp);
            fSpecifiers.refresh();
        }));
        up.setText("^"); //$NON-NLS-1$

        Button down = new Button(controls, SWT.PUSH);
        down.setEnabled(false);
        fSpecifiers.addSelectionChangedListener(event -> down.setEnabled(!event.getSelection().isEmpty() && fData.size() != 1));
        down.addSelectionListener(specifierAdapter(fSpecifiers, specifier -> {
            int index = fData.indexOf(specifier);
            @NonNull
            SegmentSpecifier temp = fData.get(index + 1);
            fData.set(index + 1, specifier);
            fData.set(index, temp);
            fSpecifiers.refresh();
        }));
        down.setText("v"); //$NON-NLS-1$
        localParent.pack();
        return localParent;
    }

    private void exportFile() {
        FileDialog dialog = TmfFileDialogFactory.create(Display.getCurrent().getActiveShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[] { "*.json" }); //$NON-NLS-1$
        loadFileDialogSettings(dialog);
        String selectedFileName = dialog.open();
        if (selectedFileName != null) {
            File fileName = new File(selectedFileName);
            InAndOutAnalysisModule.write(fileName, fData);
            saveFileDialogSettings(fileName.getParent());
        }
    }

    private void importFile() {
        FileDialog dialog = TmfFileDialogFactory.create(Display.getCurrent().getActiveShell(), SWT.OPEN);
        dialog.setFilterExtensions(new String[] { "*.json"}); //$NON-NLS-1$
        loadFileDialogSettings(dialog);
        String selectedFileName = dialog.open();
        if (selectedFileName != null) {
            File fileName = new File (selectedFileName);
            List<@NonNull SegmentSpecifier> read = InAndOutAnalysisModule.read(fileName);
            fData.addAll(read);
            fSpecifiers.refresh();
            saveFileDialogSettings(fileName.getParent());
        }
    }

    private static void loadFileDialogSettings(FileDialog fd) {
        IEclipsePreferences defaultPreferences = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        String lastLocation = defaultPreferences.get(PREF_SAVED_OPEN_CONFIG_LOCATION, null);
        if (lastLocation != null && !lastLocation.isEmpty()) {
            File parentFile = new File(lastLocation).getParentFile();
            if (parentFile != null && parentFile.exists()) {
                fd.setFilterPath(parentFile.toString());
            }
        }
    }

    private static void saveFileDialogSettings(String filePath) {
        if (filePath == null) {
            return;
        }

        InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).put(PREF_SAVED_OPEN_CONFIG_LOCATION, filePath);
    }

    private static ControlListener resizeLayouter(Composite composite) {
        return new ControlListener() {

            @Override
            public void controlResized(ControlEvent e) {
                composite.layout();
            }

            @Override
            public void controlMoved(ControlEvent e) {
                composite.layout();
            }
        };
    }

    private void add(ListViewer specifiers) {
        fData.add(new SegmentSpecifier(InAndOutAnalysisModule.REFERENCE));
        specifiers.refresh();
        fAdd.setEnabled(false);
    }

    private void edit(ISelection selection) {
        Object first = ((StructuredSelection) selection).getFirstElement();
        SegmentSpecifier specifier = (SegmentSpecifier) first;
        SegmentSpecifierDialog dialog = new SegmentSpecifierDialog(getParentShell(), specifier, fTrace);
        dialog.open();
        if (dialog.isUpdated()) {
            fSpecifiers.refresh();
            fAdd.setEnabled(!fData.contains(InAndOutAnalysisModule.REFERENCE));
        }
    }

    private static SelectionAdapter specifierAdapter(ListViewer specifiers, Consumer<@NonNull SegmentSpecifier> ss) {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(@Nullable SelectionEvent e) {
                ISelection selection = specifiers.getSelection();
                Object firstElement = ((StructuredSelection) selection).getFirstElement();
                if (firstElement != null) {
                    ss.accept((SegmentSpecifier) firstElement);
                }
            }
        };
    }

    /**
     * Has the dataset changed
     *
     * @return has the dataset changed?
     */
    public boolean isChanged() {
        return fChanged;
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == Window.OK) {
            File file = new File(fPath.getAbsolutePath() + File.separator + InAndOutAnalysisModule.ID + InAndOutAnalysisModule.JSON);
            fChanged = !(Objects.equals(InAndOutAnalysisModule.read(file), fData));
            if (fChanged) {
                InAndOutAnalysisModule.write(file, fData);
                File stateSystemFile = new File(fPath.getAbsolutePath() + File.separator + InAndOutAnalysisModule.ID + ".ht"); //$NON-NLS-1$
                if (stateSystemFile.exists()) {
                    try {
                        Files.delete(stateSystemFile.toPath());
                    } catch (IOException e) {
                        Activator activator = Activator.getDefault();
                        if (activator == null) {
                            throw new IllegalStateException("Activator should not be null here, IO exception caught", e); //$NON-NLS-1$
                        }
                        activator.getLog().log(new Status(IStatus.ERROR, activator.getBundle().getSymbolicName(),
                                "Failed to delete file: " + stateSystemFile.getAbsolutePath(), e)); //$NON-NLS-1$
                    }
                }
            }
        }
        super.buttonPressed(buttonId);
    }
}
