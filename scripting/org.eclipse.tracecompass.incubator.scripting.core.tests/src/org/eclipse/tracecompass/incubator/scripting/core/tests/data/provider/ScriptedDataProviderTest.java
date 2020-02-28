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

package org.eclipse.tracecompass.incubator.scripting.core.tests.data.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.scripting.core.data.provider.ScriptedEntryDataModel;
import org.eclipse.tracecompass.incubator.scripting.core.analysis.ScriptedAnalysis;
import org.eclipse.tracecompass.incubator.scripting.core.data.provider.DataProviderScriptingModule;
import org.eclipse.tracecompass.incubator.scripting.core.tests.stubs.ScriptingTestUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Test the {@link DataProviderScriptingModule} class methods
 *
 * @author Geneviève Bastien
 */
public class ScriptedDataProviderTest {

    private static final @NonNull String ANALYSIS_NAME = "scriptAnalysisTest";
    private static final @NonNull IProgressMonitor NULL_MONITOR = new NullProgressMonitor();

    private static final @NonNull String ELEMENT_PARENT = "parent";
    private static final @NonNull String ELEMENT1 = "321";
    private static final @NonNull String ELEMENT2 = "123";

    private @Nullable ITmfTrace fTrace;

    private ScriptedAnalysis getFixture() {
        ITmfTrace trace = fTrace;
        assertNotNull(trace);
        return ScriptingTestUtils.getAnalysis(trace, ANALYSIS_NAME);
    }

    /**
     * Prepare the trace for the test
     */
    @Before
    public void setUpData() {
        fTrace = ScriptingTestUtils.getTrace();
    }

    /**
     * Dispose of objects after the test
     */
    @After
    public void cleanup() {
        ITmfTrace trace = fTrace;
        if (trace != null) {
            trace.dispose();
        }
    }

    private static void fillFixtureForTGTest(ITmfStateSystemBuilder ss) {
        /**
         * Simple state system hierarchy
         *
         * <pre>
         * +el1-- + el2-- + el3
         * </pre>
         */
        int quark = ss.getQuarkAbsoluteAndAdd(ELEMENT_PARENT);
        ss.modifyAttribute(1, "test", quark);
        quark = ss.getQuarkAbsoluteAndAdd(ELEMENT_PARENT, ELEMENT1);
        ss.modifyAttribute(2, 123, quark);
        ss.removeAttribute(10, quark);
        quark = ss.getQuarkAbsoluteAndAdd(ELEMENT_PARENT, ELEMENT2);
        ss.modifyAttribute(5, 321, quark);
        ss.closeHistory(15);
    }

    /**
     * Test the time graph providers generated with parameters
     */
    @Test
    public void testTimeGraphProvider() {
        // Prepare the data
        ITmfTrace trace = fTrace;
        assertNotNull(trace);
        ScriptedAnalysis fixture = getFixture();
        ITmfStateSystemBuilder stateSystem = fixture.getStateSystem(false);
        fillFixtureForTGTest(stateSystem);

        DataProviderScriptingModule scriptingModule = new DataProviderScriptingModule();

        // Create a data provider that returns a flat hierarchy of objects
        ITimeGraphDataProvider<@NonNull TimeGraphEntryModel> dp = scriptingModule.createTimeGraphProvider(fixture, ImmutableMap.of(DataProviderScriptingModule.ENTRY_PATH, "*/*"), "test");
        assertNotNull(dp);

        /*
         * The data provider is a DataDrivenDataProvider, tested in
         * analysis.xml.core.tests. Here we just test it returns the right
         * number of entries and hierarchy
         */
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull TimeGraphEntryModel>> tree = dp.fetchTree(ImmutableMap.of(DataProviderParameterUtils.REQUESTED_TIME_KEY, ImmutableList.of(0, Integer.MAX_VALUE)), NULL_MONITOR);
        assertEquals("Simple dp tree", Status.COMPLETED, tree.getStatus());
        TmfTreeModel<@NonNull TimeGraphEntryModel> model = tree.getModel();
        assertNotNull("Simple dp model", model);
        List<@NonNull TimeGraphEntryModel> entries = model.getEntries();
        assertEquals("Simple dp size", 3, entries.size());
        assertNotNull("Simple dp trace entry", findTimeGraphWithName(entries, trace.getName()));
        assertNotNull("Simple dp entry1", findTimeGraphWithName(entries, ELEMENT1));
        assertNotNull("Simple dp entry2", findTimeGraphWithName(entries, ELEMENT2));

    }

    private static @Nullable TimeGraphEntryModel findTimeGraphWithName(List<@NonNull TimeGraphEntryModel> entries, String name) {
        for (TimeGraphEntryModel entry : entries) {
            if (entry.getName().equals(name)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Test the scripted time graph providers
     *
     * @throws AttributeNotFoundException
     *             Exception thrown if the state system has wrong data
     */
    @Test
    public void testScriptedTimeGraphProvider() throws AttributeNotFoundException {
        // Prepare the data
        ITmfTrace trace = fTrace;
        assertNotNull(trace);
        ScriptedAnalysis fixture = getFixture();
        ITmfStateSystemBuilder stateSystem = fixture.getStateSystem(false);
        assertNotNull(stateSystem);
        fillFixtureForTGTest(stateSystem);

        DataProviderScriptingModule scriptingModule = new DataProviderScriptingModule();

        /*
         * Create a hierarchy of entries for the scripted data provider, test
         * the {@link DataProviderScriptingModule#createEntry} method
         */
        List<ITimeGraphEntryModel> entries = new ArrayList<>();
        // Parent entry
        int parentQuark = stateSystem.getQuarkAbsolute(ELEMENT_PARENT);
        ITmfTreeDataModel parentEntry = scriptingModule.createEntry(ELEMENT_PARENT, ImmutableMap.of(DataProviderScriptingModule.ENTRY_FIELD_QUARK, parentQuark));
        assertTrue(parentEntry instanceof ScriptedEntryDataModel);
        assertEquals(parentQuark, ((ScriptedEntryDataModel) parentEntry).getQuark());
        entries.add((ITimeGraphEntryModel) parentEntry);

        // First child
        int element1Quark = stateSystem.getQuarkAbsolute(ELEMENT_PARENT, ELEMENT1);
        ITmfTreeDataModel entry = scriptingModule.createEntry(ELEMENT1, ImmutableMap.of(DataProviderScriptingModule.ENTRY_FIELD_QUARK, element1Quark, DataProviderScriptingModule.ENTRY_FIELD_PARENT_ID, parentEntry.getId()));
        assertTrue(entry instanceof ScriptedEntryDataModel);
        assertEquals(element1Quark, ((ScriptedEntryDataModel) entry).getQuark());
        entries.add((ITimeGraphEntryModel) entry);

        // Second child
        int element2Quark = stateSystem.getQuarkAbsolute(ELEMENT_PARENT, ELEMENT2);
        entry = scriptingModule.createEntry(ELEMENT2, ImmutableMap.of(DataProviderScriptingModule.ENTRY_FIELD_QUARK, element2Quark, DataProviderScriptingModule.ENTRY_FIELD_PARENT_ID, parentEntry.getId()));
        assertTrue(entry instanceof ScriptedEntryDataModel);
        assertEquals(element2Quark, ((ScriptedEntryDataModel) entry).getQuark());
        entries.add((ITimeGraphEntryModel) entry);

        // Add an arrow
        ITimeGraphArrow arrow = scriptingModule.createArrow(entry.getId(), parentEntry.getId(), 3, 5, 1);
        assertNotNull(arrow);

        // Create a data provider that returns a flat hierarchy of objects
        ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> dp = scriptingModule.createScriptedTimeGraphProvider(fixture, m -> entries, null, m -> Collections.singletonList(arrow));
        assertNotNull(dp);

        // Verify the entries are the same
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull ITimeGraphEntryModel>> tree = dp.fetchTree(ImmutableMap.of(DataProviderParameterUtils.REQUESTED_TIME_KEY, ImmutableList.of(0, Integer.MAX_VALUE)), NULL_MONITOR);
        assertEquals("Scripted dp tree", Status.COMPLETED, tree.getStatus());
        TmfTreeModel<@NonNull ITimeGraphEntryModel> model = tree.getModel();
        assertNotNull("Scripted dp model", model);
        List<@NonNull ITimeGraphEntryModel> actualEntries = model.getEntries();
        assertEquals("Scripted dp size", entries.size(), actualEntries.size());
        assertEquals("Scripted dp entries", entries, actualEntries);

        // Verify the arrows
        TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> arrows = dp.fetchArrows(ImmutableMap.of(DataProviderParameterUtils.REQUESTED_TIME_KEY, ImmutableList.of(0, Integer.MAX_VALUE)), NULL_MONITOR);
        assertEquals("Scripted dp arrows", Status.COMPLETED, arrows.getStatus());
        List<@NonNull ITimeGraphArrow> arrowModel = arrows.getModel();
        assertNotNull("Arrows", arrowModel);
        assertEquals("Arrow count", 1, arrowModel.size());
        ITimeGraphArrow actualArrow = arrowModel.get(0);
        assertEquals("Arrow source", entry.getId(), actualArrow.getSourceId());
        assertEquals("Arrow destination", parentEntry.getId(), actualArrow.getDestinationId());
        assertEquals("Arrow start", 3, actualArrow.getStartTime());
        assertEquals("Arrow duration", 5, actualArrow.getDuration());
        assertEquals("Arrow value", 1, actualArrow.getValue());

    }

}
