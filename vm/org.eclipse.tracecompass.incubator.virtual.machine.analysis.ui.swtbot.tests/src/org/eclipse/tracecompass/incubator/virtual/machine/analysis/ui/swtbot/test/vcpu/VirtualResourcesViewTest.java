/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.virtual.machine.analysis.ui.swtbot.test.vcpu;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Objects;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vresources.Messages;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vresources.VirtualResourcesView;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestExperiment;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTraces;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotTimeGraph;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.tracecompass.tmf.ui.tests.shared.WaitUtils;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Test the {@link VirtualResourcesView} class
 *
 * @author Geneviève Bastien
 */
public class VirtualResourcesViewTest {

    private static final String TRACE_TYPE = "org.eclipse.tracecompass.incubator.virtual.machine.analysis.vm.trace.stub";
    private static final String PROJECT_NAME = "test";
    private static final String EXPERIMENT_NAME = "exp";
    private static final String VIEW_ID = VirtualResourcesView.ID;

    /** The Log4j logger instance. */
    private static final Logger fLogger = Objects.requireNonNull(Logger.getRootLogger());

    /**
     * Things to setup
     */
    @BeforeClass
    public static void beforeClass() {

        SWTBotUtils.initialize();
        Thread.currentThread().setName("SWTBotTest");
        /* set up for swtbot */
        SWTBotPreferences.TIMEOUT = 5000; /* 5 seconds timeout */
        SWTBotPreferences.KEYBOARD_LAYOUT = "EN_US";
        fLogger.removeAllAppenders();
        fLogger.addAppender(new ConsoleAppender(new SimpleLayout(), ConsoleAppender.SYSTEM_OUT));
        SWTWorkbenchBot bot = new SWTWorkbenchBot();
        SWTBotUtils.closeView("welcome", bot);
        /* Switch perspectives */
        SWTBotUtils.switchToTracingPerspective();
        /* Finish waiting for eclipse to load */
        WaitUtils.waitForJobs();

    }

    /**
     * Opens and returns a the Virtual Resources view
     *
     * @return The newly opened view
     *
     * @throws SecurityException
     *             If a security manager is present and any the wrong class is
     *             loaded or the class loader is not the same as its ancestor's
     *             loader.
     *
     * @throws IllegalArgumentException
     *             the object is not the correct class type
     */
    public SWTBotView setupView() throws SecurityException, IllegalArgumentException {
        SWTBotUtils.openView(VIEW_ID);
        SWTWorkbenchBot bot = new SWTWorkbenchBot();
        SWTBotView viewBot = bot.viewById(VIEW_ID);
        assertNotNull(viewBot);
        final IViewReference viewReference = viewBot.getViewReference();
        @SuppressWarnings("null")
        IViewPart viewPart = UIThreadRunnable.syncExec(new Result<IViewPart>() {
            @Override
            public IViewPart run() {
                return viewReference.getView(true);
            }
        });
        assertNotNull(viewPart);
        if (!(viewPart instanceof VirtualResourcesView)) {
            fail("Could not instanciate view");
        }
        return viewBot;
    }

    /**
     * Closes the view
     */
    @After
    public void closeView() {
        final SWTWorkbenchBot swtWorkbenchBot = new SWTWorkbenchBot();
        SWTBotView viewBot = swtWorkbenchBot.viewById(VIEW_ID);
        viewBot.close();
        SWTBotUtils.deleteProject(PROJECT_NAME, swtWorkbenchBot);
    }

    /**
     * Test with a trace of a host with one QEMU guest
     */
    @Ignore
    @Test
    public void testOneQemuKvm() {
        String[] physCPU0 = { VmTraces.HOST_ONE_QEMUKVM.getHostId(), Messages.FusedVMView_PhysicalCpusEntry, "PCPU 0" };
        String[] virtCPU0 = { VmTraces.HOST_ONE_QEMUKVM.getHostId(), Messages.FusedVMView_VirtualMachinesEntry, VmTraces.GUEST_ONE_QEMUKVM.getHostId(), Messages.FusedVMView_PhysicalCpusEntry, "PCPU 0" };
        testExperiment(VmTestExperiment.ONE_QEMUKVM, ImmutableSet.of(physCPU0, virtCPU0));
    }

    /**
     * Test with a trace with containers
     */
    @Ignore
    @Test
    public void testOneContainer() {
        String[] physCPU0 = { VmTraces.ONE_CONTAINER.getHostId(), Messages.FusedVMView_PhysicalCpusEntry, "PCPU 0" };
        String[] physCPU1 = { VmTraces.ONE_CONTAINER.getHostId(), Messages.FusedVMView_PhysicalCpusEntry, "PCPU 1" };

        String[] contCPU00 = { VmTraces.ONE_CONTAINER.getHostId(), Messages.FusedVMView_ContainersEntry, "654321", Messages.FusedVMView_PhysicalCpusEntry, "PCPU 0" };
        String[] contCPU01 = { VmTraces.ONE_CONTAINER.getHostId(), Messages.FusedVMView_ContainersEntry, "654321", Messages.FusedVMView_PhysicalCpusEntry, "PCPU 1" };
        String[] contCPU10 = { VmTraces.ONE_CONTAINER.getHostId(), Messages.FusedVMView_ContainersEntry, "654321", Messages.FusedVMView_ContainersEntry, "987654", Messages.FusedVMView_PhysicalCpusEntry, "PCPU 0" };
        String[] contCPU11 = { VmTraces.ONE_CONTAINER.getHostId(), Messages.FusedVMView_ContainersEntry, "654321", Messages.FusedVMView_ContainersEntry, "987654", Messages.FusedVMView_PhysicalCpusEntry, "PCPU 1" };

        testExperiment(VmTestExperiment.ONE_CONTAINER, ImmutableSet.of(physCPU0, physCPU1, contCPU00, contCPU01, contCPU10, contCPU11));
    }

    /**
     * Test with a trace of a host with one QEMU guest with containers
     */
    @Ignore
    @Test
    public void testQemuContainer() {
        // The getHostId method of the traces cannot be overridden in those
        // tests, as the SWTBOT utils uses trace compass's default behavior, so
        // the host ID will be the ones from the ONE_QEMUKVM experiment
        String[] physCPU0 = { VmTraces.HOST_ONE_QEMUKVM.getHostId(), Messages.FusedVMView_PhysicalCpusEntry, "PCPU 0" };
        String[] virtCPU0 = { VmTraces.HOST_ONE_QEMUKVM.getHostId(), Messages.FusedVMView_VirtualMachinesEntry, VmTraces.GUEST_ONE_QEMUKVM.getHostId(), Messages.FusedVMView_PhysicalCpusEntry, "PCPU 0" };
        String[] contCPU0 = { VmTraces.HOST_ONE_QEMUKVM.getHostId(), Messages.FusedVMView_VirtualMachinesEntry, VmTraces.GUEST_ONE_QEMUKVM.getHostId(), Messages.FusedVMView_ContainersEntry, "987654", Messages.FusedVMView_PhysicalCpusEntry, "PCPU 0" };

        testExperiment(VmTestExperiment.QEMU_CONTAINER, ImmutableSet.of(physCPU0, virtCPU0, contCPU0));
    }

    private static void openExperiment(VmTestExperiment experiment) {
     // Create the project with the 2 stub traces
        SWTWorkbenchBot bot = new SWTWorkbenchBot();
        SWTBotUtils.createProject(PROJECT_NAME);
        String[] array = new String[experiment.getTraces().size()];
        int i = 0;
        for (VmTraces trace : experiment.getTraces()) {
            SWTBotUtils.openTrace(PROJECT_NAME, trace.getPath().toOSString(), TRACE_TYPE);
            array[i] = trace.getFileName();
            i++;
        }
        SWTBotUtils.createExperiment(bot, PROJECT_NAME, EXPERIMENT_NAME);
        WaitUtils.waitForJobs();

        final SWTBotView projectViewBot = bot.viewById(IPageLayout.ID_PROJECT_EXPLORER);
        projectViewBot.setFocus();

        SWTBotTree treeBot = projectViewBot.bot().tree();
        SWTBotTreeItem node = treeBot.getTreeItem(PROJECT_NAME);
        node.expand();
        node = node.getNode("Traces [" + array.length + "]");
        node.expand();
        node.select(array);
        treeBot.contextMenu().menu("Open As Experiment...", "Virtual Machine Experiment (incubator)").click();
    }

    private void testExperiment(VmTestExperiment experiment, Collection<String[]> entries) {
        openExperiment(experiment);

        // Open the view and wait for completion
        @NonNull SWTBotView viewBot = setupView();
        WaitUtils.waitForJobs();

        // Test the entries
        assertNotNull(viewBot);
        SWTBotTimeGraph timeGraphBot = new SWTBotTimeGraph(viewBot.bot());
        for (String[] entryPath : entries) {
            timeGraphBot.getEntry(entryPath);
        }

    }

}
