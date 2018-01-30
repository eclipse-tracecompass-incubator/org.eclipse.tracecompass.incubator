/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model;

import java.util.HashSet;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;

/**
 * This class represents a machine, host or guest, in a virtual machine model. A
 * machine is identified by a trace's host ID.
 *
 * @TODO Review this class's API with the new virtual machine environment
 *
 * @author Geneviève Bastien
 */
public final class VirtualMachine {

    private static final int UNKNOWN = 0;
    private static final int HOST = (1 << 0);
    private static final int GUEST = (1 << 1);
    private static final int CONTAINER = (1 << 2);

    private long fVmUid;
    private final String fHostId;
    private int fType;
    private final String fTraceName;
    private @Nullable VirtualMachine fParent;
    private HashSet<VirtualMachine> fChildren = new HashSet<>();
    private HashSet<HostThread> fThreadsWaitingForNextLayer = new HashSet<>();
    private HashSet<HostThread> fThreadsReadyForNextLayer = new HashSet<>();

    /**
     * Create an unknown machine for a host
     *
     * @param hostId
     *            The ID of the host
     * @param traceName
     *            The name of the trace
     * @return A virtual machine
     */
    public static VirtualMachine newUnknownMachine(String hostId, String traceName) {
        return new VirtualMachine(UNKNOWN, hostId, -1, traceName);
    }

    /**
     * Create a new host machine. A host is a physical machine that may contain
     * virtual guest machines.
     *
     * @param hostId
     *            The host ID of the trace(s) this machine represents
     * @param traceName
     *            The name of the trace
     * @return A {@link VirtualMachine} of type host
     */
    public static VirtualMachine newHostMachine(String hostId, String traceName) {
        return new VirtualMachine(HOST, hostId, -1, traceName);
    }

    /**
     * Create a new guest machine. A guest is a virtual machine with virtual
     * CPUs running on a host.
     *
     * @param uid
     *            Some unique identifier of this guest machine that can be used
     *            in both the guest and the host to match both machines.
     * @param hostId
     *            The host ID of the trace(s) this machine represents
     * @param traceName
     *            The name of the trace
     * @return A {@link VirtualMachine} of type guest.
     */
    public static VirtualMachine newGuestMachine(long uid, String hostId, String traceName) {
        return new VirtualMachine(GUEST, hostId, uid, traceName);
    }

    /**
     * Create a new container machine. A container is a new namespace contained
     * in the host, a virtual machine or an other namespace
     *
     * @param uid
     *            Unique identifier of the container. It is the namespace
     *            identifier
     * @param hostId
     *            ID of the machine containing the container.
     * @param traceName
     *            The name of the trace
     * @return A {@link VirtualMachine} of type container.
     */
    public static VirtualMachine newContainerMachine(long uid, String hostId, String traceName) {
        return new VirtualMachine(CONTAINER, hostId, uid, traceName);
    }

    private VirtualMachine(int type, String hostId, long uid, String traceName) {
        fType = type;
        fVmUid = uid;
        fHostId = hostId;
        fTraceName = traceName;
        fParent = null;
    }

    /**
     * Return true if this machine is a guest
     *
     * @return {@code true} if the machine is a guest, or {@code false} if not
     */
    public boolean isGuest() {
        return (fType & GUEST) == GUEST;
    }

    /**
     * Add the guest type to the machine
     *
     * @param uid
     *            The ID of the virtual machine
     */
    public void setGuest(long uid) {
        fType |= GUEST;
        fVmUid = uid;
    }

    /**
     * Return true if this machine is a host
     *
     * @return {@code true} if the machine is a host, or {@code false} if not
     */
    public boolean isHost() {
        return (fType & HOST) == HOST;
    }

    /**
     * Add the host type to the machine
     */
    public void setHost() {
        fType |= HOST;
    }

    /**
     * Return true if this machine is a container
     *
     * @return {@code true} if the machine is a container, or {@code false} if
     *         not
     */
    public boolean isContainer() {
        return (fType & CONTAINER) == CONTAINER;
    }

    /**
     * Add the container type to the machine
     */
    public void setContainer() {
        fType |= CONTAINER;
    }

    /**
     * Get the unique identifier that is used between the host and the guest to
     * identify this machine.
     *
     * @return The Virtual Machine unique ID.
     */
    public long getVmUid() {
        return fVmUid;
    }

    /**
     * Get the host ID of this machine
     *
     * @return The host ID of this machine
     */
    public String getHostId() {
        return fHostId;
    }

    /**
     * Get the trace's name.
     *
     * @return The trace's name.
     */
    public String getTraceName() {
        return fTraceName;
    }

    @Override
    public String toString() {
        return "VirtualMachine: " + fHostId; //$NON-NLS-1$
    }

    /**
     * Get the children of the machine
     *
     * @return the children
     */
    public HashSet<VirtualMachine> getChildren() {
        return fChildren;
    }

    /**
     * Add a child to the machine
     *
     * @param child
     *            the child to add
     */
    public void addChild(VirtualMachine child) {
        fChildren.add(child);
    }

    /**
     * Get the type of the machine
     *
     * @return the type of the machine
     */
    public int getType() {
        return fType;
    }

    /**
     * Get the parent
     *
     * @return the parent
     */
    public @Nullable VirtualMachine getParent() {
        return fParent;
    }

    /**
     * Set the parent
     *
     * @param parent
     *            the parent
     */
    public void setParent(VirtualMachine parent) {
        fParent = parent;
    }

    /**
     * Add a thread in the waiting for next level threads set. Meaning that we
     * are waiting for a specific event before going to an upper layer.
     *
     * @param hostThread
     *            the thread
     */
    public void addThreadWaitingForNextLayer(HostThread hostThread) {
        fThreadsWaitingForNextLayer.add(hostThread);
        fThreadsReadyForNextLayer.remove(hostThread);
    }

    /**
     * Returns false if the thread is not in a waiting for next level state.
     * Return true otherwise.
     *
     * @param hostThread
     *            the thread
     * @return true if the thread was ready
     */
    public boolean isThreadWaitingForNextLayer(HostThread hostThread) {
        return fThreadsWaitingForNextLayer.contains(hostThread);
    }

    /**
     * Put a thread in the ready state if it was in waiting state.
     *
     * @param hostThread
     *            the thread
     */
    public void makeThreadReadyForNextLayer(HostThread hostThread) {
        if (fThreadsWaitingForNextLayer.remove(hostThread)) {
            fThreadsReadyForNextLayer.add(hostThread);
        }
    }

    /**
     * Return true if the thread is ready to go to the next layer.
     *
     * @param hostThread
     *            the thread
     * @return
     */
    public boolean isThreadReadyForNextLayer(HostThread hostThread) {
        return fThreadsReadyForNextLayer.contains(hostThread);
    }

    /**
     * Remove the thread from the ready for next level set.
     *
     * @param hostThread
     *            the thread
     */
    public void removeThreadFromReadyForNextLayerSet(HostThread hostThread) {
        fThreadsReadyForNextLayer.remove(hostThread);
    }

}