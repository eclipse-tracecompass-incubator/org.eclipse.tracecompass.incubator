/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial implementation and API
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.event.matching;

import java.util.Collection;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.qemukvm.QemuKvmStrings;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.matching.IEventMatchingKey;
import org.eclipse.tracecompass.tmf.core.event.matching.ITmfMatchEventDefinition;
import org.eclipse.tracecompass.tmf.core.event.matching.TmfEventMatching.Direction;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceWithPreDefinedEvents;
import org.eclipse.tracecompass.tmf.core.trace.TmfEventTypeCollectionHelper;

import com.google.common.collect.ImmutableSet;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Class to match virtual machine synchronization events. These events are
 * obtained using especially instrumented lttng-modules with the QEMU/KVM
 * hypervisor. The instrumentation can be obtained with the 'addons' branch of
 * lttng-modules, found at
 * https://github.com/giraldeau/lttng-modules/tree/addons
 *
 * To obtain traces with the right event, you should compile lttng-modules with
 * the 'addons' branch and manually insert kernel modules lttng-vmsync-guest on
 * the virtual machines and lttng-vmsync-host on the host machine.
 *
 * @author Geneviève Bastien
 */
public class TwoWayVmEventMatching implements ITmfMatchEventDefinition {

    private static final String COUNTER_PAYLOAD = "cnt"; //$NON-NLS-1$
    private static final String VM_UID_PAYLOAD = "vm_uid"; //$NON-NLS-1$

    /*
     * TODO: Maybe not define the QemuPacketKey here but in
     * org.eclipse.tracecompass.tmf.core.event.matching
     */
    private static class QemuPacketKey implements IEventMatchingKey {
        private static HashFunction hf = Hashing.goodFastHash(32);
        private long vmUid;
        private long seq;

        /**
         * Constructor with parameters
         *
         * @param uid
         *            The uid of the virtual machine
         * @param s
         *            The packet sequence number
         */
        public QemuPacketKey(long uid, long s) {
            vmUid = uid;
            seq = s;
        }

        @Override
        public int hashCode() {
            return hf.newHasher(32).putLong(vmUid).putLong(seq).hash().asInt();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o instanceof QemuPacketKey) {
                QemuPacketKey key = (QemuPacketKey) o;
                if (key.seq == this.seq &&
                        key.vmUid == vmUid) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "QemuPacketKey: " + vmUid + ':' + seq; //$NON-NLS-1$
        }

    }

    /**
     * Computes the unique key for a given event.
     *
     * @param event
     *            The event for which to compute the key
     * @return the unique key for this event
     */
    @Override
    public @Nullable IEventMatchingKey getEventKey(@Nullable ITmfEvent event) {
        if (event == null) {
            return null;
        }
        String evname = event.getType().getName();

        IEventMatchingKey key = null;

        if (evname.equals(QemuKvmStrings.VMSYNC_GH_HOST) || evname.equals(QemuKvmStrings.VMSYNC_HG_HOST)) {
            TmfEventField field = (TmfEventField) event.getContent();
            ITmfEventField data = field.getField(VM_UID_PAYLOAD);
            if (data == null) {
                return null;
            }
            long vmUid = (Long) data.getValue();
            data = field.getField(COUNTER_PAYLOAD);
            if (data == null) {
                return null;
            }
            long seqno = (Long) data.getValue();
            key = new QemuPacketKey(vmUid, seqno);
        } else if (evname.equals(QemuKvmStrings.VMSYNC_GH_GUEST) || evname.equals(QemuKvmStrings.VMSYNC_HG_GUEST)) {
            TmfEventField field = (TmfEventField) event.getContent();
            ITmfEventField data = field.getField(VM_UID_PAYLOAD);
            if (data == null) {
                return null;
            }
            long vmUid = (Long) data.getValue();
            data = field.getField(COUNTER_PAYLOAD);
            if (data == null) {
                return null;
            }
            long seqno = (Long) data.getValue();
            key = new QemuPacketKey(vmUid, seqno);
        }

        return key;
    }

    @Override
    public boolean canMatchTrace(@Nullable ITmfTrace trace) {
        if (trace instanceof ITmfTraceWithPreDefinedEvents) {
            Collection<String> events = ImmutableSet.of(QemuKvmStrings.VMSYNC_GH_HOST, QemuKvmStrings.VMSYNC_GH_GUEST, QemuKvmStrings.VMSYNC_HG_HOST, QemuKvmStrings.VMSYNC_HG_GUEST);
            Set<String> traceEvents = TmfEventTypeCollectionHelper.getEventNames(((ITmfTraceWithPreDefinedEvents) trace).getContainedEventTypes());
            traceEvents.retainAll(events);
            return !traceEvents.isEmpty();
        }
        return true;
    }

    @Override
    public @Nullable Direction getDirection(@Nullable ITmfEvent event) {
        if (event == null) {
            return null;
        }
        String evname = event.getType().getName();

        /* Is the event a source or destination event */
        if ((evname.equals(QemuKvmStrings.VMSYNC_GH_HOST) || evname.equals(QemuKvmStrings.VMSYNC_HG_GUEST))
                && canMatchEvent(event)) {
            return Direction.EFFECT;
        } else if ((evname.equals(QemuKvmStrings.VMSYNC_GH_GUEST) || evname.equals(QemuKvmStrings.VMSYNC_HG_HOST))
                && canMatchEvent(event)) {
            return Direction.CAUSE;
        }
        return null;
    }

    private static boolean canMatchEvent(final ITmfEvent event) {
        /* Make sure all required fields are present to match with this event */
        String evname = event.getType().getName();

        if (evname.equals(QemuKvmStrings.VMSYNC_GH_GUEST) || evname.equals(QemuKvmStrings.VMSYNC_GH_HOST) ||
                evname.equals(QemuKvmStrings.VMSYNC_HG_GUEST) || evname.equals(QemuKvmStrings.VMSYNC_HG_HOST)) {

            ITmfEventField content = event.getContent();
            if ((content.getField(VM_UID_PAYLOAD) != null) &&
                    (content.getField(COUNTER_PAYLOAD) != null)) {
                return true;
            }
        }
        return false;
    }

}