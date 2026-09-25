/**
 *
 */
package org.eclipse.tracecompass.incubator.internal.perfetto.core.event;

import java.util.Hashtable;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;

/**
 *
 */
public class PerfettoEventData extends TmfEventField {

    @SuppressWarnings("null")
    private static PerfettoEventData[] getExtraFields(Hashtable<String, String> extras) {
        PerfettoEventData[] fields = new PerfettoEventData[extras.size()];
        int i = 0;
        for (String key : extras.keySet()) {
            fields[i] = new PerfettoEventData(key, extras.get(key));
            i++;
        }
        return fields;
    }

    /**
     * @param name
     * @param value
     * @param fields
     */
    @SuppressWarnings("javadoc")
    public PerfettoEventData(@NonNull String name, String value, Hashtable<String, String> extras) {
        super(name, value, getExtraFields(extras));
    }

    /**
     * @param name
     * @param value
     */
    public PerfettoEventData(@NonNull String name, String value) {
        super(name, value, null);
    }


    /**
     * @param field
     */
    public PerfettoEventData(TmfEventField field) {
        super(field);
    }

}
