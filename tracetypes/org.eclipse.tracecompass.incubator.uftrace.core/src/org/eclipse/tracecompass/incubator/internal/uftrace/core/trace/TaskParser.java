/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.trace;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

/**
 * This files shows relation between tasks and sessions. A session keeps a
 * memory map of tasks which can be created when the (first) task was started or
 * a new program was executed (by exec(3)). When a child task was forked, it
 * inherits the session of parent (since it's memory mapping will be same unless
 * child adds or removes mappings). So a session can be shared by multiple tasks
 * and also a single task can have multiple sessions. The task.txt saves task
 * (parent-child relationship) and session info with timestamp so that it can
 * track the correct mappings.
 *
 * @author Matthew Khouzam
 *
 */
public class TaskParser {

    /*
     * Note: If this were implemented as a regex, it should be this:
     *
     * "^([a-fA-F\\d]+)\\s+([PTpt])\\s+(.+)$"
     */

    private final Map<Integer, Integer> fTidToPid = new TreeMap<>();
    private final Map<Integer, Long> fPidToSid = new TreeMap<>();
    private final Map<Integer, String> fPidToExecName = new TreeMap<>();

    /**
     * Task parser
     *
     * @param file
     *            the file to read
     * @throws IOException
     *             the file cannot be found
     */
    public TaskParser(File file) throws IOException {
        LineIterator iter = FileUtils.lineIterator(file);
        while (iter.hasNext()) {
            String line = iter.next();
            String[] tuples = line.split(" "); //$NON-NLS-1$
            if (tuples[0].equals("SESS")) { //$NON-NLS-1$
                String[] pair = tuples[2].split("="); //$NON-NLS-1$
                int pid = Integer.parseInt(pair[1]);
                pair = tuples[3].split("="); //$NON-NLS-1$
                long sid = Long.parseUnsignedLong(pair[1], 16);
                String execName = tuples[4].split("=")[1]; //$NON-NLS-1$
                fPidToSid.put(pid, sid);
                fPidToExecName.put(pid, execName);
            }
            if (tuples[0].equals("TASK")) { //$NON-NLS-1$
                String[] pair = tuples[2].split("="); //$NON-NLS-1$
                int tid = Integer.parseInt(pair[1]);
                pair = tuples[3].split("="); //$NON-NLS-1$
                int pid = Integer.parseInt(pair[1]);
                fTidToPid.put(tid, pid);
            }
        }
    }

    /**
     * Get all the tids, to navigate
     *
     * @return all the tids
     */
    public Collection<Integer> getTids() {
        return fTidToPid.keySet();
    }

    /**
     * Get the pid for a tid
     *
     * @param tid
     *            the tid
     * @return the pid
     */
    public int getPid(int tid) {
        return fTidToPid.getOrDefault(tid, -1);
    }

    /**
     * Get the exec name for a tid
     *
     * @param tid
     *            the tid
     * @return the exec name
     */
    public String getExecName(int tid) {
        return fPidToExecName.get(getPid(tid));
    }

    /**
     * Get the session name for a tid
     *
     * @param tid
     *            the tid
     * @return the session name
     */
    public Long getSessName(int tid) {
        return fPidToSid.get(getPid(tid));
    }

}
