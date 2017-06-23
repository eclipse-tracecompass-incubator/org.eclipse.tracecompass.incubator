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

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.incubator.internal.inandout.core.analysis.SegmentSpecifier;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Segment creator dialog
 *
 * @author Matthew Khouzam
 */
public class SegmentSpecifierDialog extends Dialog {

    private final SegmentSpecifier fSegmentSpecifier;
    private boolean[] fValid = { true, true, true, true };
    private @Nullable String fLabel = null;

    private void setLabel(String value) {
        fLabel = value;
    }

    private void setInRegex(String value) {
        try {
            Pattern.compile(value);
            fInRegex = value;
            fValid[0] = true;
        } catch (PatternSyntaxException e) {
            fValid[0] = false;
        }
    }

    private void setOutRegex(String value) {
        try {
            Pattern.compile(value);
            fOutRegex = value;
            fValid[1] = true;
        } catch (PatternSyntaxException e) {
            fValid[1] = false;
        }
    }

    private void setContextInRegex(String value) {
        try {
            Pattern.compile(value);
            fContextInRegex = value;
            fValid[2] = true;
        } catch (PatternSyntaxException e) {
            fValid[2] = false;
        }
    }

    private void setContextOutRegex(String value) {
        try {
            Pattern.compile(value);
            fContextOutRegex = value;
            fValid[3] = true;
        } catch (PatternSyntaxException e) {
            fValid[3] = false;
        }
    }

    private void setCategory(String category) {
        fCategory = category;
    }

    private @Nullable String fInRegex = null;
    private @Nullable String fOutRegex = null;
    private @Nullable String fContextInRegex = null;
    private @Nullable String fContextOutRegex = null;
    private @Nullable String fCategory = null;
    private ITmfTrace fRef;
    private boolean fUpdated = false;

    /**
     * Segment specifier dialog
     *
     * @param parentShell
     *            the parent shell
     * @param segmentSpecifier
     *            the specifier to update
     * @param ref
     *            the reference trace to get classifiers from
     */
    protected SegmentSpecifierDialog(Shell parentShell, SegmentSpecifier segmentSpecifier, ITmfTrace ref) {
        super(parentShell);
        fSegmentSpecifier = segmentSpecifier;
        fRef = ref;
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        if (parent == null || fRef == null) {
            return null;
        }
        Shell shell = getShell();
        shell.setText("Configure In and Out analysis"); //$NON-NLS-1$
        Composite localParent = (Composite) super.createDialogArea(parent);
        localParent.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
        localParent.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        createEntry(localParent, "Label", fSegmentSpecifier::getLabel, this::setLabel); //$NON-NLS-1$
        createEntry(localParent, "InRegex", fSegmentSpecifier::getInRegex, this::setInRegex); //$NON-NLS-1$
        createEntry(localParent, "OutRegex", fSegmentSpecifier::getOutRegex, this::setOutRegex); //$NON-NLS-1$
        createEntry(localParent, "ContextIn", fSegmentSpecifier::getContextInRegex, this::setContextInRegex); //$NON-NLS-1$
        createEntry(localParent, "ContextOut", fSegmentSpecifier::getContextOutRegex, this::setContextOutRegex); //$NON-NLS-1$
        Label label = new Label(localParent, SWT.NONE);
        label.setText("Classifier"); //$NON-NLS-1$
        label.setLayoutData(GridDataFactory.fillDefaults().create());
        Combo classifier = new Combo(localParent, SWT.NONE);
        for (ITmfEventAspect<?> aspect : fRef.getEventAspects()) {
            String name = aspect.getName();
            classifier.add(name);
            if (name.equals(fSegmentSpecifier.getClassifierType())) {
                classifier.select(classifier.getItemCount() - 1);
            }
        }
        classifier.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = classifier.getSelectionIndex();
                if (index >= 0) {
                    setCategory(classifier.getItems()[index]);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
        classifier.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        return localParent;
    }

    private void createEntry(Composite parent, String labelText, Supplier<String> resolver, Consumer<String> setter) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(labelText);
        label.setLayoutData(GridDataFactory.fillDefaults().create());
        Text text = new Text(parent, SWT.NONE);
        text.setText(resolver.get());
        text.setLayoutData(GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 20).grab(true, false).create());
        text.addModifyListener(listener -> {
            setter.accept(text.getText());
            for (boolean validentry : fValid) {
                if (!validentry) {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                    return;
                }
            }
            getButton(IDialogConstants.OK_ID).setEnabled(true);
        });
    }

    @Override
    protected void okPressed() {
        fUpdated = fSegmentSpecifier.setIfNotNull(fLabel, fInRegex, fOutRegex, fContextInRegex, fContextOutRegex, fCategory);
        super.okPressed();
    }

    /**
     * Has the value changed
     *
     * @return true if it changed
     */
    public boolean isUpdated() {
        return fUpdated;
    }
}
