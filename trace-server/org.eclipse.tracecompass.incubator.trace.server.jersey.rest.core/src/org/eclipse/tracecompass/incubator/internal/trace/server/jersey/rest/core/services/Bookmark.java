package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.io.Serializable;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Bookmark model for TSP
 *
 * @author Kaveh Shahedi
 * @since 10.1
 */
public class Bookmark implements Serializable {
    private static final long serialVersionUID = -3626414315455912960L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final UUID fUUID;
    private final String fName;
    private final String fExperimentId;
    private final long fStart;
    private final long fEnd;
    private final JsonNode fPayload;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param uuid
     *            the stub's UUID
     * @param name
     *            bookmark name
     * @param experimentId
     *            experiment id
     * @param start
     *            start time
     * @param end
     *            end time
     * @param payload
     *            additional JSON data associated with the bookmark (optional)
     */
    @JsonCreator
    public Bookmark(
            @JsonProperty("UUID") UUID uuid,
            @JsonProperty("name") String name,
            @JsonProperty("experimentId") String experimentId,
            @JsonProperty("start") long start,
            @JsonProperty("end") long end,
            @JsonProperty(value = "payload", required = false) JsonNode payload) {
        fUUID = uuid;
        fName = name;
        fExperimentId = experimentId;
        fStart = start;
        fEnd = end;
        fPayload = (payload != null) ? payload : MAPPER.createObjectNode();
    }

    /**
     * Constructor without payload
     *
     * @param uuid
     *            the stub's UUID
     * @param name
     *            bookmark name
     * @param experimentId
     *            experiment id
     * @param start
     *            start time
     * @param end
     *            end time
     */
    public Bookmark(UUID uuid, String name, String experimentId, long start, long end) {
        this(uuid, name, experimentId, start, end, MAPPER.createObjectNode());
    }

    /**
     * Get the UUID
     *
     * @return the UUID
     */
    public UUID getUUID() {
        return fUUID;
    }

    /**
     * Get the bookmark name
     *
     * @return the bookmark name
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the experiment id
     *
     * @return the experiment id
     */
    public String getExperimentId() {
        return fExperimentId;
    }

    /**
     * Get the start time
     *
     * @return the start time
     */
    public long getStart() {
        return fStart;
    }

    /**
     * Get the end time
     *
     * @return the end time
     */
    public long getEnd() {
        return fEnd;
    }

    /**
     * Get the payload
     *
     * @return the JSON payload, empty JSON object if no payload was set
     */
    public JsonNode getPayload() {
        return fPayload;
    }

    @Override
    public String toString() {
        return "Bookmark [fUUID=" + fUUID + ", fName=" + fName + ", fExperimentId=" + fExperimentId //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
               + ", fStart=" + fStart + ", fEnd=" + fEnd + ", fPayload=" + fPayload + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
}