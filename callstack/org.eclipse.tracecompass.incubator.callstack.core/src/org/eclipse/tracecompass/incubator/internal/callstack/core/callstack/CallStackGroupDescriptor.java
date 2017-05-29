/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.callstack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;

/**
 * These group descriptors describe each group of a callstack hierarchy with
 * patterns in the state system.
 *
 * @author Geneviève Bastien
 */
public class CallStackGroupDescriptor implements ICallStackGroupDescriptor {

    private final String[] fSubPattern;
    private final @Nullable CallStackGroupDescriptor fNextGroup;
    private final boolean fSymbolKeyGroup;
    private final ITmfStateSystem fStateSystem;
    private final String fHostId;

    /**
     * Constructor
     *
     * @param ss
     *            The state system containing the callstack
     * @param subPath
     *            The sub-path to this group
     * @param nextGroup
     *            The next group of the hierarchy, ie the child of the group
     *            being constructed or <code>null</code> if this group is the
     *            leaf
     * @param isSymbolKeyGroup
     *            Whether this level contains the symbol key
     * @param hostId
     *            The ID of the host these callstack groups are from
     */
    public CallStackGroupDescriptor(ITmfStateSystem ss, String[] subPath, @Nullable CallStackGroupDescriptor nextGroup, boolean isSymbolKeyGroup, String hostId) {
        fSubPattern = subPath;
        fNextGroup = nextGroup;
        fSymbolKeyGroup = isSymbolKeyGroup;
        fStateSystem = ss;
        fHostId = hostId;
    }

    @Override
    public @Nullable CallStackGroupDescriptor getNextGroup() {
        return fNextGroup;
    }

    /**
     * Get the pattern in the state system corresponding to this level. From the
     * quark of the parent, the elements corresponding to this sub-pattern will
     * be the elements of this group.
     *
     * @return The pattern of the elements at this level
     */
    public String[] getSubPattern() {
        return fSubPattern;
    }

    @Override
    public boolean isSymbolKeyGroup() {
        return fSymbolKeyGroup;
    }

    @Override
    public List<@NonNull ICallStackElement> getElements(@Nullable ICallStackElement parent, int baseQuark, @Nullable ICallStackElement symbolKeyElement, @Nullable IThreadIdResolver threadIdProvider) {
        // Get the elements from the base quark at the given pattern
        List<Integer> quarks = fStateSystem.getQuarks(baseQuark, getSubPattern());
        if (quarks.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        if (parent != null && !(parent instanceof CallStackElement)) {
            return Collections.EMPTY_LIST;
        }

        CallStackGroupDescriptor nextGroup = fNextGroup;
        // If the next level is null, then this is a callstack final element
        List<ICallStackElement> elements = new ArrayList<>(quarks.size());
        for (Integer quark : quarks) {
            CallStackElement element = nextGroup == null ? new CallStackLeafElement(fHostId, fStateSystem, quark, symbolKeyElement, threadIdProvider, (CallStackElement) parent)
                    : new CallStackElement(fHostId, fStateSystem, quark, getNextGroup(), symbolKeyElement, threadIdProvider, (CallStackElement) parent);
            if (isSymbolKeyGroup()) {
                element.setSymbolKeyElement(element);
            }
            elements.add(element);
        }
        return elements;
    }

    @Override
    public @NonNull String getName() {
        return String.valueOf(StringUtils.join(fSubPattern, '/'));
    }

}
