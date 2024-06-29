/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo;

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.KB2MB;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_INT;

import java.util.Objects;

public class GCMemoryItem {
    private MemoryArea fArea;

    // memory size in kb
    private long preUsed = UNKNOWN_INT;
    private long preCapacity = UNKNOWN_INT;
    private long postUsed = UNKNOWN_INT;
    private long postCapacity = UNKNOWN_INT;

    public GCMemoryItem(MemoryArea area) {
        this.setArea(area);
    }

    public GCMemoryItem(MemoryArea area, long preUsed, long preCapacity, long postUsed, long postCapacity) {
        this.setArea(area);
        this.setPreUsed(preUsed);
        this.setPreCapacity(preCapacity);
        this.postUsed = postUsed;
        this.postCapacity = postCapacity;
    }

    public GCMemoryItem(MemoryArea area, long preUsed, long postUsed, long postCapacity) {
        this.setArea(area);
        this.setPreUsed(preUsed);
        this.setPreCapacity(UNKNOWN_INT);
        this.postUsed = postUsed;
        this.postCapacity = postCapacity;
    }

    public GCMemoryItem(MemoryArea area, long []memories) {
        this(area, memories[0],memories[1],memories[2], memories[3]);
    }

    public long getMemoryReduction() {
        return minus(getPreUsed(), getPostUsed());
    }

    /**
     * unknown value in this or anotherItem will lead result to be unknown.
     */
    public GCMemoryItem merge(GCMemoryItem anotherItem) {
        if (anotherItem == null) {
            return new GCMemoryItem(getArea());
        }
        return new GCMemoryItem(getArea(),
                plus(getPreUsed(), anotherItem.getPreUsed()),
                plus(getPreCapacity(), anotherItem.getPreCapacity()),
                plus(getPostUsed(), anotherItem.getPostUsed()),
                plus(postCapacity, anotherItem.postCapacity));
    }

    /**
     * unknown value in this will lead result to be unknown. unknown value in
     * anotherItem are seen as 0
     */
    public GCMemoryItem mergeIfPresent(GCMemoryItem anotherItem) {
        if (anotherItem == null) {
            return this;
        }
        return new GCMemoryItem(getArea(),
                plusIfPresent(getPreUsed(), anotherItem.getPreUsed()),
                plusIfPresent(getPreCapacity(), anotherItem.getPreCapacity()),
                plusIfPresent(getPostUsed(), anotherItem.getPostUsed()),
                plusIfPresent(postCapacity, anotherItem.postCapacity));
    }

    /**
     * unknown value in this or anotherItem will lead result to be unknown.
     */
    public GCMemoryItem subtract(GCMemoryItem anotherItem) {
        if (anotherItem == null) {
            return new GCMemoryItem(getArea());
        }
        return new GCMemoryItem(getArea(),
                minus(getPreUsed(), anotherItem.getPreUsed()),
                minus(getPreCapacity(), anotherItem.getPreCapacity()),
                minus(getPostUsed(), anotherItem.getPostUsed()),
                minus(postCapacity, anotherItem.postCapacity));
    }

    /**
     * unknown value in this will lead result to be unknown. unknown value in
     * anotherItem are seen as 0
     */
    public GCMemoryItem subtractIfPresent(GCMemoryItem anotherItem) {
        if (anotherItem == null) {
            return this;
        }
        return new GCMemoryItem(getArea(),
                minusIfPresent(getPreUsed(), anotherItem.getPreUsed()),
                minusIfPresent(getPreCapacity(), anotherItem.getPreCapacity()),
                minusIfPresent(getPostUsed(), anotherItem.getPostUsed()),
                minusIfPresent(postCapacity, anotherItem.postCapacity));
    }

    public GCMemoryItem updateIfAbsent(GCMemoryItem anotherItem) {
        if (anotherItem == null) {
            return this;
        }
        return new GCMemoryItem(getArea(),
                getPreUsed() == UNKNOWN_INT ? anotherItem.getPreUsed() : getPreUsed(),
                getPreCapacity() == UNKNOWN_INT ? anotherItem.getPreCapacity() : getPreCapacity(),
                getPostUsed() == UNKNOWN_INT ? anotherItem.getPostUsed() : getPostUsed(),
                postCapacity == UNKNOWN_INT ? anotherItem.postCapacity : postCapacity);
    }

    private static long plus(long x, long y) {
        if (x == UNKNOWN_INT || y == UNKNOWN_INT) {
            return UNKNOWN_INT;
        }
        return x + y;
    }

    private static long plusIfPresent(long x, long y) {
        if (x == UNKNOWN_INT || y == UNKNOWN_INT) {
            return x;
        }
        return x + y;
    }

    private static long minus(long x, long y) {
        if (x == UNKNOWN_INT || y == UNKNOWN_INT) {
            return UNKNOWN_INT;
        }
        return x >= y ? x - y : 0;
    }

    private static long minusIfPresent(long x, long y) {
        if (x == UNKNOWN_INT || y == UNKNOWN_INT) {
            return x;
        }
        return x >= y ? x - y : 0;
    }

    public void multiply(long x) {
        if (getPreUsed() != UNKNOWN_INT) {
            setPreUsed(getPreUsed() * x);
        }
        if (getPreCapacity() != UNKNOWN_INT) {
            setPreCapacity(getPreCapacity() * x);
        }
        if (getPostUsed() != UNKNOWN_INT) {
            postUsed = getPostUsed() * x;
        }
        if (postCapacity != UNKNOWN_INT) {
            postCapacity *= x;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GCMemoryItem item = (GCMemoryItem) o;
        return getPreUsed() == item.getPreUsed() && getPreCapacity() == item.getPreCapacity() && getPostUsed() == item.getPostUsed() && postCapacity == item.postCapacity && getArea() == item.getArea();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArea(), getPreUsed(), getPreCapacity(), getPostUsed(), postCapacity);
    }

    public boolean isEmpty() {
        return getPreUsed() == UNKNOWN_INT && getPreCapacity() == UNKNOWN_INT && getPostUsed() == UNKNOWN_INT && postCapacity == UNKNOWN_INT;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String area = this.getArea().toString().toLowerCase();
        sb.append((char) (area.charAt(0) - 32)).append(area.substring(1)).append(": ");
        if (isEmpty()) {
            sb.append("unknown");
        } else {
            if (getPreUsed() != UNKNOWN_INT) {
                sb.append((long) (Math.max(0, getPreUsed()) / KB2MB / KB2MB)).append("M");
            }
            if (getPreCapacity() != UNKNOWN_INT) {
                sb.append('(').append((long) (Math.max(0, getPreCapacity()) / KB2MB / KB2MB)).append("M)");
            }
            if (getPreUsed() != UNKNOWN_INT || getPreCapacity() != UNKNOWN_INT) {
                sb.append("->");
            }
            if (getPostUsed() != UNKNOWN_INT) {
                sb.append((long) (Math.max(0, getPostUsed()) / KB2MB / KB2MB)).append('M');
            } else {
                sb.append("unknown");
            }
            if (postCapacity != UNKNOWN_INT) {
                sb.append('(').append((long) (Math.max(0, postCapacity) / KB2MB / KB2MB)).append("M)");
            }
        }
        return sb.toString();
    }

    public long getPreUsed() {
        return preUsed;
    }

    public void setPreUsed(long preUsed) {
        this.preUsed = preUsed;
    }

    public long getPreCapacity() {
        return preCapacity;
    }

    public void setPreCapacity(long preCapacity) {
        this.preCapacity = preCapacity;
    }

    public MemoryArea getArea() {
        return fArea;
    }

    public void setArea(MemoryArea area) {
        fArea = area;
    }

    /**
     * @return the postUsed
     */
    public long getPostUsed() {
        return postUsed;
    }

    public void setPostUsed(int i) {
        postUsed = i;
    }

    /**
     * @return the postCapacity
     */
    public long getPostCapacity() {
        return postCapacity;
    }

    /**
     * @param postCapacity the postCapacity to set
     */
    public void setPostCapacity(long postCapacity) {
        this.postCapacity = postCapacity;
    }

    /**
     * @param postUsed the postUsed to set
     */
    public void setPostUsed(long postUsed) {
        this.postUsed = postUsed;
    }
}
