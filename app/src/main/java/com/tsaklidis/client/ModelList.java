package com.tsaklidis.client;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ModelList {
    int count;

    @SerializedName("next")
    String next;

    @SerializedName("results")
    List<Model> results;

    public int getCount() {
        return count;
    }

    public String getNext() {
        return next;
    }

    public List<Model> getResults() {
        return results;
    }
}
