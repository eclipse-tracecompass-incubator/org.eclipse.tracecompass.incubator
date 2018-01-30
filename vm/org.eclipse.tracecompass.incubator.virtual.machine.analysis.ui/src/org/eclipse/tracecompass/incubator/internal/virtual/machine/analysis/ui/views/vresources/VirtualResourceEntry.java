/*******************************************************************************
 * Copyright (c) 2016-2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vresources;

import java.util.Iterator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.resourcesstatus.SoftIrqLabelProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * An entry for the Virtual Resources View
 *
 * TODO: Instead of using the Type enum and doing ifs on it in the view, maybe
 * use different sub-classes and methods to retrieve the events
 *
 * @author Cedric Biancheri
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class VirtualResourceEntry extends TimeGraphEntry implements Comparable<ITimeGraphEntry> {

    /** Type of resource */
    public static enum Type {
        /** Null resources (filler rows, etc.) */
        NULL,
        /** Entries for CPUs */
        CPU,
        /** Entries for PCPUs of VMs */
        PCPU_VM,
        /** Entries for PCPUs of Containers */
        PCPU_CONTAINER,
        /** Entries for IRQs */
        IRQ,
        /** Entries for Soft IRQ */
        SOFT_IRQ,
        /** Entries for VM */
        VM,
        /** Entries for containers */
        CONTAINER;

        @Override
        public String toString() {
            /* Every CPU displayed is a physical CPU. */
            if (this == Type.CPU || this == Type.PCPU_VM || this == Type.PCPU_CONTAINER) {
                return "PCPU";
            }
            return super.toString();
        }
    }

    private final int fId;
    private final @NonNull ITmfTrace fTrace;
    private final Type fType;
    private final int fQuark;

    /**
     * Constructor
     *
     * @param quark
     *            The attribute quark matching the entry
     * @param trace
     *            The trace on which we are working
     * @param name
     *            The exec_name of this entry
     * @param startTime
     *            The start time of this entry lifetime
     * @param endTime
     *            The end time of this entry
     * @param type
     *            The type of this entry
     * @param id
     *            The id of this entry
     */
    public VirtualResourceEntry(int quark, @NonNull ITmfTrace trace, String name,
            long startTime, long endTime, Type type, int id) {
        super(name, startTime, endTime);
        fId = id;
        fTrace = trace;
        fType = type;
        fQuark = quark;
    }

    /**
     * Constructor
     *
     * @param trace
     *            The trace on which we are working
     * @param name
     *            The exec_name of this entry
     * @param startTime
     *            The start time of this entry lifetime
     * @param endTime
     *            The end time of this entry
     * @param type
     *            The type of this entry
     * @param id
     *            The id of this entry
     */
    public VirtualResourceEntry(@NonNull ITmfTrace trace, String name,
            long startTime, long endTime, Type type, int id) {
        this(-1, trace, name, startTime, endTime, type, id);
    }

    /**
     * Constructor
     *
     * @param quark
     *            The attribute quark matching the entry
     * @param trace
     *            The trace on which we are working
     * @param startTime
     *            The start time of this entry lifetime
     * @param endTime
     *            The end time of this entry
     * @param type
     *            The type of this entry
     * @param id
     *            The id of this entry
     */
    public VirtualResourceEntry(int quark, @NonNull ITmfTrace trace,
            long startTime, long endTime, Type type, int id) {
        this(quark, trace, computeEntryName(type, id), startTime, endTime, type, id);
    }

    private static String computeEntryName(Type type, int id) {
        if (Type.SOFT_IRQ.equals(type)) {
            return type.toString() + ' ' + id + ' ' + SoftIrqLabelProvider.getSoftIrq(id);
        }
        return type.toString() + ' ' + id;
    }

    /**
     * Get the entry's id
     *
     * @return the entry's id
     */
    public int getId() {
        return fId;
    }

    /**
     * Get the entry's trace
     *
     * @return the entry's trace
     */
    public @NonNull ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Get the entry Type of this entry. Uses the inner Type enum.
     *
     * @return The entry type
     */
    public Type getType() {
        return fType;
    }

    /**
     * Retrieve the attribute quark that's represented by this entry.
     *
     * @return The integer quark The attribute quark matching the entry
     */
    public int getQuark() {
        return fQuark;
    }

    @Override
    public boolean hasTimeEvents() {
        if (fType == Type.NULL || fType == Type.VM || fType == Type.CONTAINER) {
            return false;
        }
        return true;
    }

    /**
     * Get the default expanded state of this entry
     *
     * @return The default expanded state of the entry
     */
    public boolean getDefaultExpandedState() {
        return fType != Type.CPU;
    }

    @Override
    public int compareTo(ITimeGraphEntry other) {
        if (!(other instanceof VirtualResourceEntry)) {
            /*
             * Should not happen, but if it does, put those entries at the end
             */
            return -1;
        }
        VirtualResourceEntry o = (VirtualResourceEntry) other;

        /*
         * Resources entry names should all be of type "ABC 123"
         *
         * We want to filter on the Type first (the "ABC" part), then on the ID
         * ("123") in numerical order (so we get 1,2,10 and not 1,10,2).
         */
        int ret = this.getType().compareTo(o.getType());
        if (ret != 0) {
            return ret;
        }
        return Integer.compare(this.getId(), o.getId());
    }

    @Override
    public Iterator<@NonNull ITimeEvent> getTimeEventsIterator() {
        return super.getTimeEventsIterator();
    }

}
