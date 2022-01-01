package com.tsaklidis.client;

import android.appwidget.AppWidgetManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.RemoteViews;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class Data {
    private static final String TAG = "Data";
    public String temperature;
    public String humidity;
    public String pressure;
    public String created_on;
    private ArrayList arrayList = new ArrayList();
    AppWidgetManager widgetManager;
    RemoteViews views = null;
    ArrayAdapter adapter = null;
    int appWidgetId;

    Methods methods = RetrofitClient.getRetrofitInstance().create(Methods.class);
    Call<Model> temperature_method = methods.getLastTemprData();
    Call<Model> humidity_method = methods.getLastHumData();
    Call<Model> pressure_method = methods.getLastPressData();

    private void update_data(){
        //adapter has changed
        if (views != null){
            widgetManager.updateAppWidget(appWidgetId, views);
        }
        if (adapter != null){
            adapter.notifyDataSetChanged();
        }
    }

    public Data(AppWidgetManager widgetManager, RemoteViews views, int appWidgetId) {
        //Constructor overload
        this.temperature = getTemperature();
        this.humidity = getHumidity();
        this.pressure = getPressure();
        this.widgetManager = widgetManager;
        this.views = views;
        this.appWidgetId = appWidgetId;
    }
    public Data(ArrayAdapter adapter, ArrayList remoteArrayList) {
        // Constructor overload
        this.temperature = getTemperature();
        this.humidity = getHumidity();
        this.pressure = getPressure();
        this.adapter = adapter;
        this.arrayList = remoteArrayList;
    }

    public String getTemperature() {
        temperature_method.clone().enqueue(new Callback<Model>() {
            @Override
            public void onResponse(Call<Model> call, Response<Model> response) {
                temperature = String.valueOf(response.body().getValue());
                created_on = response.body().getCreated_on();

                //"2022-01-01T17:10:15.977152+02:00"
                created_on = created_on.substring(11, 16);


                if (views != null){
                    views.setTextViewText(R.id.temperature, "Θερμοκρασία: " + temperature + " \u2103");
                    views.setTextViewText(R.id.created_on, "Ώρα μέτρησης: " + created_on);
                }
                if (adapter != null){
                    arrayList.add("Time: " + String.valueOf(created_on));
                    arrayList.add("Temperature: " + temperature + " \u2103");
                }
                update_data();
                Log.d(TAG, "called getTemperature(): " + temperature);

            }
            @Override
            public void onFailure(Call<Model> call, Throwable t) {
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
        return temperature;

    }

    public String getHumidity() {
        humidity_method.clone().enqueue(new Callback<Model>() {
            @Override
            public void onResponse(Call<Model> call, Response<Model> response) {
                humidity = String.valueOf(response.body().getValue());
                Log.d(TAG, "called getHumidity(): " + humidity);
                if (views != null) {
                    views.setTextViewText(R.id.humidity, "Υγρασία: " + humidity + " %Rh");
                }
                if (adapter != null){
                    arrayList.add("Humidity: " + humidity + " %Rh");
                }

                update_data();

            }
            @Override
            public void onFailure(Call<Model> call, Throwable t) {
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
        return humidity;
    }

    public String getPressure() {
        pressure_method.clone().enqueue(new Callback<Model>() {
            @Override
            public void onResponse(Call<Model> call, Response<Model> response) {
                pressure = String.valueOf(response.body().getValue());
                if (views != null){
                    views.setTextViewText(R.id.pressure, "Βαρομετρική πίεση: " + pressure + " hPa");
                }
                if (adapter != null){
                    arrayList.add("Pressure: " + pressure + " hPa");
                }
                update_data();
                Log.d(TAG, "called getPressure(): " + pressure);


            }
            @Override
            public void onFailure(Call<Model> call, Throwable t) {
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
        return pressure;
    }
}
