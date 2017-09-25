/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Raphaël Beamonte
 */
class InterruptionReasonSet implements Cloneable {
    private Map<InterruptionReason, Integer> map = new HashMap<>();

    public void add(InterruptionReason ir) {
        add(ir, 1);
    }

    public void add(InterruptionReason ir, int n) {
        Integer i = map.get(ir);
        if (i == null) {
            map.put(ir, new Integer(n));
        } else {
            i += n;
        }
    }

    public int remove(InterruptionReason ir) {
        Integer i = map.get(ir);
        if (i == null) {
            return -1;
        } else if (i < 2) {
            map.remove(ir);
            return 0;
        } else {
            i--;
            return i;
        }
    }

    public int size() {
        int s = 0;
        for (Integer i : map.values()) {
            s += i;
        }
        return s;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((map == null) ? 0 : map.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        InterruptionReasonSet other = (InterruptionReasonSet) obj;
        if (map == null) {
            if (other.map != null) {
                return false;
            }
        } else if (!map.equals(other.map)) {
            return false;
        }
        return true;
    }
}