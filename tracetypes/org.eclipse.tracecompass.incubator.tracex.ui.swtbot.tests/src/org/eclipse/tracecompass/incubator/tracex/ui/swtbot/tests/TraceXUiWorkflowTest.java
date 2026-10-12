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

package org.eclipse.tracecompass.incubator.tracex.ui.swtbot.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.incubator.internal.tracex.ui.view.TraceXView;
import org.eclipse.tracecompass.incubator.tracex.core.tests.TraceXTestData;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotTimeGraph;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.tracecompass.tmf.ui.tests.shared.WaitUtils;
import org.eclipse.tracecompass.tmf.ui.views.filter.FilterView;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests the complete TraceX desktop workflow with a generated trace. */
@RunWith(SWTBotJunit4ClassRunner.class)
@SuppressWarnings("restriction")
public class TraceXUiWorkflowTest {

    private static final String PROJECT_NAME = "TraceXWorkflow"; //$NON-NLS-1$
    private static final String TRACE_TYPE_ID =
            "org.eclipse.tracecompass.incubator.tracex.core.trace"; //$NON-NLS-1$
    private static final String VIEW_ID =
            "org.eclipse.tracecompass.incubator.tracex.ui.view"; //$NON-NLS-1$
    private static final String FILTER_NAME = "tracexTextAndField"; //$NON-NLS-1$
    private static final long THREAD_A = 0x10001000L;
    private static final long THREAD_B = 0x10002000L;
    private static final long ISR_CONTEXT = 0xFFFFFFFFL;

    private static SWTWorkbenchBot fBot;
    private static Path fTracePath;

    /** Creates the deterministic fixture and initializes SWTBot. */
    @BeforeClass
    public static void setUpClass() throws IOException {
        SWTBotUtils.initialize();
        SWTBotPreferences.TIMEOUT = 30000;
        fBot = new SWTWorkbenchBot();
        WaitUtils.waitForJobs();

        TraceXTestData.Fixture fixture = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x73000000L, 16, 2, 6, 5);
        byte[] data = fixture.fileBytes();
        fixture.putThread(data, 0, THREAD_A, "alpha"); //$NON-NLS-1$
        fixture.putThread(data, 1, THREAD_B, "beta"); //$NON-NLS-1$
        put(fixture, data, 0, THREAD_A, 6, 0, 0, 0);
        put(fixture, data, 1, THREAD_A, 5, 2, THREAD_B, 0);
        put(fixture, data, 2, ISR_CONTEXT, 3, 4, 12, 0);
        put(fixture, data, 3, ISR_CONTEXT, 4, 6, 12, 0);
        put(fixture, data, 4, THREAD_B, 2, 8, 0, 0);
        fTracePath = Files.write(Files.createTempFile("tracex-ui-", ".trx"), //$NON-NLS-1$ //$NON-NLS-2$
                data);
    }

    /** Imports and opens the fixture for each test. */
    @Before
    public void setUp() {
        SWTBotUtils.createProject(PROJECT_NAME);
        SWTBotUtils.openTrace(PROJECT_NAME, fTracePath.toString(),
                TRACE_TYPE_ID);
    }

    /** Closes views, editors, and the temporary project. */
    @After
    public void tearDown() {
        closeView(VIEW_ID);
        closeView(FilterView.ID);
        fBot.closeAllEditors();
        SWTBotUtils.deleteProject(PROJECT_NAME, fBot);
    }

    /** Deletes the generated fixture. */
    @AfterClass
    public static void tearDownClass() throws IOException {
        Files.deleteIfExists(fTracePath);
    }

    /**
     * Tests import, clock configuration, selection, filters, view data,
     * cross-selection, and persisted configuration after reopening.
     */
    @Test
    public void testDesktopWorkflow() throws Exception {
        TraceXTrace configured = activeTrace();
        configured.setTicksPerSecond("2"); //$NON-NLS-1$
        assertEquals(2, configured.getTicksPerSecond().getAsLong());
        assertEquals(4_000_000_000L, configured.getEndTime().toNanos());

        fBot.closeAllEditors();
        WaitUtils.waitForJobs();
        openEditor();
        TraceXTrace original = activeTrace();
        assertNotSame(configured, original);
        assertEquals(2, original.getTicksPerSecond().getAsLong());

        SWTBotUtils.openView(VIEW_ID);
        SWTBotView viewBot = fBot.viewById(VIEW_ID);
        TraceXView view = (TraceXView) viewBot.getReference().getView(true);
        assertNotNull(view);
        SWTBotTimeGraph timeGraph = new SWTBotTimeGraph(viewBot.bot());
        timeGraph.getEntry(original.getName(), "CPU 0", "Execution"); //$NON-NLS-1$ //$NON-NLS-2$
        timeGraph.getEntry(original.getName(), "Contexts", "Thread", //$NON-NLS-1$ //$NON-NLS-2$
                "alpha [0x10001000]"); //$NON-NLS-1$

        SWTBotUtils.activateEditor(fBot, original.getName());
        SWTBotTable events = SWTBotUtils.activeEventsEditor(fBot).bot().table();
        fBot.waitUntil(new TableHasRows(events, 5));
        events.select(2);
        fBot.waitUntil(new SelectionReached(view));

        SWTBotTreeItem filterNode = createAndApplyFilter(events);
        fBot.waitUntil(ConditionHelpers.isTableCellFilled(events, "/5", 1, 1)); //$NON-NLS-1$
        assertEquals("1/5", events.cell(1, 1)); //$NON-NLS-1$
        filterNode.contextMenu("Delete").click(); //$NON-NLS-1$
        fBot.viewById(FilterView.ID).toolbarButton("Save filters").click(); //$NON-NLS-1$

        closeView(VIEW_ID);
        fBot.closeAllEditors();
        WaitUtils.waitForJobs();
        openEditor();
        WaitUtils.waitForJobs();
        TraceXTrace reopened = activeTrace();
        assertNotSame(original, reopened);
        assertEquals(2, reopened.getTicksPerSecond().getAsLong());
        assertEquals(4_000_000_000L, reopened.getEndTime().toNanos());
    }

    private static SWTBotTreeItem createAndApplyFilter(SWTBotTable events) {
        SWTBotUtils.openView(FilterView.ID);
        SWTBotView viewBot = fBot.viewById(FilterView.ID);
        viewBot.setFocus();
        SWTBot filterBot = viewBot.bot();
        SWTBotTree tree = filterBot.tree();

        viewBot.toolbarButton("Add new filter").click(); //$NON-NLS-1$
        tree.getTreeItem("FILTER <name>").select(); //$NON-NLS-1$
        SWTBotText text = filterBot.text();
        text.setFocus();
        text.setText(FILTER_NAME);
        SWTBotTreeItem filterNode = tree.getTreeItem("FILTER " + FILTER_NAME); //$NON-NLS-1$
        filterNode.contextMenu("TRACETYPE").click(); //$NON-NLS-1$
        filterNode.expand();
        SWTBotCCombo traceTypes = filterBot.ccomboBox();
        String traceType = Arrays.stream(traceTypes.items())
                .filter(item -> item.contains("TraceX")) //$NON-NLS-1$
                .findFirst().orElseThrow(AssertionError::new);
        traceTypes.setSelection(traceType);

        SWTBotTreeItem withTraceType = filterNode
                .getNode("WITH TRACETYPE " + traceType); //$NON-NLS-1$
        withTraceType.contextMenu("AND").click(); //$NON-NLS-1$
        withTraceType.expand();
        SWTBotTreeItem andNode = withTraceType.getNode("AND"); //$NON-NLS-1$
        andNode.contextMenu("CONTAINS").click(); //$NON-NLS-1$
        andNode.expand();
        filterBot.ccomboBox(1).setSelection("Event type"); //$NON-NLS-1$
        text = filterBot.text();
        text.setFocus();
        text.setText("TIME_SLICE"); //$NON-NLS-1$

        andNode.contextMenu("MATCHES").click(); //$NON-NLS-1$
        andNode.getNode(1).select();
        filterBot.ccomboBox(1).setSelection("Contents"); //$NON-NLS-1$
        text = filterBot.text(0);
        text.setFocus();
        text.setText("context_name"); //$NON-NLS-1$
        text = filterBot.text(1);
        text.setFocus();
        text.setText("alpha"); //$NON-NLS-1$
        viewBot.toolbarButton("Save filters").click(); //$NON-NLS-1$

        events.getTableItem(2).contextMenu(FILTER_NAME).click();
        return filterNode;
    }

    private static TraceXTrace activeTrace() {
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        assertTrue(trace instanceof TraceXTrace);
        return (TraceXTrace) trace;
    }

    private static void openEditor() {
        SWTBotUtils.openEditor(fBot, PROJECT_NAME,
                new org.eclipse.core.runtime.Path(fTracePath.getFileName()
                        .toString()));
    }

    private static void closeView(String id) {
        try {
            SWTBotUtils.closeViewById(id, fBot);
        } catch (WidgetNotFoundException e) {
            // The workflow can close this view before cleanup.
        }
    }

    private static void put(TraceXTestData.Fixture fixture, byte[] data,
            int index, long context, long eventId, long timestamp, long first,
            long fourth) {
        fixture.putEvent(data, index, context, 0x80010002L, eventId,
                timestamp, first, eventId == 3 || eventId == 4 ? first : 0,
                0, fourth);
    }

    private static final class TableHasRows extends DefaultCondition {
        private final SWTBotTable fTable;
        private final int fRows;

        private TableHasRows(SWTBotTable table, int rows) {
            fTable = table;
            fRows = rows;
        }

        @Override
        public boolean test() {
            return fTable.rowCount() >= fRows;
        }

        @Override
        public String getFailureMessage() {
            return "TraceX event table did not contain " + fRows + " rows"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static final class SelectionReached extends DefaultCondition {
        private final TraceXView fView;
        private long fContextSelection;
        private long fViewSelection;

        private SelectionReached(TraceXView view) {
            fView = view;
        }

        @Override
        public boolean test() {
            fContextSelection = TmfTraceManager.getInstance()
                    .getCurrentTraceContext().getSelectionRange()
                    .getStartTime().toNanos();
            fViewSelection = fView.getTimeGraphViewer()
                    .getSelectionBegin();
            return fContextSelection > 0 && fContextSelection == fViewSelection;
        }

        @Override
        public String getFailureMessage() {
            return "TraceX context selection was " + fContextSelection //$NON-NLS-1$
                    + " and view selection was " + fViewSelection; //$NON-NLS-1$
        }
    }
}
