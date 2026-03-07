package com.tsaklidis.client;

import java.util.List;

public class HouseModel {
    private String name;
    private String uuid;
    private List<Space> spaces;

    public String getName() { return name; }
    public String getUuid() { return uuid; }
    public List<Space> getSpaces() { return spaces; }

    public static class Space {
        private String name;
        private String uuid;
        private List<Sensor> sensors;

        public String getName() { return name; }
        public String getUuid() { return uuid; }
        public List<Sensor> getSensors() { return sensors; }
    }

    public static class Sensor {
        private String kind;
        private String uuid;
        private String name;

        public String getKind() { return kind; }
        public String getUuid() { return uuid; }
        public String getName() { return name; }
    }
}
