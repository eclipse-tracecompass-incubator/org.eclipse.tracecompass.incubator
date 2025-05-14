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

import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.analysis.core.reports.IReportDataProvider.ReportProviderType;
import org.eclipse.tracecompass.internal.analysis.profiling.core.model.ModelManager;
import org.eclipse.tracecompass.incubator.analysis.core.reports.ImageReportDataProvider;
import org.eclipse.tracecompass.incubator.analysis.core.reports.ReportsDataProviderFactory;
import org.eclipse.tracecompass.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.tracecompass.lttng2.lttng.kernel.core.tests.shared.LttngKernelTestTraceUtils;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test class for {@link ImageReportDataProvider}
 *
 * @author Kaveh Shahedi
 */
public class ImageReportDataProviderTest {

    private ReportsDataProviderFactory fFactory;
    private ImageReportDataProvider fImageProvider;

    private static final @NonNull CtfTestTrace TEST_TRACE = CtfTestTrace.KERNEL;
    private static CtfTmfTrace fTrace;

    /** Temporary folder for test files */
    @Rule
    public TemporaryFolder fTempFolder = new TemporaryFolder();

    private static final String PATH_PARAM = "path";
    private static final String FACTORY_ID = "org.eclipse.tracecompass.incubator.analysis.core.reports.reportsDataProviderFactory";

    private static final String IMAGE_REPORT_ID = "test-image";
    private static final String IMAGE_REPORT_NAME = "Test Image";
    private static final String IMAGE_REPORT_DESCRIPTION = "Test image description";
    private static final String IMAGE_FILE_NAME = "test-image.png";

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
        fImageProvider = new ImageReportDataProvider();

        try {
            createTestImageFile();
        } catch (IOException e) {
            fail("Error creating test image file: " + e.getMessage());
        }
    }

    /**
     * Cleanup after each test
     */
    @After
    @SuppressWarnings({ "null", "restriction" })
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

        ModelManager.disposeModels();

        if (fFactory != null) {
            fFactory.dispose();
            fFactory = null;
        }
    }

    /**
     * Create test image file for testing
     */
    private void createTestImageFile() throws IOException {
        File pngFile = fTempFolder.newFile(IMAGE_FILE_NAME);
        Files.write(pngFile.toPath(), "fake png content".getBytes());
    }

    /**
     * Test the provider type
     */
    @Test
    public void testProviderType() {
        assertEquals(ReportProviderType.IMAGE, fImageProvider.getReportType());
    }

    /**
     * Test the configuration source type and parameters
     */
    @SuppressWarnings("null")
    @Test
    public void testConfigurationSourceType() {
        ITmfConfigurationSourceType sourceType = fImageProvider.getConfigurationSourceType();
        assertNotNull(sourceType);

        List<ITmfConfigParamDescriptor> descriptors = sourceType.getConfigParamDescriptors();
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());

        ITmfConfigParamDescriptor pathDescriptor = descriptors.iterator().next();
        assertEquals(PATH_PARAM, pathDescriptor.getKeyName());
        assertTrue(pathDescriptor.isRequired());
    }

    /**
     * Test the configuration source type and parameters from the factory
     */
    @SuppressWarnings("null")
    @Test
    public void testConfigurationSourceTypeFromFactory() {
        List<ITmfConfigurationSourceType> sourceTypes = fFactory.getConfigurationSourceTypes();
        assertNotNull(sourceTypes);

        ITmfConfigurationSourceType sourceType = sourceTypes.stream()
                .filter(type -> type.getId().equals(fImageProvider.getConfigurationSourceType().getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(sourceType);

        List<ITmfConfigParamDescriptor> descriptors = sourceType.getConfigParamDescriptors();
        assertNotNull(descriptors);
        assertTrue(descriptors.size() >= 2); // parentId + path

        ITmfConfigParamDescriptor pathDescriptor = descriptors.stream().filter(d -> d.getKeyName().equals(PATH_PARAM)).findFirst().orElse(null);
        assertNotNull(pathDescriptor);
        assertEquals(PATH_PARAM, pathDescriptor.getKeyName());
        assertTrue(pathDescriptor.isRequired());
    }

    /**
     * Test getting descriptor from config
     */
    @SuppressWarnings("null")
    @Test
    public void testGetDescriptorFromConfig() {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        Map<String, Object> params = new HashMap<>();
        params.put(PATH_PARAM, fTempFolder.getRoot().getAbsolutePath() + "/" + IMAGE_FILE_NAME);
        ITmfConfiguration config = new TmfConfiguration.Builder()
                .setId(IMAGE_REPORT_ID)
                .setSourceTypeId(fImageProvider.getConfigurationSourceType().getId())
                .setName(IMAGE_REPORT_NAME)
                .setDescription(IMAGE_REPORT_DESCRIPTION)
                .setParameters(params)
                .build();

        IDataProviderDescriptor descriptor = fImageProvider.getDescriptorFromConfig(trace, config);

        assertNotNull(descriptor);
        assertEquals(IMAGE_REPORT_ID, descriptor.getId());
        assertEquals(IMAGE_REPORT_NAME, descriptor.getName());
        assertEquals(IMAGE_REPORT_DESCRIPTION, descriptor.getDescription());
        assertEquals(ProviderType.NONE, descriptor.getType());
        assertTrue(descriptor.getCapabilities().canDelete());
    }

    /**
     * Test createImage method with a valid image
     *
     * @throws TmfConfigurationException
     *             If the configuration is invalid
     */
    @SuppressWarnings("null")
    @Test
    public void testCreateImageValid() throws TmfConfigurationException {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        // Now create image configuration
        String imagePath = fTempFolder.getRoot().getAbsolutePath() + "/" + IMAGE_FILE_NAME;
        Map<String, Object> imageParams = new HashMap<>();
        imageParams.put(PATH_PARAM, imagePath);
        ITmfConfiguration imageConfig = new TmfConfiguration.Builder()
                .setId(IMAGE_REPORT_ID)
                .setSourceTypeId(fImageProvider.getConfigurationSourceType().getId())
                .setName(IMAGE_REPORT_NAME)
                .setDescription(IMAGE_REPORT_DESCRIPTION)
                .setParameters(imageParams)
                .build();

        IDataProviderDescriptor descriptor = fFactory.createDataProviderDescriptors(trace, imageConfig);
        assertNotNull(descriptor);

        ITmfConfiguration config = descriptor.getConfiguration();
        assertNotNull(config);

        File newImageFile = new File(nullToEmptyString(config.getParameters().get(PATH_PARAM)));
        assertTrue("Image file should exist at new location", newImageFile.exists());
        assertTrue("New file should be a file, not a directory", newImageFile.isFile());
        assertFalse("New file should not be the same as the original file", imagePath.equals(newImageFile.getAbsolutePath()));
    }

    /**
     * Test createImage method with a non-existent image file
     */
    @SuppressWarnings("null")
    @Test
    public void testCreateImageNonExistentFile() {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        // Create configuration with non-existent file
        String imagePath = fTempFolder.getRoot().getAbsolutePath() + "/non-existent.png";
        Map<String, Object> params = new HashMap<>();
        params.put(PATH_PARAM, imagePath);
        ITmfConfiguration config = new TmfConfiguration.Builder()
                .setId(IMAGE_REPORT_ID)
                .setSourceTypeId(fImageProvider.getConfigurationSourceType().getId())
                .setName(IMAGE_REPORT_NAME)
                .setDescription(IMAGE_REPORT_DESCRIPTION)
                .setParameters(params)
                .build();

        try {
            fFactory.createDataProviderDescriptors(trace, config);
            fail("Should have thrown exception for non-existent file");
        } catch (TmfConfigurationException e) {

        }
    }

    /**
     * Test createImage method with a directory instead of a file
     */
    @SuppressWarnings("null")
    @Test
    public void testCreateImageWithDirectory() {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        // Create configuration with directory path
        String dirPath = fTempFolder.getRoot().getAbsolutePath() + "/test-directory";
        Map<String, Object> params = new HashMap<>();
        params.put(PATH_PARAM, dirPath);
        ITmfConfiguration config = new TmfConfiguration.Builder()
                .setId(IMAGE_REPORT_ID)
                .setSourceTypeId(fImageProvider.getConfigurationSourceType().getId())
                .setName(IMAGE_REPORT_NAME)
                .setDescription(IMAGE_REPORT_DESCRIPTION)
                .setParameters(params)
                .build();

        try {
            fFactory.createDataProviderDescriptors(trace, config);
            fail("Should have thrown exception for directory path");
        } catch (TmfConfigurationException e) {

        }

    }

    /**
     * Test createImage method with an invalid file extension
     */
    @SuppressWarnings("null")
    @Test
    public void testCreateImageInvalidExtension() {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        // Create configuration with invalid extension
        String txtPath = fTempFolder.getRoot().getAbsolutePath() + "/test-image.txt";
        Map<String, Object> params = new HashMap<>();
        params.put(PATH_PARAM, txtPath);
        ITmfConfiguration config = new TmfConfiguration.Builder()
                .setId(IMAGE_REPORT_ID)
                .setSourceTypeId(fImageProvider.getConfigurationSourceType().getId())
                .setName(IMAGE_REPORT_NAME)
                .setDescription(IMAGE_REPORT_DESCRIPTION)
                .setParameters(params)
                .build();

        try {
            fFactory.createDataProviderDescriptors(trace, config);
            fail("Should have thrown exception for invalid extension");
        } catch (TmfConfigurationException e) {

        }
    }

    /**
     * Test that removing the parent folder also removes all images inside it
     *
     * @throws TmfConfigurationException
     *             If the configuration is invalid
     */
    @SuppressWarnings("null")
    @Test
    public void testRemoveParentFolderWithImages() throws TmfConfigurationException {
        CtfTmfTrace trace = fTrace;
        assertNotNull(trace);

        // Create parent folder
        ITmfConfiguration folderConfig = new TmfConfiguration.Builder()
                .setId("parent-folder")
                .setSourceTypeId(fFactory.getConfigurationSourceType().getId())
                .setName("Parent Folder")
                .setDescription("Parent folder description")
                .build();

        IDataProviderDescriptor folderDescriptor = fFactory.createDataProviderDescriptors(trace, folderConfig);
        assertNotNull(folderDescriptor);

        ITmfConfiguration folderConfiguration = folderDescriptor.getConfiguration();
        assertNotNull(folderConfiguration);

        // Create first image
        Map<String, Object> imageParams = new HashMap<>();
        imageParams.put(PATH_PARAM, fTempFolder.getRoot().getAbsolutePath() + "/" + IMAGE_FILE_NAME);
        imageParams.put("parentId", folderConfiguration.getId());
        ITmfConfiguration imageConfig1 = new TmfConfiguration.Builder()
                .setId(IMAGE_REPORT_ID)
                .setSourceTypeId(fImageProvider.getConfigurationSourceType().getId())
                .setName(IMAGE_REPORT_NAME)
                .setDescription(IMAGE_REPORT_DESCRIPTION)
                .setParameters(imageParams)
                .build();

        IDataProviderDescriptor imageDescriptor = fFactory.createDataProviderDescriptors(trace, imageConfig1);
        assertNotNull(imageDescriptor);

        ITmfConfiguration imageConfiguration = imageDescriptor.getConfiguration();
        assertNotNull(imageConfiguration);

        // Verify all descriptors were created
        Collection<IDataProviderDescriptor> descriptors = fFactory.getDescriptors(trace);
        assertEquals(3, descriptors.size()); // Factory + folder + image
        assertNotNull(descriptors.stream().filter(d -> d.getId().equals(FACTORY_ID)).findFirst().orElse(null));
        assertNotNull(descriptors.stream().filter(d -> d.getId().equals(folderDescriptor.getId())).findFirst().orElse(null));
        assertNotNull(descriptors.stream().filter(d -> d.getId().equals(imageDescriptor.getId())).findFirst().orElse(null));

        File imageFile = new File(nullToEmptyString(imageConfiguration.getParameters().get(PATH_PARAM)));
        assertTrue("Image file should exist", imageFile.exists());

        // Now remove the parent folder - should recursively remove all images
        fFactory.removeDataProviderDescriptor(trace, folderDescriptor);

        // Verify descriptors removed
        descriptors = fFactory.getDescriptors(trace);
        assertEquals(1, descriptors.size()); // Only factory remains
        assertNotNull(descriptors.stream().filter(d -> d.getId().equals(FACTORY_ID)).findFirst().orElse(null));

        // Verify files removed
        assertFalse("Image file should be deleted", imageFile.exists());
    }
}