/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.filters.ui.lspFilterTextbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.incubator.internal.filters.core.client.wrapper.LanguageFilterClientWrapper;
import org.eclipse.tracecompass.incubator.internal.filters.core.shared.LspObserver;

/**
 * Widget to wrap a FilterText widget with additional logic of a filter lsp
 * client
 *
 * @author Jeremy Dube
 */
public class LspFilterTextbox implements LspObserver {

    private final String fFilterBoxUri;
    private @Nullable LanguageFilterClientWrapper fLspClient;
    private List<FilterValidityListener> fListeners = new ArrayList<>();
    private final Color fDefaultFilterTextColor;
    private final Color fDefaultFilterBackgroundColor;
    private final StyledText fFilterStyledText;
    private final TextViewer fTextViewer;
    private final CLabel fSearchButton;
    private final CLabel fCancelButton;
    private final RecentlyUsedFilters fRecentlyUsedFilters;
    private String fLastTextUpdate;
    private final DefaultToolTip fToolTip;

    private Boolean fIsValidString = false;
    private List<ColorInformation> fColors = new ArrayList<>();
    private List<Diagnostic> fDiagnostics = new ArrayList<>();

    /**
     * Constructor
     *
     * @param parent
     *            the parent view
     * @param filterBoxUri
     *            the uri of the lsp client
     */
    public LspFilterTextbox(Composite parent, String filterBoxUri) {
        fFilterBoxUri = filterBoxUri;
        final Composite baseComposite = new Composite(parent, SWT.BORDER);
        baseComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        final GridLayout baseCompositeGridLayout = new GridLayout(3, false);
        baseCompositeGridLayout.marginHeight = 0;
        baseCompositeGridLayout.marginWidth = 0;
        baseComposite.setLayout(baseCompositeGridLayout);

        // Search icon
        fSearchButton = new CLabel(baseComposite, SWT.CENTER);
        fSearchButton.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).create());
        fSearchButton.setText("search"); //$NON-NLS-1$ // Will be changed for an image

        // Text box
        fTextViewer = new TextViewer(baseComposite, SWT.SINGLE | SWT.BORDER);
        fFilterStyledText = fTextViewer.getTextWidget();
        fFilterStyledText.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

        // Cancel icon
        fCancelButton = new CLabel(baseComposite, SWT.CENTER);
        fCancelButton.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).create());
        fCancelButton.setText("clear"); //$NON-NLS-1$ // Will be changed for an image

        setIconsListener();
        setKeyListener();
        fDefaultFilterBackgroundColor = fFilterStyledText.getBackground();
        fLspClient = new LanguageFilterClientWrapper(this, fFilterBoxUri);
        fDefaultFilterTextColor = fFilterStyledText.getForeground();
        fRecentlyUsedFilters = new RecentlyUsedFilters(5, "GlobalFilterViewer"); //$NON-NLS-1$
        // TODO: To combine with the completion items once available
        // List<String> filterStrings = fRecentlyUsedFilters.getRecently();

        fToolTip = new DefaultToolTip(fFilterStyledText);
        styleToolTip();
    }

    /**
     * Method to set the focus on the FilterText
     *
     * @return true if the FilterText gets the focus
     */
    public boolean setFocus() {
        return fFilterStyledText.setFocus();
    }

    /**
     * Method to return the text in the Filter Box
     *
     * @return the text
     */
    public String getText() {
        return fFilterStyledText.getText();
    }

    /**
     * Method to set the text in the Filter Box
     *
     * @param text
     *            the value to set to
     */
    public void setText(String text) {
        fFilterStyledText.setText(text);
    }

    /**
     * Method to add an external listener when the string is valid
     *
     * @param validListener
     *            the listener to add
     */
    public void addValidListener(FilterValidityListener validListener) {
        fListeners.add(validListener);
    }

    /**
     * Method to add some custom style to the toolTip
     */
    private void styleToolTip() {
        fToolTip.setBackgroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        fToolTip.setForegroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        fToolTip.setShift(new Point(0, 10));
        fToolTip.deactivate();
    }

    /**
     * Method to notify listeners of valid string
     */
    private void notifyValid() {
        fRecentlyUsedFilters.addFilter(fFilterStyledText.getText());
        if (fIsValidString) {
            for (FilterValidityListener validListener : fListeners) {
                validListener.validFilter();
            }
        }
    }

    /**
     * Method to notify listeners of invalid string
     */
    private void notifyInvalid() {
        for (FilterValidityListener validListener : fListeners) {
            validListener.invalidFilter();
        }
    }

    /**
     * Method to add a key Listener to the FilterText Widget
     */
    private void setKeyListener() {
        fFilterStyledText.addKeyListener(new KeyListener() {

            @Override
            public void keyReleased(@Nullable KeyEvent e) {
                if (e == null) {
                    return;
                }

                // Get the filterbox texte
                String text = Objects.requireNonNull(fFilterStyledText.getText());

                // If text has not change since last update from autocompletion,
                // do nothing
                // Also if the last key-up is not backspace and delete,
                if (e.character != SWT.BS && e.character != SWT.DEL && !text.equals(fLastTextUpdate)) {
                    // it means a new character has been inserted, so try to
                    // autocomplete it
                    Integer cursorPosition = fFilterStyledText.getCaretOffset();
                    String newText = FilterBoxLocalTextCompletion.autocomplete(text, cursorPosition);
                    // If the autocompletion changed the string
                    if (!newText.equals(text)) {
                        // Update the filtertextbox
                        fFilterStyledText.setText(newText);
                        fFilterStyledText.setCaretOffset(cursorPosition);
                        text = newText;
                    }
                }
                fLastTextUpdate = text;
                notifyLspClient(text);
            }

            @Override
            public void keyPressed(@Nullable KeyEvent e) {
                if (e == null) {
                    return;
                }
                if (e.character == SWT.CR) {
                    notifyValid();
                }
            }
        });
    }

    /**
     * Method to add a mouse Listener to the icons
     */
    private void setIconsListener() {
        fSearchButton.addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent e) {
                notifyValid();
            }

            @Override
            public void mouseDown(MouseEvent e) {
                // Nothing to do here
            }

            @Override
            public void mouseDoubleClick(MouseEvent e) {
                // Nothing to do here
            }
        });
        fCancelButton.addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent e) {
                fFilterStyledText.setText(StringUtils.EMPTY);
                resetView();
            }

            @Override
            public void mouseDown(MouseEvent e) {
                // Nothing to do here
            }

            @Override
            public void mouseDoubleClick(MouseEvent e) {
                // Nothing to do here
            }
        });
    }

    /**
     * Method called by the lsp client to notify the view of errors
     */
    @Override
    public void diagnostic(String uri, List<Diagnostic> diagnostics) {
        if (fFilterBoxUri.equals(uri)) {
            Display.getDefault().syncExec(new Runnable() {
                @Override()
                public void run() {
                    fDiagnostics = diagnostics;
                    if (fDiagnostics.size() > 0) {
                        updateView();
                        fIsValidString = false;
                        notifyInvalid();
                        addErrorPopup();
                    } else {
                        fToolTip.deactivate();
                        fIsValidString = true;
                    }
                }
            });
        }
    }

    /**
     * Method called by the lsp client to notify the view of completion items
     */
    @Override
    public void completion(String uri, final Either<List<CompletionItem>, CompletionList> completion) {
        if (fFilterBoxUri.equals(uri)) {
            Display.getDefault().syncExec(new Runnable() {
                @Override()
                public void run() {
                    // TODO: Needs to be implemented and linked with the dropdown
                    /*List<CompletionItem> completions = completion.getLeft();
                    for (int i = 0; i < completions.size(); i++) {
                        TextEdit textEdit = completions.get(i).getTextEdit();
                        String suggestion = textEdit.getNewText();
                        // this need to be link with the dropdown
                        System.out.println("Please do something with this variable " + suggestion);
                    }*/
                }
            });
        }
    }

    /**
     * Method called by the lsp client to notify the view of colors' definition
     */
    @Override
    public void syntaxHighlighting(String uri, List<ColorInformation> colors) {
        if (fFilterBoxUri.equals(uri)) {
            Display.getDefault().syncExec(new Runnable() {
                @Override()
                public void run() {
                    fColors = colors;
                    updateView();
                }
            });
        }
    }

    /**
     * Method to notify the LSP Client of a change
     *
     * @param message
     *            string entered in the filter box
     */
    private void notifyLspClient(String message) {
        if (message.isEmpty()) {
            resetView();
        } else {
            if (fLspClient != null) {
                fLspClient.notify(fFilterBoxUri, message, fFilterStyledText.getCaretOffset());
            }
        }
    }

    /**
     * Method to reset the filter box view (i.e. put back initial color, remove
     * error message, remove suggestions)
     */
    private void resetView() {
        fFilterStyledText.setBackground(fDefaultFilterBackgroundColor);
    }

    /**
     * Method to add a popup on hover of the range to show the error message
     *
     * @param diagnostic
     */
    private void addErrorPopup() {
        fToolTip.activate();
        String toolTipText = ""; //$NON-NLS-1$
        int index = 0;
        for (Diagnostic diagnostic : fDiagnostics) {
            int start = diagnostic.getRange().getStart().getCharacter() + 1;
            toolTipText += "Error at character " + start + " : " + diagnostic.getMessage();  //$NON-NLS-1$//$NON-NLS-2$
            index++;
            if (index < fDiagnostics.size()) {
                toolTipText += "\n"; //$NON-NLS-1$
            }
        }
        fToolTip.setText(toolTipText);
    }

    /**
     * Method to call when an update to the text is needed
     */
    private void updateView() {
        for (int index = 0; index < fFilterStyledText.getText().length(); index++) {
            Color foregroundColor = getColor(index);
            Boolean hasError = indexIsError(index);
            updateViewBetween(index, index + 1, foregroundColor, hasError);
        }
    }

    /**
     * Method to update the view between the specified indexes
     *
     * @param start
     *            the start index
     * @param end
     *            the end index
     * @param foregroundColor
     *            the color of the text
     * @param hasError
     *            if the range has an error, the underlining will be added
     */
    private void updateViewBetween(int start, int end, Color foregroundColor, Boolean hasError) {
        StyleRange styleRange = new StyleRange();
        styleRange.start = start;
        styleRange.length = end - start;
        styleRange.foreground = foregroundColor;

        if (hasError) {
            styleRange.underline = true;
            styleRange.underlineColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
        }
        fFilterStyledText.setStyleRange(styleRange);
    }

    /**
     * Method to check if the index passed is inside an error range
     *
     * @param index
     *            the index to check
     * @return true if index is inside error range, false otherwise
     */
    private boolean indexIsError(int index) {
        for (Diagnostic diagnostic : fDiagnostics) {
            int start = diagnostic.getRange().getStart().getCharacter();
            int end = diagnostic.getRange().getEnd().getCharacter() - 1;

            if (index >= start && index <= end) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the color at the specified index. Looks into all the received color
     * information items
     *
     * @param index
     *            the index to check
     * @return the color, either gotten inside a color information item or the
     *         default text color
     */
    private Color getColor(int index) {
        for (ColorInformation colorInformation : fColors) {
            int start = colorInformation.getRange().getStart().getCharacter();
            int end = colorInformation.getRange().getEnd().getCharacter();

            if (index >= start && index <= end) {
                Device device = Display.getCurrent();
                Color color = new Color(device, (int) (colorInformation.getColor().getRed() * 255),
                        (int) (colorInformation.getColor().getGreen() * 255),
                        (int) (colorInformation.getColor().getBlue() * 255));
                return color;
            }
        }
        return fDefaultFilterTextColor;
    }
}
