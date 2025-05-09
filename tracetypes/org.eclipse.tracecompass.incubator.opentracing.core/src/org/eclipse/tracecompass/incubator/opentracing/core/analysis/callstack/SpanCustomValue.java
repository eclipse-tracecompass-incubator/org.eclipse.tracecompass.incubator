package org.eclipse.tracecompass.incubator.opentracing.core.analysis.callstack;



import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.datastore.core.serialization.SafeByteBufferFactory;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * The states in concurrent state system is composed of spanId and parent spanId since there might be
 * different spans with the same name and the combination of spanId and parent spanId is unique.
 * The funcName also in included in the class to represent in UI.
 *
 * @author fateme faraji daneshgar
 *
 */
@SuppressWarnings("restriction")
public class SpanCustomValue extends CustomStateValue {

    public static final CustomStateValueFactory FACTORY = (b) -> {
        String spanStri = b.getString();
        String parentStr = b.getString();
        String func = b.getString();
        return new SpanCustomValue(spanStri, parentStr, func);
    };

    /** Custom type ID */
    public static final byte CUSTOM_TYPE_ID = 115;

    private final String spanId;
    private final String parentSpanId;
    private final String funcName;

    public SpanCustomValue(String span, String parentSpan, String func) {
        spanId = span;
        parentSpanId = parentSpan;
        funcName = func;
    }

    @Override
    public int compareTo(@Nullable ITmfStateValue o) {
        if (o == null) {
            throw new IllegalArgumentException();
        }
        if (!(o instanceof SpanCustomValue)) {
            throw new StateValueTypeException("Need a TestCustomStateValue object to compare to"); //$NON-NLS-1$
        }
        SpanCustomValue other = (SpanCustomValue) o;
        if (spanId.equals(other.spanId)) {
            if (parentSpanId.equals(other.parentSpanId)) {
                if (funcName.equals(other.funcName)) {
                    return 1;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean equals(@Nullable Object arg0) {
        if (!(arg0 instanceof SpanCustomValue)) {
            return false;
        }
        SpanCustomValue tcsv = (SpanCustomValue) arg0;
        return (spanId.equals(tcsv.spanId)) && (parentSpanId.equals(tcsv.parentSpanId) && (funcName.equals(tcsv.funcName)));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + spanId.hashCode();
        result = prime * result + parentSpanId.hashCode();
        result = prime * result + funcName.hashCode();

        return result;
    }

    @Override
    public String toString() {
        return "[span:" + spanId + ",parentSpan:" + parentSpanId + ",opName:" + funcName + "]";
    }

    @Override
    protected void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        buffer.putString(spanId);
        buffer.putString(parentSpanId);
        buffer.putString(funcName);
    }

    @Override
    protected int getSerializedValueSize() {
        return SafeByteBufferFactory.getStringSizeInBuffer(spanId) + SafeByteBufferFactory.getStringSizeInBuffer(parentSpanId) + SafeByteBufferFactory.getStringSizeInBuffer(funcName);
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    public String getSpanId() {
        return this.spanId;
    }

    public String getParentId() {
        return this.parentSpanId;
    }

    public String getFunction() {
        return this.funcName;
    }

}
