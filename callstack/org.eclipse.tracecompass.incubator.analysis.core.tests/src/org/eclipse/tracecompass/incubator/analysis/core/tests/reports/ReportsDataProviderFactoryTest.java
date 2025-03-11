/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.analysis.core.tests.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.analysis.core.reports.ReportsDataProviderFactory;
import org.eclipse.tracecompass.internal.analysis.profiling.core.model.ModelManager;
import org.eclipse.tracecompass.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.tracecompass.lttng2.lttng.kernel.core.tests.shared.LttngKernelTestTraceUtils;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for {@link ReportsDataProviderFactory}
 *
 * @author Kaveh Shahedi
 */
public class ReportsDataProviderFactoryTest {

    private ReportsDataProviderFactory fFactory;

    private static final @NonNull CtfTestTrace TEST_TRACE = CtfTestTrace.KERNEL;
    private static CtfTmfTrace fTrace;

    private static final String FACTORY_ID = "org.eclipse.tracecompass.incubator.analysis.core.reports.reportsDataProviderFactory";
    private static final String PARENT_ID = "parentId";

    private static final String FOLDER_ID = "test-folder";
    private static final String FOLDER_NAME = "Test Folder";
    private static final String FOLDER_DESCRIPTION = "Test folder description";
    private static final String PARENT_FOLDER_ID = "parent-folder";
    private static final String PARENT_FOLDER_NAME = "Parent Folder";
    private static final String PARENT_FOLDER_DESCRIPTION = "Parent folder description";
    private static final String CHILD_FOLDER_ID = "child-folder";
    private static final String CHILD_FOLDER_NAME = "Child Folder";
    private static final String CHILD_FOLDER_DESCRIPTION = "Child folder description";

    /**
     * Set up the tests
     */
    @BeforeClass
    public static void beforeClass() {
        LttngKernelTrace trace = LttngKernelTestTraceUtils.getTrace(TEST_TRACE);
        TmfSignalManager.dispatchSignal(new TmfTraceOpenedSignal(ReportsDataProviderFactoryTest.class, trace, null));
        fTrace = trace;
    }

    /**
     * Clean up after the tests
     */
    @SuppressWarnings("restriction")
    @AfterClass
    public static void afterClass() {
        if (fTrace != null) {
            TmfSignalManager.dispatchSignal(new TmfTraceClosedSignal(ReportsDataProviderFactoryTest.class, fTrace));
        }

        ModelManager.disposeModels();

        LttngKernelTestTraceUtils.dispose(CtfTestTrace.KERNEL);
        fTrace = null;
    }

    /**
     * Setup before each test
     */
    @Before
    public void setUp() {
        fFactory = new ReportsDataProviderFactory();
    }

    /**
     * Cleanup after each test
     */
    @After
    @SuppressWarnings("null")
    public void tearDown() {
        try {
            Collection<IDataProviderDescriptor> descriptors = fFactory.getDescriptors(fTrace);
            for (IDataProviderDescriptor descriptor : descriptors) {
                if (descriptor.getId().equals(FACTORY_ID)) {
                    continue;
                }
                fFactory.removeDataProviderDescriptor(fTrace, descriptor);
            }
        } catch (TmfConfigurationException e) {
            fail("Error removing configurations: " + e.getMessage());
        }

        if (fFactory != null) {
            fFactory.dispose();
            fFactory = null;
        }
    }

    /**
     * Test getting descriptors without any configurations
     */
    @Test
    public void testGetDescriptorsEmpty() {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        Collection<IDataProviderDescriptor> descriptors = fFactory.getDescriptors(trace);

        // Should only have the factory descriptor itself
        assertEquals(1, descriptors.size());

        IDataProviderDescriptor factoryDescriptor = descriptors.iterator().next();
        assertEquals(FACTORY_ID, factoryDescriptor.getId());
    }

    /**
     * Test creating a new folder/group descriptor
     *
     * @throws TmfConfigurationException
     *             If configuration is invalid
     */
    @Test
    public void testCreateFolderDescriptor() throws TmfConfigurationException {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        // Create a folder configuration
        ITmfConfiguration folderConfig = new TmfConfiguration.Builder()
                .setId(FOLDER_ID)
                .setSourceTypeId(fFactory.getConfigurationSourceType().getId())
                .setName(FOLDER_NAME)
                .setDescription(FOLDER_DESCRIPTION)
                .build();

        // Create descriptor
        IDataProviderDescriptor descriptor = fFactory.createDataProviderDescriptors(trace, folderConfig);

        assertNotNull(descriptor);
        assertNotNull(descriptor.getConfiguration());
        assertEquals(FOLDER_NAME, descriptor.getName());
        assertEquals(FOLDER_DESCRIPTION, descriptor.getDescription());
        assertFalse(descriptor.getCapabilities().canCreate());
        assertTrue(descriptor.getCapabilities().canDelete());

        // The descriptor should now be in the list of descriptors
        Collection<IDataProviderDescriptor> descriptors = fFactory.getDescriptors(trace);
        assertEquals(2, descriptors.size());
        assertNotNull(descriptors.stream().filter(d -> d.getId().equals(descriptor.getId())).findFirst().orElse(null));
    }

    /**
     * Test creating nested folder descriptors
     *
     * @throws TmfConfigurationException
     *             If configuration is invalid
     */
    @SuppressWarnings("null")
    @Test
    public void testCreateNestedFolderDescriptors() throws TmfConfigurationException {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        // Create parent folder
        ITmfConfiguration parentConfig = new TmfConfiguration.Builder()
                .setId(PARENT_FOLDER_ID)
                .setSourceTypeId(fFactory.getConfigurationSourceType().getId())
                .setName(PARENT_FOLDER_NAME)
                .setDescription(PARENT_FOLDER_DESCRIPTION)
                .build();

        IDataProviderDescriptor parentDescriptor = fFactory.createDataProviderDescriptors(trace, parentConfig);
        ITmfConfiguration parentConfiguration = parentDescriptor.getConfiguration();
        assertNotNull(parentDescriptor);
        assertNotNull(parentConfiguration);

        // Create child folder with parent ID reference
        Map<String, Object> paramsChild = new HashMap<>();
        paramsChild.put(PARENT_ID, parentConfiguration.getId());
        ITmfConfiguration childConfig = new TmfConfiguration.Builder()
                .setId(CHILD_FOLDER_ID)
                .setSourceTypeId(fFactory.getConfigurationSourceType().getId())
                .setName(CHILD_FOLDER_NAME)
                .setDescription(CHILD_FOLDER_DESCRIPTION)
                .setParameters(paramsChild)
                .build();

        IDataProviderDescriptor childDescriptor = fFactory.createDataProviderDescriptors(trace, childConfig);
        assertNotNull(childDescriptor);

        // Get all descriptors - should have factory + parent + child
        Collection<IDataProviderDescriptor> descriptors = fFactory.getDescriptors(trace);
        assertEquals(3, descriptors.size());
        assertNotNull(descriptors.stream().filter(d -> d.getId().equals(parentDescriptor.getId())).findFirst().orElse(null));
        assertNotNull(descriptors.stream().filter(d -> d.getId().equals(childDescriptor.getId())).findFirst().orElse(null));
    }

    /**
     * Test removing a folder descriptor with no children
     *
     * @throws TmfConfigurationException
     *             If configuration is invalid
     */
    @Test
    public void testRemoveFolderDescriptor() throws TmfConfigurationException {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        // Create a folder
        ITmfConfiguration folderConfig = new TmfConfiguration.Builder()
                .setId(FOLDER_ID)
                .setSourceTypeId(fFactory.getConfigurationSourceType().getId())
                .setName(FOLDER_NAME)
                .setDescription(FOLDER_DESCRIPTION)
                .build();

        IDataProviderDescriptor descriptor = fFactory.createDataProviderDescriptors(trace, folderConfig);
        assertNotNull(descriptor);

        // Verify descriptor was created
        Collection<IDataProviderDescriptor> descriptors = fFactory.getDescriptors(trace);
        assertEquals(2, descriptors.size());
        assertNotNull(descriptors.stream().filter(d -> d.getId().equals(descriptor.getId())).findFirst().orElse(null));

        // Remove the descriptor
        fFactory.removeDataProviderDescriptor(trace, descriptor);

        // Verify descriptor was removed
        descriptors = fFactory.getDescriptors(trace);
        assertEquals(1, descriptors.size());
        assertFalse(descriptors.stream().filter(d -> d.getId().equals(descriptor.getId())).findFirst().isPresent());
    }

    /**
     * Test removing a parent folder with a child
     *
     * @throws TmfConfigurationException
     *             If configuration is invalid
     */
    @SuppressWarnings("null")
    @Test
    public void testRemoveFolderWithChild() throws TmfConfigurationException {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        // Create parent folder
        ITmfConfiguration parentConfig = new TmfConfiguration.Builder()
                .setId(PARENT_FOLDER_ID)
                .setSourceTypeId(fFactory.getConfigurationSourceType().getId())
                .setName(PARENT_FOLDER_NAME)
                .setDescription(PARENT_FOLDER_DESCRIPTION)
                .build();

        IDataProviderDescriptor parentDescriptor = fFactory.createDataProviderDescriptors(trace, parentConfig);
        ITmfConfiguration parentConfiguration = parentDescriptor.getConfiguration();
        assertNotNull(parentDescriptor);
        assertNotNull(parentConfiguration);

        // Create child folder
        Map<String, Object> paramsChild = new HashMap<>();
        paramsChild.put(PARENT_ID, parentConfiguration.getId());
        ITmfConfiguration childConfig = new TmfConfiguration.Builder()
                .setId(CHILD_FOLDER_ID)
                .setSourceTypeId(fFactory.getConfigurationSourceType().getId())
                .setName(CHILD_FOLDER_NAME)
                .setDescription(CHILD_FOLDER_DESCRIPTION)
                .setParameters(paramsChild)
                .build();

        IDataProviderDescriptor childDescriptor = fFactory.createDataProviderDescriptors(trace, childConfig);
        assertNotNull(childDescriptor);

        // Verify both descriptors were created
        Collection<IDataProviderDescriptor> descriptors = fFactory.getDescriptors(trace);
        assertEquals(3, descriptors.size());
        assertNotNull(descriptors.stream().filter(d -> d.getId().equals(parentDescriptor.getId())).findFirst().orElse(null));
        assertNotNull(descriptors.stream().filter(d -> d.getId().equals(childDescriptor.getId())).findFirst().orElse(null));

        // Remove the parent - should recursively remove the child
        fFactory.removeDataProviderDescriptor(trace, parentDescriptor);

        // Verify both descriptors were removed
        descriptors = fFactory.getDescriptors(trace);
        assertEquals(1, descriptors.size());
        assertFalse(descriptors.stream().filter(d -> d.getId().equals(parentDescriptor.getId())).findFirst().isPresent());
        assertFalse(descriptors.stream().filter(d -> d.getId().equals(childConfig.getId())).findFirst().isPresent());
    }

    /**
     * Test getting configuration source types
     */
    @SuppressWarnings("null")
    @Test
    public void testGetConfigurationSourceTypes() {
        List<ITmfConfigurationSourceType> sourceTypes = fFactory.getConfigurationSourceTypes();
        assertFalse(sourceTypes.isEmpty());

        // Check that each source type has parameters
        for (ITmfConfigurationSourceType sourceType : sourceTypes) {
            assertNotNull(sourceType.getId());
            List<ITmfConfigParamDescriptor> paramDescriptors = sourceType.getConfigParamDescriptors();
            assertFalse(paramDescriptors.isEmpty());

            // Check that each source type includes the parent ID parameter
            boolean hasParentIdParam = false;
            for (ITmfConfigParamDescriptor paramDesc : paramDescriptors) {
                if (paramDesc.getKeyName().equals(PARENT_ID)) {
                    hasParentIdParam = true;
                    break;
                }
            }
            assertTrue("Source type " + sourceType.getId() + " is missing parentId parameter", hasParentIdParam);
        }
    }

    /**
     * Test validating configuration with invalid parent ID
     */
    @SuppressWarnings("null")
    @Test
    public void testValidateConfigurationInvalidParent() {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        Map<String, Object> params = new HashMap<>();
        params.put(PARENT_ID, "non-existent-parent");
        ITmfConfiguration config = new TmfConfiguration.Builder()
                .setId(FOLDER_ID)
                .setSourceTypeId(fFactory.getConfigurationSourceType().getId())
                .setName(FOLDER_NAME)
                .setDescription(FOLDER_DESCRIPTION)
                .setParameters(params)
                .build();

        try {
            fFactory.createDataProviderDescriptors(trace, config);
            fail("Should have thrown TmfConfigurationException for invalid parent");
        } catch (TmfConfigurationException e) {

        }
    }

    /**
     * Test ConfigurationPath resolution and base path utilities
     *
     * @throws TmfConfigurationException
     *             If configuration is invalid
     */
    @Test
    public void testConfigurationPaths() throws TmfConfigurationException {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        // Create a folder config
        ITmfConfiguration folderConfig = new TmfConfiguration.Builder()
                .setId(FOLDER_ID)
                .setSourceTypeId(fFactory.getConfigurationSourceType().getId())
                .setName(FOLDER_NAME)
                .setDescription(FOLDER_DESCRIPTION)
                .build();

        IDataProviderDescriptor descriptor = fFactory.createDataProviderDescriptors(trace, folderConfig);
        ITmfConfiguration descriptorConfig = descriptor.getConfiguration();
        assertNotNull(descriptor);
        assertNotNull(descriptorConfig);

        // Get the base path for the configuration
        IPath basePath = ReportsDataProviderFactory.getConfigurationBasePath(trace, descriptorConfig);
        assertNotNull(basePath);
        assertTrue(basePath.toString().contains(FACTORY_ID));
        assertTrue(basePath.toString().contains(descriptorConfig.getId()));

        // Should create a folder - verify it exists
        File baseFolder = basePath.toFile();
        assertTrue("Configuration folder should be created", baseFolder.exists());
        assertTrue("Configuration path should be a directory", baseFolder.isDirectory());
    }

    /**
     * Test getting a configuration parent
     *
     * @throws TmfConfigurationException
     *             If configuration is invalid
     */
    @SuppressWarnings("null")
    @Test
    public void testGetConfigurationParent() throws TmfConfigurationException {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        // Create parent folder
        ITmfConfiguration parentConfig = new TmfConfiguration.Builder()
                .setId(PARENT_FOLDER_ID)
                .setSourceTypeId(fFactory.getConfigurationSourceType().getId())
                .setName(PARENT_FOLDER_NAME)
                .setDescription(PARENT_FOLDER_DESCRIPTION)
                .build();

        IDataProviderDescriptor parentDescriptor = fFactory.createDataProviderDescriptors(trace, parentConfig);
        ITmfConfiguration parentConfiguration = parentDescriptor.getConfiguration();
        assertNotNull(parentDescriptor);
        assertNotNull(parentConfiguration);

        // Create child folder
        Map<String, Object> paramsChild = new HashMap<>();
        paramsChild.put(PARENT_ID, parentConfiguration.getId());
        ITmfConfiguration childConfig = new TmfConfiguration.Builder()
                .setId(CHILD_FOLDER_ID)
                .setSourceTypeId(fFactory.getConfigurationSourceType().getId())
                .setName(CHILD_FOLDER_NAME)
                .setDescription(CHILD_FOLDER_DESCRIPTION)
                .setParameters(paramsChild)
                .build();

        IDataProviderDescriptor childDescriptor = fFactory.createDataProviderDescriptors(trace, childConfig);
        ITmfConfiguration childConfiguration = childDescriptor.getConfiguration();
        assertNotNull(childDescriptor);
        assertNotNull(childConfiguration);

        // Get the parent of the child configuration
        ITmfConfiguration retrievedParent = ReportsDataProviderFactory.getConfigurationParent(trace, childConfiguration);
        assertNotNull(retrievedParent);
        assertEquals(parentConfiguration.getId(), retrievedParent.getId());
        assertEquals(parentConfiguration.getName(), retrievedParent.getName());
        assertEquals(parentConfiguration.getDescription(), retrievedParent.getDescription());

        // Test with no parent (root-level folder) - should return null
        ITmfConfiguration noParent = ReportsDataProviderFactory.getConfigurationParent(trace, parentConfiguration);
        assertEquals(null, noParent);
    }

    /**
     * Test loading configurations from files
     *
     * @throws TmfConfigurationException
     *             If configuration is invalid
     */
    @Test
    public void testLoadConfigurations() throws TmfConfigurationException {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        // Create a folder
        ITmfConfiguration folderConfig = new TmfConfiguration.Builder()
                .setId(FOLDER_ID)
                .setSourceTypeId(fFactory.getConfigurationSourceType().getId())
                .setName(FOLDER_NAME)
                .setDescription(FOLDER_DESCRIPTION)
                .build();

        IDataProviderDescriptor descriptor = fFactory.createDataProviderDescriptors(trace, folderConfig);
        assertNotNull(descriptor);

        // Close the trace to clear configurations
        TmfTraceClosedSignal closeSignal = new TmfTraceClosedSignal(this, trace);
        TmfSignalManager.dispatchSignal(closeSignal);

        // Verify configurations are cleared
        Collection<IDataProviderDescriptor> clearedDescriptors = fFactory.getDescriptors(trace);
        assertEquals(1, clearedDescriptors.size());
        assertEquals(FACTORY_ID, clearedDescriptors.iterator().next().getId());

        // Reopen the trace - should load configurations from files
        TmfTraceOpenedSignal openSignal = new TmfTraceOpenedSignal(this, trace, null);
        TmfSignalManager.dispatchSignal(openSignal);

        // Configurations should now be loaded
        Collection<IDataProviderDescriptor> descriptors = fFactory.getDescriptors(trace);
        assertEquals(2, descriptors.size());
        assertNotNull(descriptors.stream().filter(d -> d.getId().equals(FACTORY_ID)).findFirst().orElse(null));
        assertNotNull(descriptors.stream().filter(d -> d.getId().equals(descriptor.getId())).findFirst().orElse(null));
    }
}