package com.tsaklidis.client;

import retrofit2.Call;
import retrofit2.http.GET;

public interface Methods {
    @GET("list/last/?sensor_uuid=9bd60&client=android")
    Call<Model> getLastTemprData();

    @GET("list/last/?sensor_uuid=90eab&client=android")
    Call<Model> getLastHumData();

    @GET("list/last/?sensor_uuid=f7909564&client=android")
    Call<Model> getLastPressData();
}
