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
 * An entry of the /proc/[pid]/maps file. For more information, see the man page
 * of proc ( man 5 proc ).
 *
 * @author Matthew Khouzam
 */
public class MapEntry {

    private final long fAddrHigh;
    private final long fAddrLow;

    private final char fDeviceMajor;

    private final char fDeviceMinor;

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
     * @param deviceMinor
     *            device major
     * @param deviceMajor
     *            device minor
     * @param iNode
     *            inode location of the device
     * @param pathName
     *            file path that backs this mapping (there may be pseudo-paths
     *            in brackes [])
     */
    public MapEntry(long addrLow, long addrHigh, Perms perms, long offset, char deviceMinor, char deviceMajor, long iNode,
            String pathName) {
        fAddrLow = addrLow;
        fAddrHigh = addrHigh;
        fPerms = perms;
        fOffset = offset;
        fDeviceMajor = deviceMajor;
        fDeviceMinor = deviceMinor;
        fINode = iNode;
        fPathName = pathName;
    }

    /**
     * Get the ceiling value of the mapping address
     *
     * @return the high address
     */
    public long getAddrHigh() {
        return fAddrHigh;
    }

    /**
     * Get the floor value of the mapping address
     *
     * @return the low address
     */
    public long getAddrLow() {
        return fAddrLow;
    }

    /**
     * Get the device major address byte, to be used in conjunction with
     * {@link #getDeviceMinor()}
     *
     * @return the deviceMajor
     */
    public char getDeviceMajor() {
        return fDeviceMajor;
    }

    /**
     * Get the device minor address byte, to be used in conjunction with
     * {@link #getDeviceMajor()}
     *
     * @return the deviceMinor
     */
    public char getDeviceMinor() {
        return fDeviceMinor;
    }

    /**
     * Get the inode on the device, an inode of 0 means none is associated
     *
     * @return the iNode
     */
    public long getiNode() {
        return fINode;
    }

    /**
     * The offset in the file described in {@link #getPathName()}
     *
     * @return the offset
     */
    public long getOffset() {
        return fOffset;
    }

    /**
     * Get the path name of the file backing this mapping. If it is in brackets,
     * it is a special case, if it is blank, it is anonymous.
     *
     * @return the pathName
     */
    public String getPathName() {
        return fPathName;
    }

    /**
     * Get the permissions for the file
     *
     * @return the permissions for the file
     */
    public Perms getPerms() {
        return fPerms;
    }

}
