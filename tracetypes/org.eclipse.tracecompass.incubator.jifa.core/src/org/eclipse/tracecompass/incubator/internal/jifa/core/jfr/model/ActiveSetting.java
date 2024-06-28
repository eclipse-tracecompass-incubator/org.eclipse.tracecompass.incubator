package org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model;

import java.util.Objects;

public class ActiveSetting {
    private String fEventType;
    private Long fEventId;

    /**
     * @return the eventType
     */
    public String getEventType() {
        return fEventType;
    }

    /**
     * @param eventType
     *            the eventType to set
     */
    public void setEventType(String eventType) {
        fEventType = eventType;
    }

    /**
     * @return the eventId
     */
    public Long getEventId() {
        return fEventId;
    }

    /**
     * @return the eventId
     */
    public Long eventId() {
        return fEventId;
    }

    /**
     * @param eventId
     *            the eventId to set
     */
    public void setEventId(Long eventId) {
        fEventId = eventId;
    }

    /**
     * @return the settingName
     */
    public String getSettingName() {
        return fSettingName;
    }

    /**
     * @param settingName
     *            the settingName to set
     */
    public void setSettingName(String settingName) {
        fSettingName = settingName;
    }

    private String fSettingName;

    public ActiveSetting(String eventType, Long eventId, String settingName) {
        fEventType = eventType;
        fEventId = eventId;
        fSettingName = settingName;
    }

    @Override
    public boolean equals(Object b) {
        if (!(b instanceof ActiveSetting other)) {
            return false;
        }

        return Objects.equals(fEventType, other.getEventType())
                && Objects.equals(fEventId, other.getEventId())
                && Objects.equals(fSettingName, other.getSettingName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fEventType, fEventId, fSettingName);
    }
}