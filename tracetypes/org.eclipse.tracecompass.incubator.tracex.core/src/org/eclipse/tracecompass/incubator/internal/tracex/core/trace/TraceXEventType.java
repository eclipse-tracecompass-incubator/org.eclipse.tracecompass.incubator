package org.eclipse.tracecompass.incubator.internal.tracex.core.trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;

public enum TraceXEventType {

    TX_TRACE_THREAD_RESUME(1, "thread ptr", "previous_state", "stack ptr", "next thread"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_THREAD_SUSPEND(2, "thread ptr", "new_state", "stack ptr", "next thread"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_ISR_ENTER(3, "stack_ptr", "ISR number", "system state", "preempt disable"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_ISR_EXIT(4, "stack_ptr", "ISR number", "system state", "preempt disable"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_TIME_SLICE(5, "next thread ptr", "system state", "preempt disable", "stack"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_RUNNING(6),

    TX_TRACE_BLOCK_ALLOCATE(10, "pool ptr", "memory ptr", "wait option", "remaining blocks"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_BLOCK_POOL_CREATE(11, "pool ptr", "pool_start", "total blocks", "block size"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_BLOCK_POOL_DELETE(12, "pool ptr", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_BLOCK_POOL_INFO_GET(13, "pool ptr"), //$NON-NLS-1$
    TX_TRACE_BLOCK_POOL_PERFORMANCE_INFO_GET(14, "pool ptr"), //$NON-NLS-1$
    TX_TRACE_BLOCK_POOL__PERFORMANCE_SYSTEM_INFO_GET(15), TX_TRACE_BLOCK_POOL_PRIORITIZE(16, "pool ptr", "suspended count", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_BLOCK_RELEASE(17, "pool ptr", "memory ptr", "suspended", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    TX_TRACE_BYTE_ALLOCATE(20, "pool ptr", "memory ptr", "size requested", "wait option"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_BYTE_POOL_CREATE(21, "pool ptr", "start ptr", "pool size", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_BYTE_POOL_DELETE(22, "pool ptr", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_BYTE_POOL_INFO_GET(23, "pool ptr"), //$NON-NLS-1$
    TX_TRACE_BYTE_POOL_PERFORMANCE_INFO_GET(24, "pool ptr"), //$NON-NLS-1$
    TX_TRACE_BYTE_POOL__PERFORMANCE_SYSTEM_INFO_GET(25), TX_TRACE_BYTE_POOL_PRIORITIZE(26, "pool ptr", "suspended count", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_BYTE_RELEASE(27, "pool ptr", "memory ptr", "suspended", "available bytes"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    TX_TRACE_EVENT_FLAGS_CREATE(30, "group ptr", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_EVENT_FLAGS_DELETE(31, "group ptr", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_EVENT_FLAGS_GET(32, "group ptr", "requested flags", "current flags", "get option"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_EVENT_FLAGS_INFO_GET(33, "group ptr"), //$NON-NLS-1$
    TX_TRACE_EVENT_FLAGS_PERFORMANCE_INFO_GET(34, "group ptr"), //$NON-NLS-1$
    TX_TRACE_EVENT_FLAGS__PERFORMANCE_SYSTEM_INFO_GET(35), TX_TRACE_EVENT_FLAGS_SET(36, "group ptr", "flags to set", "set option", "suspended count"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_EVENT_FLAGS_SET_NOTIFY(37, "group ptr"), //$NON-NLS-1$

    TX_TRACE_INTERRUPT_CONTROL(40, "new interrupt posture", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$

    TX_TRACE_MUTEX_CREATE(50, "mutex ptr", "inheritance", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_MUTEX_DELETE(51, "mutex ptr", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_MUTEX_GET(52, "mutex ptr", "wait option", "owning thread", "own count"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_MUTEX_INFO_GET(53, "mutex ptr"), //$NON-NLS-1$
    TX_TRACE_MUTEX_PERFORMANCE_INFO_GET(54, "mutex ptr"), //$NON-NLS-1$
    TX_TRACE_MUTEX_PERFORMANCE_SYSTEM_INFO_GET(55), TX_TRACE_MUTEX_PRIORITIZE(56, "mutex ptr", "suspended count", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_MUTEX_PUT(57, "mutex ptr", "owning thread", "own count", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    TX_TRACE_QUEUE_CREATE(60, "queue ptr", "message size", "queue start", "queue size"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_QUEUE_DELETE(61, "queue ptr", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_QUEUE_FLUSH(62, "queue ptr", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_QUEUE_FRONT_SEND(63, "queue ptr", "source ptr", "wait option", "enqueued"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_QUEUE_INFO_GET(64, "queue ptr"), //$NON-NLS-1$
    TX_TRACE_QUEUE_PERFORMANCE_INFO_GET(65, "queue ptr"), //$NON-NLS-1$
    TX_TRACE_QUEUE_PERFORMANCE_SYSTEM_INFO_GET(66), TX_TRACE_QUEUE_PRIORITIZE(67, "queue ptr", "suspended count", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_QUEUE_RECEIVE(68, "queue ptr", "destination ptr", "wait option", "enqueued"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_QUEUE_SEND(69, "queue ptr", "source ptr", "wait option", "enqueued"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_QUEUE_SEND_NOTIFY(70, "queue ptr"), //$NON-NLS-1$

    TX_TRACE_SEMAPHORE_CEILING_PUT(80, "semaphore ptr", "current count", "suspended count", "ceiling"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_SEMAPHORE_CREATE(81, "semaphore ptr", "initial count", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_SEMAPHORE_DELETE(82, "semaphore ptr", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_SEMAPHORE_GET(83, "semaphore ptr", "wait option", "current count", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_SEMAPHORE_INFO_GET(84, "semaphore ptr"), //$NON-NLS-1$
    TX_TRACE_SEMAPHORE_PERFORMANCE_INFO_GET(85, "semaphore ptr"), //$NON-NLS-1$
    TX_TRACE_SEMAPHORE__PERFORMANCE_SYSTEM_INFO_GET(86), TX_TRACE_SEMAPHORE_PRIORITIZE(87, "semaphore ptr", "suspended count", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_SEMAPHORE_PUT(88, "semaphore ptr", "current count", "suspended count", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_SEMAPHORE_PUT_NOTIFY(89, "semaphore ptr"), //$NON-NLS-1$

    TX_TRACE_THREAD_CREATE(100, "thread ptr", "priority", "stack ptr", "stack_size"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_THREAD_DELETE(101, "thread ptr", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_THREAD_ENTRY_EXIT_NOTIFY(102, "thread ptr", "thread state", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_THREAD_IDENTIFY(103), TX_TRACE_THREAD_INFO_GET(104, "thread ptr", "thread state"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_THREAD_PERFORMANCE_INFO_GET(105, "thread ptr", "thread state"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_THREAD_PERFORMANCE_SYSTEM_INFO_GET(106), TX_TRACE_THREAD_PREEMPTION_CHANGE(107, "thread ptr", "new threshold", "old threshold", "thread state"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_THREAD_PRIORITY_CHANGE(108, "thread ptr", "new priority", "old priority", "thread state"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_THREAD_RELINQUISH(109, "stack ptr", "next thread ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_THREAD_RESET(110, "thread ptr", "thread state"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_THREAD_RESUME_API(111, "thread ptr", "thread state", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_THREAD_SLEEP(112, "sleep value", "thread state", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_THREAD_STACK_ERROR_NOTIFY(113), TX_TRACE_THREAD_SUSPEND_API(114, "thread ptr", "thread state", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_THREAD_TERMINATE(115, "thread ptr", "thread state", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_THREAD_TIME_SLICE_CHANGE(116, "thread ptr", "new timeslice", "old timeslice"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_THREAD_WAIT_ABORT(117, "thread ptr", "thread state", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    TX_TRACE_TIME_GET(120, "current time", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_TIME_SET(121, "new time"), //$NON-NLS-1$
    TX_TRACE_TIMER_ACTIVATE(122, "timer ptr"), //$NON-NLS-1$
    TX_TRACE_TIMER_CHANGE(123, "timer ptr", "initial ticks", "reschedule ticks"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    TX_TRACE_TIMER_CREATE(124, "timer ptr", "initial ticks", "reschedule ticks", "enable"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    TX_TRACE_TIMER_DEACTIVATE(125, "timer ptr", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_TIMER_DELETE(126, "timer ptr"), //$NON-NLS-1$
    TX_TRACE_TIMER_INFO_GET(127, "timer ptr", "stack ptr"), //$NON-NLS-1$ //$NON-NLS-2$
    TX_TRACE_TIMER_PERFORMANCE_INFO_GET(128, "timer ptr"), //$NON-NLS-1$
    TX_TRACE_TIMER_PERFORMANCE_SYSTEM_INFO_GET(129);

    public static final TraceXEventType[] EVENT_TYPES = new TraceXEventType[4096];
    static {
        for (TraceXEventType eventType : TraceXEventType.values()) {
            EVENT_TYPES[eventType.getIndex()] = eventType;
        }

    }

    private ITmfEventType fEventType;
    private int fIndex;
    private List<String> fFieldNames;

    /* None */
    private TraceXEventType(int type, String... fieldnames) {
        fIndex = type;
        fFieldNames = new ArrayList<>();
        fFieldNames.addAll(Arrays.asList("Thread Pointer", "Thread Priority")); //$NON-NLS-1$ //$NON-NLS-2$
        fFieldNames.addAll(Arrays.asList(fieldnames));
        fEventType = new ITmfEventType() {

            @Override
            public ITmfEventField getRootField() {
                return TmfEventField.makeRoot(fFieldNames.toArray(new String[0]));
            }

            @Override
            public @NonNull String getName() {
                return name();
            }

            @Override
            public Collection<String> getFieldNames() {
                return fFieldNames;
            }
        };
    }

    /**
     * the event type
     *
     * @return the type
     */
    public ITmfEventType getEventType() {
        return fEventType;
    }

    /**
     * Get index
     *
     * @return index
     */
    public int getIndex() {
        return fIndex;
    }

}