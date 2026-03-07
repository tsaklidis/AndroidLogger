package com.tsaklidis.client;

import java.io.Serializable;
import java.util.UUID;

public class SensorConfig implements Serializable {
    private String id;
    private String name;
    private String spaceUuid;
    private String sensorUuid;
    private String lastValue = "N/A";
    private String lastTime = "-";
    private String kind = "unknown";
    private boolean hidden = false;

    public SensorConfig(String name, String spaceUuid, String sensorUuid) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.spaceUuid = spaceUuid;
        this.sensorUuid = sensorUuid;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpaceUuid() { return spaceUuid; }
    public void setSpaceUuid(String spaceUuid) { this.spaceUuid = spaceUuid; }
    public String getSensorUuid() { return sensorUuid; }
    public void setSensorUuid(String sensorUuid) { this.sensorUuid = sensorUuid; }
    public String getLastValue() { return lastValue; }
    public void setLastValue(String lastValue) { this.lastValue = lastValue; }
    public String getLastTime() { return lastTime; }
    public void setLastTime(String lastTime) { this.lastTime = lastTime; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}
