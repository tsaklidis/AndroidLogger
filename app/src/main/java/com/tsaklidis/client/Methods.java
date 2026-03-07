package com.tsaklidis.client;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface Methods {
    @GET("api/open/measurement/list/last/?sensor_uuid=9bd60&client=android")
    Call<Model> getLastTemprData();

    @GET("api/open/measurement/list/last/?sensor_uuid=90eab&client=android")
    Call<Model> getLastHumData();

    @GET("api/open/measurement/list/last/?sensor_uuid=f7909564&client=android")
    Call<Model> getLastPressData();

    @GET("api/open/measurement/list/?sensor_uuid=9bd60&client=android&latest_hours=12")
    Call<ModelList> getTemperature12h(@Query("page") int page);

    @GET("api/measurement/list/?sensor_uuid=9bd60&client=android&latest_hours=12")
    Call<ModelList> getAuthTemperature12h(@Query("page") int page);

    @GET("api/measurement/list/last/")
    Call<Model> getLastMeasurementBySpace(
            @Query("space_uuid") String spaceUuid,
            @Query("sensor_uuid") String sensorUuid,
            @Query("client") String client
    );

    @GET("api/measurement/list/last/?client=android")
    Call<ModelList> getMeasurementsBulk(
            @Query("sensor_uuid") List<String> sensorUuids
    );

    @GET("api/open/measurement/list/last/?client=android")
    Call<ModelList> getOpenMeasurementsBulk(
            @Query("sensor_uuid") List<String> sensorUuids
    );

    @GET("api/house/my")
    Call<List<HouseModel>> getMyHouses();
}
