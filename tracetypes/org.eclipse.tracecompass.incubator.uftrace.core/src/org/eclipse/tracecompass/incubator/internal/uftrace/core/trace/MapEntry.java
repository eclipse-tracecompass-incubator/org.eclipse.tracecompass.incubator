/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.trace;

/**
 * A map entry
 *
 * @author Matthew Khouzam
 */
public class MapEntry {

    private final long fAddrHigh;
    private final long fAddrLow;

    private final char fDeviceHigh;

    private final char fDeviceLow;

    private final long fINode;

    private final long fOffset;

    private final String fPathName;

    private final Perms fPerms;

    /**
     * A map entry
     *
     * @param addrLow
     *            low address value
     * @param addrHigh
     *            high address value
     * @param perms
     *            permissions
     * @param offset
     *            offset
     * @param deviceLow
     *            device low address
     * @param deviceHigh
     *            device high address
     * @param iNode
     *            inode location
     * @param pathName
     *            file path
     */
    public MapEntry(long addrLow, long addrHigh, Perms perms, long offset, char deviceLow, char deviceHigh, long iNode,
            String pathName) {
        fAddrLow = addrLow;
        fAddrHigh = addrHigh;
        fPerms = perms;
        fOffset = offset;
        fDeviceHigh = deviceHigh;
        fDeviceLow = deviceLow;
        fINode = iNode;
        fPathName = pathName;
    }

    /**
     * @return the addrHigh
     */
    public long getAddrHigh() {
        return fAddrHigh;
    }

    /**
     * @return the addrLow
     */
    public long getAddrLow() {
        return fAddrLow;
    }

    /**
     * @return the deviceHigh
     */
    public char getDeviceHigh() {
        return fDeviceHigh;
    }

    /**
     * @return the deviceLow
     */
    public char getDeviceLow() {
        return fDeviceLow;
    }

    /**
     * @return the iNode
     */
    public long getiNode() {
        return fINode;
    }

    /**
     * @return the offset
     */
    public long getOffset() {
        return fOffset;
    }

    /**
     * @return the pathName
     */
    public String getPathName() {
        return fPathName;
    }

    /**
     * @return the perms
     */
    public Perms getPerms() {
        return fPerms;
    }

}
