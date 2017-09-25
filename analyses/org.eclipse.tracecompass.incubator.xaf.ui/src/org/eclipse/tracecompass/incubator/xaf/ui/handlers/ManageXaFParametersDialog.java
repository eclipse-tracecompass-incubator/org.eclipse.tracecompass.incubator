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
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariable;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.google.common.base.Joiner;

/**
 * Dialog for extended analysis feature parameters
 *
 * @author Raphaël Beamonte
 */
public class ManageXaFParametersDialog extends Dialog {

    private static final String DEFAULT_MODEL_FILENAME = "model.sc.xml"; //$NON-NLS-1$

    private Composite container;
    private Button buttonProvideModel, buttonAllInstancesValid, buttonCheckModel;
    private FileChooser fileChooserModelLocation;
    private TimestampText textTimeRangesFrom, textTimeRangesTo;
    private Table tableTimeRanges, tableVariables;
    private Properties properties;

    @Override
    protected int getShellStyle() {
        return super.getShellStyle() & (~SWT.RESIZE);
    }

    /**
     * Constructor
     *
     * @param parent
     *            Parent shell of this dialog
     * @param properties
     *            The initial properties to be loaded into the dialog when
     *            opened
     */
    public ManageXaFParametersDialog(Shell parent, Properties properties) {
        super(parent);
        setShellStyle(SWT.RESIZE | SWT.MAX | getShellStyle() & ~SWT.APPLICATION_MODAL);
        this.properties = (properties == null) ? new Properties() : properties;
    }

    private static class TimestampText implements FocusListener, KeyListener {

        private long fValue;
        private Text fText;
        private boolean edited = false;

        public TimestampText(Composite parent, int style) {
            fText = new Text(parent, style);

            fText.addKeyListener(this);
            fText.addFocusListener(this);
        }

        /**
         * The time value in to set in the text field.
         *
         * @param time
         *            the time to set
         * @param displayTime
         *            the display value
         */
        private void setTextValue(final long time, final String displayTime) {
            // If this is the UI thread, process now
            Display display = Display.getCurrent();
            if (display != null) {
                if (!fText.isDisposed()) {
                    fValue = time;
                    fText.setText(displayTime);
                    // fComposite.layout();
                    fText.getParent().layout();
                }
                return;
            }

            // Call self from the UI thread
            if (!fText.isDisposed()) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (!fText.isDisposed()) {
                            setTextValue(time, displayTime);
                        }
                    }
                });
            }
        }

        public void setValue(long time) {
            if (time != Long.MIN_VALUE) {
                setTextValue(time, TmfTimestamp.fromNanos(time).toString());
            } else {
                setTextValue(Long.MIN_VALUE, StringUtils.EMPTY);
            }
        }

        /**
         * Returns the time value.
         *
         * @return time value.
         */
        public long getValue() {
            return fValue;
        }

        protected void updateValue() {
            if (fText.getText().isEmpty()) {
                setValue(getValue());
                return;
            }
            String string = fText.getText();
            long value = getValue();
            try {
                value = TmfTimestampFormat.getDefaulTimeFormat().parseValue(string, getValue());
            } catch (ParseException e) {
                setValue(getValue());
            }
            if (getValue() != value) {
                // Make sure that the new time is within range
                ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
                if (trace != null) {
                    TmfTimeRange range = trace.getTimeRange();
                    long startTime = range.getStartTime().toNanos();
                    long endTime = range.getEndTime().toNanos();
                    if (value < startTime) {
                        value = startTime;
                    } else if (value > endTime) {
                        value = endTime;
                    }
                }

                // Set and propagate
                setValue(value);
            } else {
                setValue(value);
            }
        }

        public boolean isEdited() {
            return edited;
        }

        public void setFont(Font font) {
            fText.setFont(font);
        }

        public void setLayoutData(GridData gridData) {
            fText.setLayoutData(gridData);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            edited = true;
            switch (e.character) {
            case SWT.CR:
                updateValue();
                break;
            default:
                break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void focusGained(FocusEvent e) {
        }

        @Override
        public void focusLost(FocusEvent e) {
            updateValue();
        }
    }

    private static class FileChooser extends Composite {
        Combo combo;
        Button button;

        public FileChooser(Composite parent) {
            super(parent, SWT.NULL);
            createContent();
        }

        public void createContent() {
            GridLayout layout = new GridLayout(2, false);
            setLayout(layout);

            combo = new Combo(this, SWT.SINGLE | SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalAlignment = GridData.FILL;
            combo.setLayoutData(gd);

            button = new Button(this, SWT.NONE);
            button.setText("..."); //$NON-NLS-1$
            button.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                }

                @Override
                public void widgetSelected(SelectionEvent e) {
                    FileDialog dialog = new FileDialog(button.getShell(), SWT.OPEN);
                    dialog.setText(Messages.ManageXaFParametersDialog_FileChooserDialogTitle);
                    String path = dialog.open();
                    if (path == null) {
                        return;
                    }
                    setText(path);
                }
            });
        }

        public String getText() {
            return combo.getText();
        }

        public void setText(String text) {
            combo.setText(text);
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (combo.getItem(i).equals(text)) {
                    combo.remove(i);
                    break;
                }
            }
            while (combo.getItemCount() > 29) {
                combo.remove(29);
            }
            combo.add(text, 0);
        }

        public void loadHistory(List<String> history) {
            combo.removeAll();
            for (String text : history) {
                combo.add(text, 0);
            }
        }

        public Combo getTextControl() {
            return combo;
        }

        public File getFile() {
            String text = combo.getText();
            if (text.length() == 0) {
                return null;
            }
            return new File(text);
        }
    }

    private static @Nullable File getDefaultModelFile() {
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace == null) {
            return null;
        }
        String directory = TmfTraceManager.getSupplementaryFileDir(trace);
        File configFile = new File(directory + DEFAULT_MODEL_FILENAME);
        return configFile;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        GridLayout gridLayout;

        getShell().setText(Messages.ManageXaFParametersDialog_XaFParameters);

        // Load parameters
        boolean modelProvided = Boolean.parseBoolean(properties.getProperty(XaFParameterProvider.PROPERTY_MODEL_PROVIDED, Boolean.TRUE.toString()));
        String defaultModelLocation = (getDefaultModelFile() == null) ? StringUtils.EMPTY : NonNullUtils.checkNotNull(getDefaultModelFile()).getPath();
        String modelLocation = properties.getProperty(XaFParameterProvider.PROPERTY_MODEL_LOCATION, defaultModelLocation);

        Set<String> selectedVariables;
        String propertySelectedVariables = properties.getProperty(XaFParameterProvider.PROPERTY_SELECTED_VARIABLES);
        if (propertySelectedVariables == null) {
            selectedVariables = StateMachineVariable.VARIABLE_TYPES.keySet();
        } else {
            selectedVariables = new HashSet<>(Arrays.asList(propertySelectedVariables.split(XaFParameterProvider.PROPERTY_SEPARATOR)));
        }

        Set<TimestampInterval> timeRanges = new TreeSet<>();
        String propertySelectedTimeranges = properties.getProperty(XaFParameterProvider.PROPERTY_SELECTED_TIMERANGES);
        if (propertySelectedTimeranges != null && !propertySelectedTimeranges.isEmpty()) {
            for (String timeRangeStr : propertySelectedTimeranges.split(XaFParameterProvider.PROPERTY_SEPARATOR)) {
                String[] timeElementsStr = timeRangeStr.split(XaFParameterProvider.PROPERTY_SELECTED_TIMERANGES_SEPARATOR);
                long startTime = Long.parseLong(timeElementsStr[0]);
                long endTime = Long.parseLong(timeElementsStr[1]);
                timeRanges.add(new TimestampInterval(startTime, endTime));
            }
        }
        boolean allInstancesValid = Boolean.parseBoolean(properties.getProperty(XaFParameterProvider.PROPERTY_ALL_INSTANCES_VALID, Boolean.FALSE.toString()));
        boolean checkModel = Boolean.parseBoolean(properties.getProperty(XaFParameterProvider.PROPERTY_CHECK_MODEL, Boolean.TRUE.toString()));

        // Main element
        container = (Composite) super.createDialogArea(parent);

        TmfSignalManager.register(this);
        container.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                TmfSignalManager.deregister(this);
            }
        });

        gridLayout = new GridLayout(1, false);
        gridLayout.marginHeight = 5;
        gridLayout.marginWidth = 5;
        container.setLayout(gridLayout);

        // To select the mode: provide a model or automate the model generation
        buttonProvideModel = new Button(container, SWT.RADIO);
        buttonProvideModel.setText(Messages.ManageXaFParametersDialog_ButtonProvideModel);
        Button buttonGenerateModel = new Button(container, SWT.RADIO);
        buttonGenerateModel.setText(Messages.ManageXaFParametersDialog_ButtonGenerateModel);

        // For the file location
        gridLayout = new GridLayout(2, false);
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        Composite compositeModelFile = new Composite(container, SWT.NONE);
        compositeModelFile.setLayout(gridLayout);

        Label l = new Label(compositeModelFile, SWT.WRAP);
        l.setText(Messages.ManageXaFParametersDialog_FileChooserLabel);

        fileChooserModelLocation = new FileChooser(compositeModelFile);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.minimumWidth = 365;
        gridData.minimumHeight = 50;
        gridData.grabExcessHorizontalSpace = true;
        fileChooserModelLocation.setLayoutData(gridData);
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                String historyStr = properties.getProperty(XaFParameterProvider.PROPERTY_MODEL_LOCATION_HISTORY);
                if (historyStr != null) {
                    fileChooserModelLocation.loadHistory(Arrays.asList(historyStr.split(XaFParameterProvider.PROPERTY_SEPARATOR)));
                }
                fileChooserModelLocation.setText(modelLocation);
            }
        });

        // For the variables
        Group groupVariables = new Group(container, SWT.FILL);
        groupVariables.setText(Messages.ManageXaFParametersDialog_VariablesGroup);

        RowLayout groupVariablesLayout = new RowLayout(SWT.HORIZONTAL);
        groupVariablesLayout.marginHeight = 5;
        groupVariablesLayout.marginWidth = 5;
        groupVariablesLayout.spacing = 15;
        groupVariables.setLayout(groupVariablesLayout);

        tableVariables = new Table(groupVariables, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        tableVariables.setLayoutData(new RowData(200, 200));
        for (Entry<String, Class<?>> entry : StateMachineVariable.VARIABLE_TYPES.entrySet()) {
            TableItem item = new TableItem(tableVariables, SWT.NONE);
            item.setText(entry.getKey());
            item.setData(entry.getValue());
            item.setChecked(selectedVariables.contains(entry.getKey()));
        }

        Label labelVariables = new Label(groupVariables, SWT.WRAP);
        labelVariables.setText(Messages.ManageXaFParametersDialog_VariablesLabelDefault);
        labelVariables.setLayoutData(new RowData(214, 200));

        HashMap<String, String> descriptionVariables = new HashMap<>();
        descriptionVariables.put("deadline", Messages.ManageXaFParametersDialog_VariablesLabelDeadline); //$NON-NLS-1$
        descriptionVariables.put("preempt", Messages.ManageXaFParametersDialog_VariablesLabelPreempt); //$NON-NLS-1$
        descriptionVariables.put("syscalls", Messages.ManageXaFParametersDialog_VariablesLabelSyscalls); //$NON-NLS-1$
        descriptionVariables.put("cputime", Messages.ManageXaFParametersDialog_VariablesLabelCputime); //$NON-NLS-1$

        tableVariables.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem item = (TableItem) e.item;
                String text = descriptionVariables.get(item.getText());
                if (text == null) {
                    text = item.getText();
                }
                labelVariables.setText(text);
            }
        });

        // --------------------------------------------------------------------
        // Reduce font size for a more pleasing rendering
        // --------------------------------------------------------------------

        final int fontSizeAdjustment = -1;
        final Font font = parent.getFont();
        final FontData fontData = font.getFontData()[0];
        Font timeRangesFont = new Font(font.getDevice(), fontData.getName(), fontData.getHeight() + fontSizeAdjustment, fontData.getStyle());

        // For the time ranges
        Group groupTimeRanges = new Group(container, SWT.NONE);
        groupTimeRanges.setText(Messages.ManageXaFParametersDialog_TimeRangesGroup);

        RowLayout groupTimeRangesLayout = new RowLayout(SWT.HORIZONTAL);
        groupTimeRangesLayout.marginHeight = 5;
        groupTimeRangesLayout.marginWidth = 5;
        groupTimeRanges.setLayout(groupTimeRangesLayout);

        tableTimeRanges = new Table(groupTimeRanges, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
        tableTimeRanges.setLayoutData(new RowData(200, 200));
        for (TimestampInterval timestampInterval : timeRanges) {
            TableItem item = new TableItem(tableTimeRanges, SWT.NONE);
            item.setData("from", timestampInterval.getStartTime().toNanos()); //$NON-NLS-1$
            item.setData("to", timestampInterval.getEndTime().toNanos()); //$NON-NLS-1$
            item.setText(String.format("[%s, %s]", //$NON-NLS-1$
                    timestampInterval.getStartTime().toString(),
                    timestampInterval.getEndTime().toString()));
        }

        Composite compositeTimeRangesRightPanel = new Composite(groupTimeRanges, SWT.NONE);
        compositeTimeRangesRightPanel.setLayout(new FillLayout(SWT.VERTICAL));

        gridLayout = new GridLayout(3, false);
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        Composite compositeTimeRanges = new Composite(compositeTimeRangesRightPanel, SWT.NONE);
        compositeTimeRanges.setLayout(gridLayout);

        Label fillerTimeRanges = new Label(compositeTimeRanges, SWT.NONE);
        fillerTimeRanges.setFont(timeRangesFont);

        Label labelTimeRangesFrom = new Label(compositeTimeRanges, SWT.NONE);
        labelTimeRangesFrom.setFont(timeRangesFont);
        labelTimeRangesFrom.setText(Messages.ManageXaFParametersDialog_TimeRangesLabelFrom);

        textTimeRangesFrom = new TimestampText(compositeTimeRanges, SWT.BORDER);
        textTimeRangesFrom.setFont(timeRangesFont);
        textTimeRangesFrom.setLayoutData(new GridData(170, -1));

        fillerTimeRanges = new Label(compositeTimeRanges, SWT.NONE);
        fillerTimeRanges.setFont(timeRangesFont);

        Label labelTimeRangesTo = new Label(compositeTimeRanges, SWT.NONE);
        labelTimeRangesTo.setFont(timeRangesFont);
        labelTimeRangesTo.setText(Messages.ManageXaFParametersDialog_TimeRangesLabelTo);

        textTimeRangesTo = new TimestampText(compositeTimeRanges, SWT.BORDER);
        textTimeRangesTo.setFont(timeRangesFont);
        textTimeRangesTo.setLayoutData(new GridData(170, -1));

        resetTimeRangeText();

        GridLayout rowLayout = new GridLayout(3, false);
        rowLayout.marginHeight = 5;
        rowLayout.marginWidth = 0;
        rowLayout.horizontalSpacing = 0;
        Composite compositeTimeRangesButtons = new Composite(compositeTimeRangesRightPanel, SWT.NONE);
        compositeTimeRangesButtons.setLayout(rowLayout);
        Button addTimeRange = new Button(compositeTimeRangesButtons, SWT.PUSH | SWT.FILL);
        addTimeRange.setText(Messages.ManageXaFParametersDialog_TimeRangesButtonAdd);
        addTimeRange.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        addTimeRange.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (textTimeRangesFrom.getValue() == Long.MIN_VALUE || textTimeRangesTo.getValue() == Long.MIN_VALUE) {
                    MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
                    messageBox.setMessage(Messages.ManageXaFParametersDialog_TimeRangesErrorInvalidInterval);
                    messageBox.open();
                    return;
                }

                // Do not add if the time range exists
                for (int i = 0; i < tableTimeRanges.getItemCount(); i++) {
                    TableItem item = tableTimeRanges.getItem(i);
                    if (item.getData("from").equals(textTimeRangesFrom.getValue()) //$NON-NLS-1$
                            && item.getData("to").equals(textTimeRangesTo.getValue())) { //$NON-NLS-1$
                        MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
                        messageBox.setMessage(Messages.ManageXaFParametersDialog_TimeRangesErrorIntervalAlreadyAdded);
                        messageBox.open();
                        return;
                    }
                }

                long newStartTime = textTimeRangesFrom.getValue();
                long newEndTime = textTimeRangesTo.getValue();

                // Check if this time range merges into another already in the
                // list
                boolean changedStartEndTime = true;
                while (changedStartEndTime) {
                    changedStartEndTime = false;
                    for (int i = 0; i < tableTimeRanges.getItemCount(); i++) {
                        TableItem item = tableTimeRanges.getItem(i);
                        long startTime = (long) item.getData("from"); //$NON-NLS-1$
                        long endTime = (long) item.getData("to"); //$NON-NLS-1$

                        if ((startTime <= newStartTime && endTime >= newStartTime)
                                || (startTime <= newEndTime && endTime >= newEndTime)
                                || (startTime >= newStartTime && endTime <= newEndTime)) {
                            newStartTime = Math.min(startTime, newStartTime);
                            newEndTime = Math.max(endTime, newEndTime);
                            tableTimeRanges.remove(i);
                            changedStartEndTime = true;
                            break;
                        }
                    }
                }

                // Finally, if nothing prevented it before, add the new time
                // range
                TableItem newItem = new TableItem(tableTimeRanges, SWT.NONE);
                newItem.setData("from", newStartTime); //$NON-NLS-1$
                newItem.setData("to", newEndTime); //$NON-NLS-1$
                newItem.setText(String.format("[%s, %s]", //$NON-NLS-1$
                        TmfTimestamp.fromNanos(newStartTime).toString(),
                        TmfTimestamp.fromNanos(newEndTime).toString()));
                resetTimeRangeText();
            }
        });
        Button removeTimeRange = new Button(compositeTimeRangesButtons, SWT.PUSH);
        removeTimeRange.setText(Messages.ManageXaFParametersDialog_TimeRangesButtonRemove);
        removeTimeRange.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        removeTimeRange.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                tableTimeRanges.remove(tableTimeRanges.getSelectionIndices());
                tableTimeRanges.deselectAll();
            }
        });
        Button clearTimeRange = new Button(compositeTimeRangesButtons, SWT.PUSH);
        clearTimeRange.setText(Messages.ManageXaFParametersDialog_TimeRangesButtonClear);
        clearTimeRange.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        clearTimeRange.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                tableTimeRanges.removeAll();
            }
        });

        buttonAllInstancesValid = new Button(container, SWT.CHECK);
        buttonAllInstancesValid.setText(Messages.ManageXaFParametersDialog_ButtonAllInstancesValid);
        buttonAllInstancesValid.setSelection(allInstancesValid);

        buttonCheckModel = new Button(container, SWT.CHECK);
        buttonCheckModel.setText(Messages.ManageXaFParametersDialog_ButtonCheckModel);
        IWorkbenchWindow window = null;
        try {
            window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        } catch (IllegalStateException e) {
        }
        final boolean foundWorkbenchWindow = (window != null);
        if (foundWorkbenchWindow) {
            buttonCheckModel.setSelection(checkModel);
        } else {
            buttonCheckModel.setSelection(false);
            buttonCheckModel.setEnabled(false);
        }

        SelectionAdapter radioButtonSelectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Button button = ((Button) e.widget);
                if (button.equals(buttonGenerateModel)) {
                    LinkedList<Control> toDisable = new LinkedList<>();
                    toDisable.add(groupVariables);
                    toDisable.add(groupTimeRanges);
                    toDisable.add(buttonAllInstancesValid);

                    while (!toDisable.isEmpty()) {
                        Control c = toDisable.pop();
                        c.setEnabled(button.getSelection());
                        if (c instanceof Composite) {
                            toDisable.addAll(Arrays.asList(((Composite) c).getChildren()));
                        }
                    }

                    if (foundWorkbenchWindow) {
                        buttonCheckModel.setEnabled(button.getSelection());
                    }
                }
            }
        };
        buttonGenerateModel.addSelectionListener(radioButtonSelectionListener);
        buttonProvideModel.setSelection(modelProvided);
        buttonGenerateModel.setSelection(!modelProvided);
        Event e = new Event();
        e.widget = buttonGenerateModel;
        radioButtonSelectionListener.widgetSelected(new SelectionEvent(e));

        getShell().setMinimumSize(470, 600);
        return container;
    }

    private void resetTimeRangeText() {
        ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activeTrace != null) {
            textTimeRangesFrom.setValue(activeTrace.getStartTime().toNanos());
            textTimeRangesTo.setValue(activeTrace.getEndTime().toNanos());
        } else {
            textTimeRangesFrom.setValue(Long.MIN_VALUE);
            textTimeRangesTo.setValue(Long.MIN_VALUE);
        }
    }

    private static boolean canWrite(File file) {
        if (file.exists()) {
            return file.canWrite();
        }

        try {
            file.createNewFile();
            file.delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.PROCEED_ID) {
            properties = null;
            File modelFile = fileChooserModelLocation.getFile();

            if (buttonProvideModel.getSelection()) {
                // Case when the model is provided
                if (modelFile == null) {
                    MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
                    messageBox.setMessage(Messages.ManageXaFParametersDialog_ProvideModelErrorNoModelLocation);
                    messageBox.open();
                    return;
                } else if (!modelFile.exists() || !modelFile.isFile() || !modelFile.canRead()) {
                    MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
                    messageBox.setMessage(String.format(
                            Messages.ManageXaFParametersDialog_ProvideModelErrorModelLocationDoesNotExist,
                            modelFile.getPath()));
                    messageBox.open();
                    return;
                }
            } else {
                // Case when the model is not provided
                if (modelFile == null) {
                    MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                    messageBox.setMessage(Messages.ManageXaFParametersDialog_GenerateModelErrorNoModelLocationDialog);
                    if (messageBox.open() == SWT.NO) {
                        return;
                    }

                    try {
                        modelFile = File.createTempFile(DEFAULT_MODEL_FILENAME, null);
                    } catch (IOException e) {
                        messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
                        messageBox.setMessage(Messages.ManageXaFParametersDialog_GenerateModelErrorCannotCreateTempFile);
                        messageBox.open();
                        return;
                    }
                } else if (modelFile.isDirectory()) {
                    MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
                    messageBox.setMessage(String.format(
                            Messages.ManageXaFParametersDialog_GenerateModelErrorModelLocationIsDir,
                            modelFile.getPath()));
                    messageBox.open();
                    return;
                } else if (!canWrite(modelFile)) {
                    MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR);
                    messageBox.setMessage(String.format(
                            Messages.ManageXaFParametersDialog_GenerateModelErrorModelLocationNotWriteable,
                            modelFile.getPath()));
                    messageBox.open();
                    return;
                } else if (modelFile.isFile()) {
                    MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                    messageBox.setMessage(String.format(
                            Messages.ManageXaFParametersDialog_GenerateModelErrorOverwriteFileDialog,
                            modelFile.getPath()));
                    if (messageBox.open() == SWT.NO) {
                        return;
                    }
                }
            }

            properties = new Properties();

            properties.setProperty(XaFParameterProvider.PROPERTY_MODEL_PROVIDED,
                    new Boolean(buttonProvideModel.getSelection()).toString());

            properties.setProperty(XaFParameterProvider.PROPERTY_MODEL_LOCATION,
                    modelFile.getAbsolutePath());

            ArrayList<String> checkedVariables = new ArrayList<>();
            for (int i = 0; i < tableVariables.getItemCount(); i++) {
                if (tableVariables.getItem(i).getChecked()) {
                    checkedVariables.add(tableVariables.getItem(i).getText());
                }
            }
            properties.setProperty(XaFParameterProvider.PROPERTY_SELECTED_VARIABLES,
                    Joiner.on(XaFParameterProvider.PROPERTY_SEPARATOR).join(checkedVariables));

            ArrayList<String> timeRanges = new ArrayList<>();
            for (int i = 0; i < tableTimeRanges.getItemCount(); i++) {
                timeRanges.add(String.format("%s%s%s", //$NON-NLS-1$
                        tableTimeRanges.getItem(i).getData("from"), //$NON-NLS-1$
                        XaFParameterProvider.PROPERTY_SELECTED_TIMERANGES_SEPARATOR,
                        tableTimeRanges.getItem(i).getData("to")) //$NON-NLS-1$
                );
            }
            properties.setProperty(XaFParameterProvider.PROPERTY_SELECTED_TIMERANGES,
                    Joiner.on(XaFParameterProvider.PROPERTY_SEPARATOR).join(timeRanges));

            LinkedList<String> modelLocationHistory = new LinkedList<>();
            modelLocationHistory.add(fileChooserModelLocation.getText());
            modelLocationHistory.addAll(Arrays.asList(fileChooserModelLocation.getTextControl().getItems()));
            Set<String> orig = new HashSet<>();
            Iterator<String> it = modelLocationHistory.iterator();
            while (it.hasNext()) {
                String element = it.next();
                if (!orig.add(element)) {
                    it.remove();
                }
            }
            while (modelLocationHistory.size() > 30) {
                modelLocationHistory.removeLast();
            }
            properties.setProperty(XaFParameterProvider.PROPERTY_MODEL_LOCATION_HISTORY,
                    Joiner.on(XaFParameterProvider.PROPERTY_SEPARATOR).join(modelLocationHistory));

            properties.setProperty(XaFParameterProvider.PROPERTY_CHECK_MODEL,
                    new Boolean(buttonCheckModel.getSelection()).toString());

            properties.setProperty(XaFParameterProvider.PROPERTY_ALL_INSTANCES_VALID,
                    new Boolean(buttonAllInstancesValid.getSelection()).toString());

            okPressed();
        } else if (buttonId == IDialogConstants.CANCEL_ID) {
            properties = null;

            super.buttonPressed(buttonId);
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        createButton(parent, IDialogConstants.PROCEED_ID, IDialogConstants.PROCEED_LABEL, false);
    }

    /**
     * @return The properties defined using this dialog
     */
    public Properties getParameters() {
        return properties;
    }

    /**
     * Updates the timestamp range selector with the selected range of the trace
     *
     * @param signal
     *            The TmfSelectionRangeUpdatedSignal
     */
    @TmfSignalHandler
    public void timeSelected(final TmfSelectionRangeUpdatedSignal signal) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                textTimeRangesFrom.setValue(signal.getBeginTime().toNanos());
                textTimeRangesTo.setValue(signal.getEndTime().toNanos());
            }
        });
    }

    /**
     * Updates the timestamp range selector with the endTime of the trace
     *
     * @param signal
     *            The TmfTraceUpdatedSignal
     */
    @TmfSignalHandler
    public void traceUpdated(final TmfTraceUpdatedSignal signal) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!textTimeRangesTo.isEdited() && signal.getTrace().equals(TmfTraceManager.getInstance().getActiveTrace())) {
                    textTimeRangesTo.setValue(signal.getTrace().getEndTime().toNanos());
                }
            }
        });
    }
}
