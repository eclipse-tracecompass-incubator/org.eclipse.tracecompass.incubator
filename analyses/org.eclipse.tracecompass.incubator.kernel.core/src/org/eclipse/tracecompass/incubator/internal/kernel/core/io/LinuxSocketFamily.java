/**********************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.io;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Class that encapsulates the values of the linux socket families. The values
 * are in the linux source code in the file "include/linux/socket.h"
 *
 * @author Geneviève Bastien
 */
public class LinuxSocketFamily {

    private static final Map<Integer, String> SOCKET_FAMILY;

    static {
        Builder<Integer, String> builder = ImmutableMap.builder();

        builder.put(0, "AF_UNSPEC"); //$NON-NLS-1$
        builder.put(1, "AF_UNIX"); //$NON-NLS-1$
        builder.put(2, "AF_INET"); //$NON-NLS-1$
        builder.put(3, "AF_AX25"); //$NON-NLS-1$
        builder.put(4, "AF_IPX"); //$NON-NLS-1$
        builder.put(5, "AF_APPLETALK"); //$NON-NLS-1$
        builder.put(6, "AF_NETROM"); //$NON-NLS-1$
        builder.put(7, "AF_BRIDGE"); //$NON-NLS-1$
        builder.put(8, "AF_ATMPVC"); //$NON-NLS-1$
        builder.put(9, "AF_X25"); //$NON-NLS-1$
        builder.put(10, "AF_INET6"); //$NON-NLS-1$
        builder.put(11, "AF_ROSE"); //$NON-NLS-1$
        builder.put(12, "AF_DECnet"); //$NON-NLS-1$
        builder.put(13, "AF_NETBEUI"); //$NON-NLS-1$
        builder.put(14, "AF_SECURITY"); //$NON-NLS-1$
        builder.put(15, "AF_KEY"); //$NON-NLS-1$
        builder.put(16, "AF_NETLINK"); //$NON-NLS-1$
        builder.put(17, "AF_PACKET"); //$NON-NLS-1$
        builder.put(18, "AF_ASH"); //$NON-NLS-1$
        builder.put(19, "AF_ECONET"); //$NON-NLS-1$
        builder.put(20, "AF_ATMSVC"); //$NON-NLS-1$
        builder.put(21, "AF_RDS"); //$NON-NLS-1$
        builder.put(22, "AF_SNA"); //$NON-NLS-1$
        builder.put(23, "AF_IRDA"); //$NON-NLS-1$
        builder.put(24, "AF_PPPOX"); //$NON-NLS-1$
        builder.put(25, "AF_WANPIPE"); //$NON-NLS-1$
        builder.put(26, "AF_LLC"); //$NON-NLS-1$
        builder.put(27, "AF_IB"); //$NON-NLS-1$
        builder.put(28, "AF_MPLS"); //$NON-NLS-1$
        builder.put(29, "AF_CAN"); //$NON-NLS-1$
        builder.put(30, "AF_TIPC"); //$NON-NLS-1$
        builder.put(31, "AF_BLUETOOTH"); //$NON-NLS-1$
        builder.put(32, "AF_IUCV"); //$NON-NLS-1$
        builder.put(33, "AF_RXRPC"); //$NON-NLS-1$
        builder.put(34, "AF_ISDN"); //$NON-NLS-1$
        builder.put(35, "AF_PHONET"); //$NON-NLS-1$
        builder.put(36, "AF_IEEE802154"); //$NON-NLS-1$
        builder.put(37, "AF_CAIF"); //$NON-NLS-1$
        builder.put(38, "AF_ALG"); //$NON-NLS-1$
        builder.put(39, "AF_NFC"); //$NON-NLS-1$
        builder.put(40, "AF_VSOCK"); //$NON-NLS-1$
        builder.put(41, "AF_KCM"); //$NON-NLS-1$
        builder.put(42, "AF_QIPCRTR"); //$NON-NLS-1$
        builder.put(43, "AF_SMC"); //$NON-NLS-1$
        builder.put(44, "AF_XDP"); //$NON-NLS-1$

        SOCKET_FAMILY = builder.build();
    }

    /**
     * Get the socket family string corresponding to an ID
     *
     * @param family
     *            The number identifying the socket family
     * @return The corresponding string
     */
    public static String getSocketFamily(int family) {
        return SOCKET_FAMILY.getOrDefault(family, "UNKNOWN FAMILY " + family); //$NON-NLS-1$
    }

}
