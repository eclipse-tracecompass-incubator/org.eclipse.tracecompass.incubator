/**********************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.inandout.core.tests.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.inandout.core.analysis.InAndOutDataProviderFactory;
import org.eclipse.tracecompass.incubator.internal.inandout.core.analysis.SegmentSpecifierConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.ITmfDataProviderConfigurator;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.tests.TmfCoreTestPlugin;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Class to test {@link InAndOutDataProviderFactory}
 */
@SuppressWarnings("null")
public class InAndOutDataProviderFactoryTest {

    private static final String EXPECTED_FACTORY_ID = "org.eclipse.tracecompass.incubator.inandout.core.analysis.inAndOutdataProviderFactory"; //$NON-NLS-1$
    private static final String EXPECTED_CONFIGURATOR_NAME = "InAndOut Configurator"; //$NON-NLS-1$
    private static final String EXPECTED_CONFIGURATOR_DESCRIPTION = "Configure custom InAndOut analysis"; //$NON-NLS-1$

    private static final String NAME = "In And Out Analysis"; //$NON-NLS-1$
    private static final String DESCRIPTION = "Configure In And Out analysis using file description"; //$NON-NLS-1$
    private static final String CONFIG_NAME = "An InAndOut Analysis"; //$NON-NLS-1$

    private static final String CUSTOM_IN_AND_OUT_ANALYSIS_NAME = "InAndOut Analysis (" + CONFIG_NAME + ")"; //$NON-NLS-1$
    private static final String CUSTOM_IN_AND_OUT_ANALYSIS_DESCRIPTION = "Custom InAndOut analysis configured by \" " + CONFIG_NAME + "\""; //$NON-NLS-1$

    private static final String XML_TRACE = "testfiles/stub_xml_traces/valid/analysis_dependency.xml";
    private static ITmfTrace sfTestTrace;

    private static ITmfConfiguration sfTestConfig;

    private static final IDataProviderDescriptor EXPECTED_DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(EXPECTED_FACTORY_ID)
            .setName(EXPECTED_CONFIGURATOR_NAME)
            .setDescription(EXPECTED_CONFIGURATOR_DESCRIPTION)
            .setProviderType(ProviderType.NONE)
            .build();

    private static InAndOutDataProviderFactory sfFixture = new InAndOutDataProviderFactory();

    /**
     * Test class setup method
     */
    @BeforeClass
    public static void setup() {
        TmfXmlTraceStub trace = TmfXmlTraceStubNs.setupTrace(TmfCoreTestPlugin.getAbsoluteFilePath(XML_TRACE));
        trace.traceOpened(new TmfTraceOpenedSignal(trace, trace, null));
        sfTestTrace = trace;

        Map<String, Object> params = new HashMap<>();
        List<Map<String, Object>> identifiers = new ArrayList<>();
        Map<String, Object> identifier = new HashMap<>();
        identifier.put("label", "Latency");
        identifier.put("inRegex", "(\\S*)_entry");
        identifier.put("outRegex", "(\\S*)_exit");
        identifier.put("contextInRegex", "(\\S*)_entry");
        identifier.put("contextOutRegex", "(\\S*)_exit");
        identifier.put("classifier", "CPU");
        identifiers.add(identifier);
        params.put("identifiers", identifiers);

        sfTestConfig = new TmfConfiguration.Builder()
                .setName(CONFIG_NAME)
                .setSourceTypeId(SegmentSpecifierConfiguration.IN_AND_OUT_CONFIG_SOURCE_TYPE_ID)
                .setParameters(params)
                .build();
    }

    /**
     * Test class clean method
     */
    @AfterClass
    public static void cleanup() {
        if (sfTestTrace != null) {
            sfTestTrace.dispose();
        }
        if (sfFixture != null) {
            TmfSignalManager.deregister(sfFixture);
        }
    }

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------
    /**
     * Test {@link IDataProviderFactory#getAdapter(Class)}
     */
    @Test
    public void testAdapter() {
        ITmfDataProviderConfigurator configurator = sfFixture.getAdapter(ITmfDataProviderConfigurator.class);
        assertNotNull(configurator);
    }

    /**
     * Test {@link ITmfDataProviderConfigurator#getConfigurationSourceTypes()}
     */
    @Test
    public void testGetConfigurationSourceTypes() {
        ITmfDataProviderConfigurator configurator = sfFixture.getAdapter(ITmfDataProviderConfigurator.class);
        assertNotNull(configurator);

        List<ITmfConfigurationSourceType> types = configurator.getConfigurationSourceTypes();
        assertFalse(types.isEmpty());
        assertEquals(1, types.size());
        ITmfConfigurationSourceType type = types.get(0);
        assertEquals(NAME, type.getName());
        assertEquals(DESCRIPTION, type.getDescription());
        assertEquals(SegmentSpecifierConfiguration.IN_AND_OUT_CONFIG_SOURCE_TYPE_ID, type.getId());
        // Omit testing schema details
        assertNotNull(type.getSchemaFile());
        assertTrue(type.getConfigParamDescriptors().isEmpty());
    }

    /**
     * Test
     * {@link ITmfDataProviderConfigurator#createDataProviderDescriptors(ITmfTrace, ITmfConfiguration)}
     *
     * @throws TmfConfigurationException
     *             if a config error happens
     */
    @Test
    public void testCreateAndDeleteDataProviderDescriptor() throws TmfConfigurationException {
        ITmfDataProviderConfigurator configurator = sfFixture.getAdapter(ITmfDataProviderConfigurator.class);
        assertNotNull(configurator);

        IDataProviderDescriptor descriptor = configurator.createDataProviderDescriptors(sfTestTrace, sfTestConfig);
        assertEquals(CUSTOM_IN_AND_OUT_ANALYSIS_NAME, descriptor.getName());
        assertEquals(CUSTOM_IN_AND_OUT_ANALYSIS_DESCRIPTION, descriptor.getDescription());
        assertEquals(ProviderType.NONE, descriptor.getType());
        assertEquals(EXPECTED_FACTORY_ID, descriptor.getParentId());
        ITmfConfiguration config = descriptor.getConfiguration();
        assertNotNull(config);

        assertEquals(sfTestConfig.getName(), config.getName());
        assertEquals(sfTestConfig.getDescription(), config.getDescription());
        assertEquals(sfTestConfig.getSourceTypeId(), config.getSourceTypeId());
        assertEquals(sfTestConfig.getParameters(), config.getParameters());

        List<IDataProviderDescriptor> descriptors = DataProviderManager.getInstance().getAvailableProviders(sfTestTrace);
        assertTrue(descriptors.contains(descriptor));

        configurator.removeDataProviderDescriptor(sfTestTrace, descriptor);
        descriptors = DataProviderManager.getInstance().getAvailableProviders(sfTestTrace);
        assertFalse(descriptors.contains(descriptor));
    }

    /**
     * Test {@link IDataProviderFactory#createProvider(ITmfTrace)} and
     * {@link IDataProviderFactory#createProvider(ITmfTrace, String)}
     */
    @Test
    public void testCreateProvider() {
        ITmfTreeDataProvider<?> dp = sfFixture.createProvider(sfTestTrace);
        assertNull(dp);
        dp = sfFixture.createProvider(sfTestTrace, "test");
        assertNull(dp);
    }

    /**
     * Test {@link IDataProviderFactory#getDescriptors(ITmfTrace)}
     */
    @Test
    public void testGetDescriptor() {
        Collection<IDataProviderDescriptor> descriptors = sfFixture.getDescriptors(sfTestTrace);
        assertTrue(descriptors.contains(EXPECTED_DESCRIPTOR));
    }
}
