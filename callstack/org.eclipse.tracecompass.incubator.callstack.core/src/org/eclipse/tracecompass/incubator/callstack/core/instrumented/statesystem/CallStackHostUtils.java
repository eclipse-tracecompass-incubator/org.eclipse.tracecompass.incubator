/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem;

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Contains interfaces and classes to resolve the host ID in instrumented
 * callstack analyses.
 *
 * @author Geneviève Bastien
 */
public final class CallStackHostUtils {

    private CallStackHostUtils() {
        // Nothing to do
    }

    /**
     * Interface for classes that provide a host ID at time t for a callstack. The
     * host ID is used to identify the machine on which the callstack was taken and
     * can be correlated with the model of a system to obtain additional data.
     */
    public interface IHostIdProvider extends Function<Long, String> {

    }

    /**
     * The host ID is always the same, so return that string
     */
    private static class TraceHostIdProvider implements IHostIdProvider {

        private final String fHostId;

        public TraceHostIdProvider(String hostId) {
            fHostId = hostId;
        }

        @Override
        public @NonNull String apply(Long time) {
            return fHostId;
        }

    }

    /**
     * Interface for describing how a callstack will get the host ID, it will return
     * the host ID provider for a callstack element
     */
    public interface IHostIdResolver extends Function<ICallStackElement, IHostIdProvider> {

    }

    /**
     * A host ID resolver that gets the host ID from the trace
     */
    public static final class TraceHostIdResolver implements IHostIdResolver {

        private final String fHostId;

        /**
         * @param trace
         *            The trace to use to provide the host ID
         */
        public TraceHostIdResolver(@Nullable ITmfTrace trace) {
            fHostId = trace == null ? StringUtils.EMPTY : trace.getHostId();
        }

        @Override
        public @NonNull IHostIdProvider apply(ICallStackElement element) {
            return new TraceHostIdProvider(fHostId);
        }

    }

}
