/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a connection endpoint with an address and a port. IPv4-specific
 * for now.
 *
 * @author Christophe Bedard
 */
public class ConnectionEndpoint {

    private static final @NonNull String ADDRESS_SEP = "."; //$NON-NLS-1$
    private static final @NonNull String PORT_SEP = ":"; //$NON-NLS-1$

    private final long[] fAddress;
    private final long fPort;

    /**
     * Constructor
     *
     * @param address
     *            the address
     * @param port
     *            the port
     */
    public ConnectionEndpoint(long[] address, long port) {
        fAddress = address;
        fPort = port;
    }

    /**
     * @return the address
     */
    public long[] getAddress() {
        return fAddress;
    }

    /**
     * @return the port
     */
    public long getPort() {
        return fPort;
    }

    /**
     * Format address and port
     *
     * @param address
     *            the address
     * @param port
     *            the port
     * @return the address and port formated like 1.2.3.4:1234
     */
    public static String formatAddressPort(long[] address, long port) {
        return StringUtils.join(ArrayUtils.toObject(address), ADDRESS_SEP) + PORT_SEP + String.valueOf(port);
    }

    @Override
    public String toString() {
        return formatAddressPort(fAddress, fPort);
    }

    /**
     * Create a {@link ConnectionEndpoint} from a string like "1.2.3.4:1234"
     *
     * @param f
     *            the string
     * @return the {@link ConnectionEndpoint}, or {@code null} if invalid
     */
    public static @Nullable ConnectionEndpoint fromStringFormat(String f) {
        @NonNull String[] split = f.split(PORT_SEP);
        if (split.length != 2) {
            return null;
        }

        @NonNull String[] addressStr = split[0].split(Objects.requireNonNull(Pattern.quote(ADDRESS_SEP)));
        long[] address = new long[addressStr.length];
        for (int i = 0; i < addressStr.length; ++i) {
            address[i] = Long.parseLong(addressStr[i]);
        }
        long port = Long.parseLong(split[1]);

        return new ConnectionEndpoint(address, port);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hash(fPort);
        result = prime * result + Arrays.hashCode(fAddress);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConnectionEndpoint)) {
            return false;
        }
        ConnectionEndpoint other = (ConnectionEndpoint) obj;
        return Arrays.equals(fAddress, other.fAddress) && fPort == other.fPort;
    }
}
