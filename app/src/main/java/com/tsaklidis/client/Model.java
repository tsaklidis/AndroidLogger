package com.tsaklidis.client;

public class Model {
    double value;
    String created_on;

    public double getValue() { return value; }

    public String getValueStr() { return String.valueOf(value); }

    public String getCreated_on() {
        return created_on;
    }
}
