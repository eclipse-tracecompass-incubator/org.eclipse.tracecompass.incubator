/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.profiling.core.symbol;

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
import org.eclipse.tracecompass.incubator.internal.perf.profiling.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.symbols.IMappingFile;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.TmfResolvedSymbol;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * @author Geneviève Bastien
 */
public class PerfMmapSymbolProvider implements ISymbolProvider {

    private final PerfMmapAnalysisModule fMmapModule;
    private final ITmfTrace fTrace;
    private final Map<String, IMappingFile> fSymbolMapping = new HashMap<>();

    /**
     * Constructor
     *
     * @param trace
     *            The trace this provider is for
     * @param module
     *            The perf mmap analysis module to use for this symbol provider
     */
    public PerfMmapSymbolProvider(ITmfTrace trace, PerfMmapAnalysisModule module) {
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
        PerfMmapAnalysisModule mmapModule = fMmapModule;
        ITmfStateSystem stateSystem = mmapModule.getStateSystem();
        if (stateSystem == null) {
            return null;
        }
        // Get the quark for the process
        int pidQuark = stateSystem.optQuarkAbsolute(String.valueOf(pid));
        if (pidQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return null;
        }
        // Get the potential base addresses for the requested address
        List<Integer> baddrQuarks = stateSystem.getSubAttributes(pidQuark, false);
        baddrQuarks = baddrQuarks.stream()
                .filter(quark -> {
                    String baddrStr = stateSystem.getAttributeName(quark.intValue());
                    long baddr = Long.parseLong(baddrStr);
                    return baddr <= address;
                })
                .collect(Collectors.toList());

        Iterable<ITmfStateInterval> intervals;
        try {
            intervals = stateSystem.query2D(baddrQuarks, Collections.singleton(timestamp));
            NavigableMap<Long, ITmfStateInterval> map = new TreeMap<>();
            for (ITmfStateInterval interval : intervals) {
                String baddrStr = stateSystem.getAttributeName(interval.getAttribute());
                long baddr = Long.parseLong(baddrStr);
                map.put(baddr, interval);
            }
            Entry<Long, ITmfStateInterval> lastEntry = map.lastEntry();
            if (lastEntry == null) {
                return null;
            }
            String filename = String.valueOf(lastEntry.getValue().getValue());
            TmfResolvedSymbol symbol = getSymbolInFile(pid, filename, address, lastEntry.getKey());
            return symbol == null ? new TmfResolvedSymbol(lastEntry.getKey(), filename) : symbol;
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            Activator.getInstance().logWarning("Exceptions while getting perf mmap symbol", e); //$NON-NLS-1$
            return getSymbol(address);
        }

    }

    private @Nullable TmfResolvedSymbol getSymbolInFile(int pid, String filename, long address, long offset) {
        long addressInFile = address - offset;
        IMappingFile mappingFile = fSymbolMapping.get(filename);
        if (mappingFile == null) {
            // Load the file and map its symbols
            Path path = Paths.get(filename);
            if (!Files.exists(path)) {
                return null;
            }
            mappingFile = IMappingFile.create(filename, true, pid);
            if (mappingFile == null) {
                return null;
            }
            fSymbolMapping.put(filename, mappingFile);
        }
        TmfResolvedSymbol symbolEntry = mappingFile.getSymbolEntry(addressInFile);
        // Return a new symbol entry with address in the process instead of file
        return symbolEntry == null ? null : new TmfLibrarySymbol(symbolEntry.getBaseAddress() + offset, symbolEntry.getSymbolName(), filename);
    }

    @Override
    public @NonNull ITmfTrace getTrace() {
        return fTrace;
    }

    @Override
    public @Nullable TmfResolvedSymbol getSymbol(long address) {
        // This symbol provider requires the process ID
        return null;
    }

    @Deprecated
    @Override
    public @Nullable String getSymbolText(long address) {
        // Need to implement this unfortunately
        return null;
    }

}
