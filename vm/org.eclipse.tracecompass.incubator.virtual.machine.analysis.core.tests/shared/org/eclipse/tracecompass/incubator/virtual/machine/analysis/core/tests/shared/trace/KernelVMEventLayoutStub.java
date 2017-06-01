/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.trace;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.DefaultEventLayout;

/**
 * Class to extend to be able to set the event names for the VM unit tests.
 *
 * @author Geneviève Bastien
 */
public class KernelVMEventLayoutStub extends DefaultEventLayout {

    /**
     * Protected constructor
     */
    protected KernelVMEventLayoutStub() {
        super();
    }

    private static final KernelVMEventLayoutStub INSTANCE = new KernelVMEventLayoutStub();

    /**
     * Get an instance of this event layout
     *
     * This object is completely immutable, so no need to create additional
     * instances via the constructor.
     *
     * @return The instance
     */
    public static synchronized KernelVMEventLayoutStub getInstance() {
        return INSTANCE;
    }

    @Override
    public Collection<String> eventsKVMEntry() {
        return Collections.singleton("kvm_entry");
    }

    @Override
    public Collection<String> eventsKVMExit() {
        return Collections.singleton("kvm_exit");
    }

}
