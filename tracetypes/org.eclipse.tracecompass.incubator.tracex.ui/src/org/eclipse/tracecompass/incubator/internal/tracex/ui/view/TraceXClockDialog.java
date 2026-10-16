/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.ui.view;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTimeNormalizer;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTimerDirection;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;

/** Edits the complete clock pair without changing the trace before confirmation. */
@NonNullByDefault({})
@SuppressWarnings("restriction")
public final class TraceXClockDialog extends TitleAreaDialog {

    private final TraceXTrace fTrace;
    private Combo fDirection;
    private Button fKnownFrequency;
    private Text fFrequency;
    private TraceXTimerDirection fSelectedDirection = TraceXTimerDirection.INCREASING;
    private @Nullable String fSelectedFrequency;

    /**
     * Create a clock editor initialized from the selected trace.
     *
     * @param shell parent shell
     * @param trace selected TraceX trace
     */
    public TraceXClockDialog(Shell shell, TraceXTrace trace) {
        super(shell);
        fTrace = trace;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("TraceX clock"); //$NON-NLS-1$
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        setTitle("TraceX clock"); //$NON-NLS-1$
        setMessage("Use the target timer configuration. Direction is not encoded in the trace."); //$NON-NLS-1$
        Composite controls = new Composite(area, SWT.NONE);
        controls.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        controls.setLayout(new GridLayout(2, false));
        new Label(controls, SWT.NONE).setText("Timer direction"); //$NON-NLS-1$
        fDirection = new Combo(controls, SWT.DROP_DOWN | SWT.READ_ONLY);
        fDirection.setItems("Increasing", "Decreasing"); //$NON-NLS-1$ //$NON-NLS-2$
        fDirection.select(fTrace.getTimerDirection() == TraceXTimerDirection.INCREASING ? 0 : 1);
        fDirection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        fKnownFrequency = new Button(controls, SWT.CHECK);
        fKnownFrequency.setText("Clock frequency is known"); //$NON-NLS-1$
        fKnownFrequency.setSelection(fTrace.getTicksPerSecond().isPresent());
        fKnownFrequency.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        new Label(controls, SWT.NONE).setText("Ticks per second"); //$NON-NLS-1$
        fFrequency = new Text(controls, SWT.BORDER);
        fFrequency.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (fTrace.getTicksPerSecond().isPresent()) {
            fFrequency.setText(Long.toString(fTrace.getTicksPerSecond().getAsLong()));
        }
        Label explanation = new Label(controls, SWT.WRAP);
        explanation.setText("Without a known frequency, timestamps are ordering ticks and physical time is unknown."); //$NON-NLS-1$
        explanation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        fKnownFrequency.addListener(SWT.Selection, event -> validate());
        fFrequency.addModifyListener(event -> validate());
        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        validate();
    }

    /** Show invalid input inline and keep it from being applied. */
    private void validate() {
        boolean known = fKnownFrequency.getSelection();
        fFrequency.setEnabled(known);
        String error = null;
        if (known) {
            try {
                TraceXTimeNormalizer.parseTicksPerSecond(fFrequency.getText().trim());
            } catch (IllegalArgumentException e) {
                error = "Enter a whole number from 1 to 1000000000 ticks per second."; //$NON-NLS-1$
            }
        }
        setErrorMessage(error);
        Button ok = getButton(IDialogConstants.OK_ID);
        if (ok != null) {
            ok.setEnabled(error == null);
        }
    }

    @Override
    protected void okPressed() {
        fSelectedDirection = fDirection.getSelectionIndex() == 0
                ? TraceXTimerDirection.INCREASING : TraceXTimerDirection.DECREASING;
        fSelectedFrequency = fKnownFrequency.getSelection() ? fFrequency.getText().trim() : null;
        super.okPressed();
    }

    /** @return selected trace-wide timer direction */
    public TraceXTimerDirection getDirection() {
        return fSelectedDirection;
    }

    /** @return the validated frequency, or null when physical time is unknown */
    public @Nullable String getFrequency() {
        return fSelectedFrequency;
    }
}
