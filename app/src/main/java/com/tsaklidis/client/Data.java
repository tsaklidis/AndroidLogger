package com.tsaklidis.client;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class Data {
    private static final String TAG = "Data";
    
    // Core Sensor UUIDs for Open Data
    private static final String UUID_TEMP = "9bd60";
    private static final String UUID_HUM = "90eab";
    private static final String UUID_PRESS = "f7909564";

    public String temperature;
    public String humidity;
    public String pressure;
    public String created_on;
    
    private final Context context;
    private final AppWidgetManager widgetManager;
    private final RemoteViews views;
    private final int appWidgetId;
    private final SensorAdapter adapter;
    
    private final AtomicInteger activeCalls = new AtomicInteger(0);
    private final Methods methods;
    private DataUpdateListener listener;

    public interface DataUpdateListener {
        void onFinished();
        void onError(String message);
    }

    private void callFinished() {
        int remaining = activeCalls.decrementAndGet();
        if (remaining <= 0 && listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onFinished());
        }
    }

    // Constructor for Widget - STRICTLY followed requirements
    public Data(Context context, AppWidgetManager widgetManager, RemoteViews views, int appWidgetId) {
        this.context = context.getApplicationContext();
        this.widgetManager = widgetManager;
        this.views = views;
        this.appWidgetId = appWidgetId;
        this.adapter = null;
        this.methods = RetrofitClient.getMethods(context);
        
        // 2 requirements: Last reading and 12h Min/Max
        activeCalls.set(2);
        
        fetchOpenTemperatureOnlyForWidget();
        fetchMinMaxTemperature(1, Double.MAX_VALUE, -Double.MAX_VALUE);
    }

    // Constructor for Main App
    public Data(Context context, SensorAdapter adapter, List<SensorConfig> sensorList, DataUpdateListener listener) {
        this.context = context;
        this.adapter = adapter;
        this.listener = listener;
        this.widgetManager = null;
        this.views = null;
        this.appWidgetId = 0;
        this.methods = RetrofitClient.getMethods(context);
        
        SharedPreferences prefs = context.getSharedPreferences("LoggerPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("auth_token", "").trim();

        int totalCalls = 2;
        if (!token.isEmpty() && sensorList != null && !sensorList.isEmpty()) {
            totalCalls++;
            getBulkCustomMeasurements(sensorList);
        }
        activeCalls.set(totalCalls);

        fetchOpenDataBulk();
        fetchMinMaxTemperature(1, Double.MAX_VALUE, -Double.MAX_VALUE);
    }

    private void fetchOpenTemperatureOnlyForWidget() {
        methods.getLastTemprData().enqueue(new Callback<Model>() {
            @Override
            public void onResponse(Call<Model> call, Response<Model> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Model m = response.body();
                    temperature = String.valueOf(m.getValue());
                    created_on = m.getCreated_on();
                    if (created_on != null && created_on.length() >= 16) {
                        created_on = "Μέτρηση: " + created_on.substring(0, 10) + " " + created_on.substring(11, 16);
                    }
                    if (views != null) {
                        views.setTextViewText(R.id.temperature, temperature + " \u2103");
                        views.setTextViewText(R.id.created_on, created_on);
                        widgetManager.updateAppWidget(appWidgetId, views);
                    }
                }
                callFinished();
            }

            @Override
            public void onFailure(Call<Model> call, Throwable t) {
                callFinished();
            }
        });
    }

    private void fetchOpenDataBulk() {
        List<String> coreUuids = Arrays.asList(UUID_TEMP, UUID_HUM, UUID_PRESS);
        methods.getOpenMeasurementsBulk(coreUuids).enqueue(new Callback<ModelList>() {
            @Override
            public void onResponse(Call<ModelList> call, Response<ModelList> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    for (Model m : response.body().getResults()) {
                        if (m.getSensor() == null) continue;
                        String uuid = m.getSensor().getUuid();
                        if (UUID_TEMP.equals(uuid)) {
                            temperature = String.valueOf(m.getValue());
                            created_on = m.getCreated_on();
                            if (created_on != null && created_on.length() >= 16) {
                                created_on = "Μέτρηση: " + created_on.substring(0, 10) + " " + created_on.substring(11, 16);
                            }
                        }
                    }
                }
                callFinished();
            }

            @Override
            public void onFailure(Call<ModelList> call, Throwable t) {
                callFinished();
            }
        });
    }

    private void getBulkCustomMeasurements(final List<SensorConfig> sensorList) {
        List<String> uuids = new ArrayList<>();
        for (SensorConfig s : sensorList) uuids.add(s.getSensorUuid());

        methods.getMeasurementsBulk(uuids).enqueue(new Callback<ModelList>() {
            @Override
            public void onResponse(Call<ModelList> call, Response<ModelList> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    Map<String, Model> sensorMap = new HashMap<>();
                    for (Model m : response.body().getResults()) {
                        if (m.getSensor() != null) sensorMap.put(m.getSensor().getUuid(), m);
                    }

                    String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                    for (SensorConfig config : sensorList) {
                        Model match = sensorMap.get(config.getSensorUuid());
                        if (match != null) {
                            config.setLastValue(String.valueOf(match.getValue()));
                            config.setLastTime(time);
                            config.setKind(match.getSensor().getKind());
                        }
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                } else if (response.code() == 404) {
                    fetchIndividually(sensorList);
                    return; 
                }
                callFinished();
            }

            @Override
            public void onFailure(Call<ModelList> call, Throwable t) {
                callFinished();
            }
        });
    }

    private void fetchIndividually(final List<SensorConfig> sensorList) {
        activeCalls.addAndGet(sensorList.size() - 1);
        for (final SensorConfig config : sensorList) {
            methods.getLastMeasurementBySpace(config.getSpaceUuid(), config.getSensorUuid(), "android").enqueue(new Callback<Model>() {
                @Override
                public void onResponse(Call<Model> call, Response<Model> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        config.setLastValue(String.valueOf(response.body().getValue()));
                        config.setLastTime(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                    callFinished();
                }
                @Override
                public void onFailure(Call<Model> call, Throwable t) {
                    callFinished();
                }
            });
        }
    }

    private void fetchMinMaxTemperature(final int page, final double currentMin, final double currentMax) {
        methods.getTemperature12h(UUID_TEMP, 12, page).enqueue(new Callback<ModelList>() {
            @Override
            public void onResponse(Call<ModelList> call, Response<ModelList> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    double min = currentMin;
                    double max = currentMax;
                    for (Model m : response.body().getResults()) {
                        double val = m.getValue();
                        if (val < min) min = val;
                        if (val > max) max = val;
                    }

                    if (response.body().getNext() != null && !response.body().getNext().isEmpty()) {
                        fetchMinMaxTemperature(page + 1, min, max);
                    } else {
                        applyMinMax(min, max);
                        callFinished();
                    }
                } else {
                    applyMinMax(currentMin, currentMax);
                    callFinished();
                }
            }

            @Override
            public void onFailure(Call<ModelList> call, Throwable t) {
                applyMinMax(currentMin, currentMax);
                callFinished();
            }
        });
    }

    private void applyMinMax(double min, double max) {
        if (min != Double.MAX_VALUE && max != -Double.MAX_VALUE) {
            String text = String.format(Locale.getDefault(), "%.1f", min) + " \u2103  /  " + String.format(Locale.getDefault(), "%.1f", max) + " \u2103";
            if (views != null) {
                views.setTextViewText(R.id.temp_min_max, text);
                widgetManager.updateAppWidget(appWidgetId, views);
            }
        }
    }
}
