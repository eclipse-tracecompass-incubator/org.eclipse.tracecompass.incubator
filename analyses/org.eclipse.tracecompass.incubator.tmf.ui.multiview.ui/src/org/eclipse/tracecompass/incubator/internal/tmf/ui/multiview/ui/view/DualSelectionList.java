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
import java.util.stream.IntStream;

import org.eclipse.nebula.widgets.opal.commons.SWTGraphicUtil;
import org.eclipse.nebula.widgets.opal.duallist.DLItem;
import org.eclipse.nebula.widgets.opal.duallist.SelectionChangeEvent;
import org.eclipse.nebula.widgets.opal.duallist.SelectionChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * Dual selection list that takes the idea from the Nebula's DualList. The
 * difference is that DualSelectionList does not change the list to the left by
 * default. So, the list of items to selected is always fixed.
 * <p>
 * The behavior can be changed by //TODO
 *
 * @author Ivan Grinenko
 *
 */
public class DualSelectionList extends Composite {

    private static final String MOVE_BOTTOM_IMAGE = "double_down.png"; //$NON-NLS-1$
    private static final String MOVE_TOP_IMAGE = "double_up.png"; //$NON-NLS-1$
    private static final String DESELECT_ALL_IMAGE = "double_left.png"; //$NON-NLS-1$
    private static final String SELECT_ALL_IMAGE = "double_right.png"; //$NON-NLS-1$
    private static final String MOVE_DOWN_IMAGE = "arrow_down.png"; //$NON-NLS-1$
    private static final String DESELECT_IMAGE = "arrow_left.png"; //$NON-NLS-1$
    private static final String MOVE_UP_IMAGE = "arrow_up.png"; //$NON-NLS-1$
    private static final String SELECT_IMAGE = "arrow_right.png"; //$NON-NLS-1$

    private List<DLItem> fOriginalItems = new ArrayList<>();
    private List<DLItem> fSelectedItems = new ArrayList<>();
    private Table fOriginalItemsTable;
    private Table fSelectedItemsTable;
    private List<SelectionChangeListener> fSelectionChangeListeners = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param parent
     *            a widget which will be the parent of the new instance (cannot
     *            be null)
     * @param style
     *            the style of widget to construct
     */
    public DualSelectionList(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout(4, false));

        createOriginalItemsTable();
        createSelectAllButton();
        createSelectedItemsTable();
        createMoveTopButton();
        createButtonSelect();
        createButtonMoveUp();
        createButtonDeselect();
        createButtonMoveDown();
        createButtonDeselectAll();
        createButtonMoveBottom();
    }

    @Override
    public void setBounds(final int x, final int y, final int width, final int height) {
        super.setBounds(x, y, width, height);
        final Point itemsTableDefaultSize = fOriginalItemsTable.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        final Point selectionTableDefaultSize = fSelectedItemsTable.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        int itemsTableSize = fOriginalItemsTable.getSize().x;
        if (itemsTableDefaultSize.y > fOriginalItemsTable.getSize().y) {
            itemsTableSize -= fOriginalItemsTable.getVerticalBar().getSize().x;
        }

        int selectionTableSize = fSelectedItemsTable.getSize().x;
        if (selectionTableDefaultSize.y > fSelectedItemsTable.getSize().y) {
            selectionTableSize -= fSelectedItemsTable.getVerticalBar().getSize().x;
        }

        final boolean itemsContainImage = hasImages(fOriginalItems) || hasImages(fSelectedItems);
        TableColumn originalItemsTableZeroColumn = fOriginalItemsTable.getColumn(0);
        TableColumn selectedItemsTableZeroColumn = fSelectedItemsTable.getColumn(0);
        if (itemsContainImage) {
            originalItemsTableZeroColumn.pack();
            fOriginalItemsTable.getColumn(1).setWidth(itemsTableSize - originalItemsTableZeroColumn.getWidth());

            selectedItemsTableZeroColumn.pack();
            fSelectedItemsTable.getColumn(1).setWidth(selectionTableSize - selectedItemsTableZeroColumn.getWidth());

        } else {
            originalItemsTableZeroColumn.setWidth(itemsTableSize);
            selectedItemsTableZeroColumn.setWidth(selectionTableSize);
        }

        originalItemsTableZeroColumn.pack();
        selectedItemsTableZeroColumn.pack();
    }

    /**
     * Sets items for the left list. Ignores items that are {@code null}.
     *
     * @param items
     *            new list of items. If it is {@code null} the list will be
     *            empty.
     */
    public void setItems(List<DLItem> items) {
        checkWidget();
        fOriginalItems = new ArrayList<>();
        if (items == null) {
            refreshTables();
            return;
        }
        for (DLItem item : items) {
            if (item != null) {
                fOriginalItems.add(item);
            }
        }
        refreshTables();
    }

    /**
     * Gets all the selected items that reside on the right side of the
     * component.
     *
     * @return The list of all the selected items.
     */
    public List<DLItem> getSelected() {
        return new ArrayList<>(fSelectedItems);
    }

    /**
     * Sets the list of selected items.
     *
     * @param list
     *            items to appear on the list to the right
     */
    public void setSelected(List<DLItem> list) {
        fSelectedItems.clear();
        fSelectedItems.addAll(list);
        refreshTables();
    }

    /**
     * Adds the listener to the collection of listeners who will be notified
     * when the user changes the receiver's selection, by sending it one of the
     * messages defined in the <code>SelectionChangeListener</code> interface.
     *
     * @param listener
     *            the listener which should be notified
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                </ul>
     *
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     *
     * @see SelectionChangeListener
     * @see #removeSelectionChangeListener
     * @see SelectionChangeEvent
     */
    public void addSelectionChangeListener(SelectionChangeListener listener) {
        checkWidget();
        if (listener == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        fSelectionChangeListeners.add(listener);
    }

    /**
     * Removes the listener from the collection of listeners who will be
     * notified when the user changes the receiver's selection.
     *
     * @param listener
     *            the listener which should no longer be notified
     *
     * @exception IllegalArgumentException
     *                <ul>
     *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     *                </ul>
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     * @see SelectionChangeListener
     * @see #addSelectionChangeListener
     */
    public void removeSelectionChangeListener(final SelectionChangeListener listener) {
        checkWidget();
        if (listener == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        fSelectionChangeListeners.remove(listener);
    }

    // Items manipulation section //

    private void selectItems(int[] indicesToSelect) {
        checkWidget();
        Arrays.sort(indicesToSelect);
        for (int index : indicesToSelect) {
            fSelectedItems.add(fOriginalItems.get(index));
        }
        fireSelectionChangeEvent(getSelected());
        refreshTables();
        fOriginalItemsTable.select(indicesToSelect);
        fOriginalItemsTable.forceFocus();
    }

    private void deselectItems(int[] indicesToRemove) {
        checkWidget();
        Arrays.sort(indicesToRemove);
        for (int i = fSelectedItems.size() - 1; i >= 0; --i) {
            if (arrayHasElement(indicesToRemove, i)) {
                fSelectedItems.remove(i);
            }
        }
        fireSelectionChangeEvent(getSelected());
        refreshTables();
    }

    private void moveTop() {
        checkWidget();
        int[] indicesToMove = fSelectedItemsTable.getSelectionIndices();
        Arrays.sort(indicesToMove);
        int i = 0;
        for (int index : indicesToMove) {
            DLItem item = fSelectedItems.remove(index);
            fSelectedItems.add(i, item);
            i++;
        }
        refreshTables();
        fSelectedItemsTable.select(0, --i);
        fSelectedItemsTable.forceFocus();
    }

    private void moveBottom() {
        checkWidget();
        int[] indicesToMove = fSelectedItemsTable.getSelectionIndices();
        Arrays.sort(indicesToMove);
        for (int index : indicesToMove) {
            fSelectedItems.add(fSelectedItems.get(index));
        }
        for (int i = fSelectedItems.size() - 1; i >= 0; --i) {
            if (arrayHasElement(indicesToMove, i)) {
                fSelectedItems.remove(i);
            }
        }
        refreshTables();
        fSelectedItemsTable.select(fSelectedItems.size() - indicesToMove.length, fSelectedItems.size());
        fSelectedItemsTable.forceFocus();
    }

    private void moveUp() {
        checkWidget();
        int[] indicesToMove = fSelectedItemsTable.getSelectionIndices();
        Arrays.sort(indicesToMove);
        if (arrayHasElement(indicesToMove, 0)) {
            return;
        }
        for (int i = 0; i < indicesToMove.length; ++i) {
            swapSelected(indicesToMove[i], indicesToMove[i] - 1);
            indicesToMove[i] = indicesToMove[i] - 1;
        }
        refreshTables();
        fSelectedItemsTable.select(indicesToMove);
        fSelectedItemsTable.forceFocus();
    }

    private void moveDown() {
        checkWidget();
        int[] indicesToMove = fSelectedItemsTable.getSelectionIndices();
        Arrays.sort(indicesToMove);
        if (arrayHasElement(indicesToMove, fSelectedItems.size() - 1)) {
            return;
        }
        for (int i = indicesToMove.length - 1; i >= 0; --i) {
            swapSelected(indicesToMove[i], indicesToMove[i] + 1);
            indicesToMove[i] = indicesToMove[i] + 1;
        }
        refreshTables();
        fSelectedItemsTable.select(indicesToMove);
        fSelectedItemsTable.forceFocus();
    }

    // Helper section //

    private void swapSelected(int index, int otherIndex) {
        DLItem item = fSelectedItems.get(index);
        fSelectedItems.set(index, fSelectedItems.get(otherIndex));
        fSelectedItems.set(otherIndex, item);
    }

    private static boolean arrayHasElement(int[] array, int element) {
        for (int e : array) {
            if (e == element) {
                return true;
            }
        }
        return false;
    }

    private void refreshTables() {
        setRedraw(false);
        refreshTable(fOriginalItemsTable, fOriginalItems);
        refreshTable(fSelectedItemsTable, fSelectedItems);
        setRedraw(true);
        setBounds(getBounds());
        getParent().layout(true, true);
    }

    private void refreshTable(Table table, List<DLItem> items) {
        cleanTable(table);
        boolean hasImages = hasImages(fOriginalItems) || hasImages(fSelectedItems);
        new TableColumn(table, SWT.LEFT);
        int textColumnIndex = 0;
        if (hasImages) {
            textColumnIndex = 1;
            new TableColumn(table, SWT.LEFT);
        }
        for (DLItem item : items) {
            TableItem tableItem = new TableItem(table, SWT.NONE);
            Image image = item.getImage();
            if (image != null) {
                tableItem.setImage(0, image);
            }
            tableItem.setText(textColumnIndex, item.getText());
        }
    }

    private static boolean hasImages(List<DLItem> items) {
        for (DLItem item : items) {
            if (item.getImage() != null) {
                return true;
            }
        }
        return false;
    }

    private static void cleanTable(Table table) {
        for (TableItem item : table.getItems()) {
            item.dispose();
        }
        for (TableColumn column : table.getColumns()) {
            column.dispose();
        }
    }

    private void fireSelectionChangeEvent(final List<DLItem> items) {
        if (fSelectionChangeListeners == null) {
            return;
        }

        final Event event = new Event();
        event.button = 1;
        event.display = getDisplay();
        event.widget = this;
        final SelectionChangeEvent selectionChangeEvent = new SelectionChangeEvent(event);
        selectionChangeEvent.setItems(items);

        for (final SelectionChangeListener listener : fSelectionChangeListeners) {
            listener.widgetSelected(selectionChangeEvent);
        }
    }

    // Creation section //

    private void createOriginalItemsTable() {
        fOriginalItemsTable = createTable(this);
        fOriginalItemsTable.addListener(SWT.MouseDoubleClick,
                e -> selectItems(fOriginalItemsTable.getSelectionIndices()));
    }

    private void createSelectedItemsTable() {
        fSelectedItemsTable = createTable(this);
        fSelectedItemsTable.addListener(SWT.MouseDoubleClick,
                e -> deselectItems(fSelectedItemsTable.getSelectionIndices()));
    }

    private void createSelectAllButton() {
        createButton(this, SELECT_ALL_IMAGE).addListener(SWT.Selection,
                e -> selectItems(IntStream.range(0, fOriginalItemsTable.getItemCount()).toArray()));
    }

    private void createMoveTopButton() {
        createButton(this, MOVE_TOP_IMAGE).addListener(SWT.Selection, e -> moveTop());
    }

    private void createButtonSelect() {
        createButton(this, SELECT_IMAGE)
                .addListener(SWT.Selection, e -> selectItems(fOriginalItemsTable.getSelectionIndices()));
    }

    private void createButtonMoveUp() {
        createButton(this, MOVE_UP_IMAGE).addListener(SWT.Selection, e -> moveUp());
    }

    private void createButtonDeselect() {
        createButton(this, DESELECT_IMAGE)
                .addListener(SWT.Selection, e -> deselectItems(fSelectedItemsTable.getSelectionIndices()));
    }

    private void createButtonMoveDown() {
        createButton(this, MOVE_DOWN_IMAGE).addListener(SWT.Selection, e -> moveDown());
    }

    private void createButtonDeselectAll() {
        createButton(this, DESELECT_ALL_IMAGE).addListener(SWT.Selection,
                e -> deselectItems(IntStream.range(0, fSelectedItemsTable.getItemCount()).toArray()));
    }

    private void createButtonMoveBottom() {
        createButton(this, MOVE_BOTTOM_IMAGE).addListener(SWT.Selection, e -> moveBottom());
    }

    private static Table createTable(Composite parent) {
        final Table table = new Table(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
        table.setLinesVisible(false);
        table.setHeaderVisible(false);

        final GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true, 1, 4);
        gridData.widthHint = 200;
        table.setLayoutData(gridData);

        return table;
    }

    private static Button createButton(Composite parent, final String fileName) {
        final Button button = new Button(parent, SWT.PUSH);
        final Image image = SWTGraphicUtil.createImageFromFile("images/" + fileName); //$NON-NLS-1$
        button.setImage(image);
        button.setLayoutData(new GridData(GridData.CENTER, SWT.BEGINNING, false, false));
        SWTGraphicUtil.addDisposer(button, image);
        return button;
    }
}
