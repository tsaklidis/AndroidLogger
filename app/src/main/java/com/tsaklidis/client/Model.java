package com.tsaklidis.client;

public class Model {
    double value;
    String created_on;
    Sensor sensor;

    public double getValue() { return value; }

    public String getValueStr() { return String.valueOf(value); }

    public String getCreated_on() {
        return created_on;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public static class Sensor {
        String kind;
        String uuid;
        String name;

        public String getKind() {
            return kind;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }
    }
}
