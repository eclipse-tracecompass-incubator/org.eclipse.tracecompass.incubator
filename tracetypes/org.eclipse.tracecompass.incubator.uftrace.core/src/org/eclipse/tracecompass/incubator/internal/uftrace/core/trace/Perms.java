/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.trace;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap.Builder;

/**
 * Permissions helper
 *
 * @author Matthew Khouzam
 *
 */
public class Perms {

    private static final Map<String, Perms> fPerms;
    static {
        Builder<String, Perms> builder = new Builder<>();
        builder.put("---p", new Perms(false, false, false, false)); //$NON-NLS-1$
        builder.put("---s", new Perms(false, false, false, true)); //$NON-NLS-1$
        builder.put("--xp", new Perms(false, false, true, false)); //$NON-NLS-1$
        builder.put("--xs", new Perms(false, false, true, true)); //$NON-NLS-1$
        builder.put("-w-p", new Perms(false, true, false, false)); //$NON-NLS-1$
        builder.put("-w-s", new Perms(false, true, false, true)); //$NON-NLS-1$
        builder.put("-wxp", new Perms(false, true, true, false)); //$NON-NLS-1$
        builder.put("-wxs", new Perms(false, true, true, true)); //$NON-NLS-1$
        builder.put("r--p", new Perms(true, false, false, false)); //$NON-NLS-1$
        builder.put("r--s", new Perms(true, false, false, true)); //$NON-NLS-1$
        builder.put("r-xp", new Perms(true, false, true, false)); //$NON-NLS-1$
        builder.put("r-xs", new Perms(true, false, true, true)); //$NON-NLS-1$
        builder.put("rw-p", new Perms(true, true, false, false)); //$NON-NLS-1$
        builder.put("rw-s", new Perms(true, true, false, true)); //$NON-NLS-1$
        builder.put("rwxp", new Perms(true, true, true, false)); //$NON-NLS-1$
        builder.put("rwxs", new Perms(true, true, true, true)); //$NON-NLS-1$
        fPerms = builder.build();
    }

    private final boolean fRead;
    private final boolean fWrite;
    private final boolean fExecute;
    private final boolean fShared;

    /**
     * Get the permissions
     *
     * @param src
     *            source
     * @return the permissions or null if the string is invalid
     */
    public static @Nullable Perms create(String src) {
        return fPerms.get(src.toLowerCase());
    }

    private Perms(boolean read, boolean write, boolean execute, boolean shared) {
        fRead = read;
        fWrite = write;
        fExecute = execute;
        fShared = shared;
    }

    @Override
    public String toString() {
        String read = fRead ? "r" : "-"; //$NON-NLS-1$ //$NON-NLS-2$
        String write = fWrite ? "w" : "-"; //$NON-NLS-1$ //$NON-NLS-2$
        String exec = fExecute ? "x" : "-"; //$NON-NLS-1$ //$NON-NLS-2$
        String shared = fShared ? "s" : "p"; //$NON-NLS-1$ //$NON-NLS-2$
        return read + write + exec + shared;
    }

}
