/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.core.symbol;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.perf.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.symbols.IMappingFile;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.TmfResolvedSymbol;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Symbol provider backed by {@link PerfDataMmapAnalysisModule}. For a given
 * (pid, time, address), it looks up the best matching mmap entry and
 * resolves the address inside the backing ELF (if it can be read from disk).
 */
public class PerfDataMmapSymbolProvider implements ISymbolProvider {

    private final PerfDataMmapAnalysisModule fMmapModule;
    private final ITmfTrace fTrace;
    private final Map<String, IMappingFile> fSymbolMapping = new HashMap<>();

    /**
     * Constructor
     *
     * @param trace
     *            the trace the provider is for
     * @param module
     *            the mmap analysis module
     */
    public PerfDataMmapSymbolProvider(ITmfTrace trace, PerfDataMmapAnalysisModule module) {
        fTrace = trace;
        fMmapModule = module;
        module.schedule();
    }

    @Override
    public void loadConfiguration(@Nullable IProgressMonitor monitor) {
        // no configuration
    }

    @Override
    public @Nullable TmfResolvedSymbol getSymbol(int pid, long timestamp, long address) {
        ITmfStateSystem ss = fMmapModule.getStateSystem();
        if (ss == null) {
            return null;
        }
        int pidQuark = ss.optQuarkAbsolute(String.valueOf(pid));
        if (pidQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return null;
        }
        List<Integer> baddrQuarks = ss.getSubAttributes(pidQuark, false);
        baddrQuarks = baddrQuarks.stream()
                .filter(q -> {
                    try {
                        long b = Long.parseLong(ss.getAttributeName(q));
                        return Long.compareUnsigned(b, address) <= 0;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
        if (baddrQuarks.isEmpty()) {
            return null;
        }
        try {
            Iterable<ITmfStateInterval> intervals = ss.query2D(baddrQuarks, Collections.singleton(timestamp));
            NavigableMap<Long, ITmfStateInterval> map = new TreeMap<>(Long::compareUnsigned);
            for (ITmfStateInterval interval : intervals) {
                long b = Long.parseLong(ss.getAttributeName(interval.getAttribute()));
                map.put(b, interval);
            }
            Entry<Long, ITmfStateInterval> last = map.lastEntry();
            if (last == null) {
                return null;
            }
            Object v = last.getValue().getValue();
            if (!(v instanceof String)) {
                return null;
            }
            String filename = (String) v;
            TmfResolvedSymbol resolved = resolveInFile(pid, filename, address, last.getKey());
            return resolved == null ? new TmfResolvedSymbol(address, filename) : resolved;
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            Activator.getInstance().logWarning("Exceptions while resolving perf.data symbol", e); //$NON-NLS-1$
            return null;
        }
    }

    private @Nullable TmfResolvedSymbol resolveInFile(int pid, String filename, long address, long loadBase) {
        long addressInFile = address - loadBase;
        IMappingFile mapping = fSymbolMapping.get(filename);
        if (mapping == null) {
            Path p = Paths.get(filename);
            if (!Files.exists(p)) {
                return null;
            }
            mapping = IMappingFile.create(filename, true, pid);
            if (mapping == null) {
                return null;
            }
            fSymbolMapping.put(filename, mapping);
        }
        TmfResolvedSymbol entry = mapping.getSymbolEntry(addressInFile);
        return entry == null ? null
                : new TmfLibrarySymbol(entry.getBaseAddress() + loadBase, entry.getSymbolName(), filename);
    }

    @Override
    public @NonNull ITmfTrace getTrace() {
        return fTrace;
    }

    @Override
    public @Nullable TmfResolvedSymbol getSymbol(long address) {
        // Resolution requires a pid; caller has to use the 3-arg form.
        return null;
    }
}
