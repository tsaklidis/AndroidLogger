package com.tsaklidis.client;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SensorAdapter.OnSensorActionListener {

    private FloatingActionButton btnRefresh;
    private List<SensorConfig> sensorList;
    private SensorAdapter adapter;
    private EditText tokenInput;
    private EditText searchInput;
    private ChipGroup kindChipGroup;
    private ChipGroup spaceChipGroup;
    private SharedPreferences prefs;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private final Gson gson = new Gson();
    
    private final Set<String> selectedKinds = new HashSet<>();
    private final Set<String> selectedSpaces = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        prefs = getSharedPreferences("LoggerPrefs", Context.MODE_PRIVATE);
        tokenInput = findViewById(R.id.token_input);
        searchInput = findViewById(R.id.search_input);
        kindChipGroup = findViewById(R.id.kind_chip_group);
        spaceChipGroup = findViewById(R.id.space_chip_group);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        emptyView = findViewById(R.id.empty_view);
        RecyclerView recyclerView = findViewById(R.id.sensors_recycler_view);
        btnRefresh = findViewById(R.id.latest);

        findViewById(R.id.btn_add_sensor).setOnClickListener(v -> showSensorDialog(null));
        findViewById(R.id.btn_sync_account).setOnClickListener(v -> syncAccountSensors());
        findViewById(R.id.btn_delete_all).setOnClickListener(v -> deleteAllSensors());

        // Load token
        tokenInput.setText(prefs.getString("auth_token", ""));
        tokenInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                prefs.edit().putString("auth_token", s.toString().trim()).apply();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Search filter
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Pull to refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        }

        // Load sensors
        loadSensors();

        adapter = new SensorAdapter(sensorList, this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        updateFilterChips();
        refreshData(); // Auto-refresh on startup

        btnRefresh.setOnClickListener(v -> refreshData());
    }

    private void deleteAllSensors() {
        if (sensorList.isEmpty()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete All Sensors")
                .setMessage("Are you sure you want to remove all " + sensorList.size() + " sensors?")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    sensorList.clear();
                    saveSensors();
                    selectedKinds.clear();
                    selectedSpaces.clear();
                    updateFilterChips();
                    adapter.updateData(sensorList);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateFilterChips() {
        kindChipGroup.removeAllViews();
        Set<String> kinds = new HashSet<>();
        for (SensorConfig sensor : sensorList) {
            if (sensor.getKind() != null && !sensor.getKind().isEmpty()) kinds.add(sensor.getKind());
        }
        for (String kind : kinds) {
            Chip c = new Chip(this);
            c.setText(kind);
            c.setCheckable(true);
            if (selectedKinds.contains(kind)) c.setChecked(true);
            c.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedKinds.add(kind);
                else selectedKinds.remove(kind);
                applyFilters();
            });
            kindChipGroup.addView(c);
        }

        spaceChipGroup.removeAllViews();
        Set<String> spaces = new HashSet<>();
        for (SensorConfig sensor : sensorList) {
            if (sensor.getSpaceName() != null && !sensor.getSpaceName().isEmpty()) spaces.add(sensor.getSpaceName());
        }
        for (String space : spaces) {
            Chip c = new Chip(this);
            c.setText(space);
            c.setCheckable(true);
            if (selectedSpaces.contains(space)) c.setChecked(true);
            c.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedSpaces.add(space);
                else selectedSpaces.remove(space);
                applyFilters();
            });
            spaceChipGroup.addView(c);
        }
    }

    private void applyFilters() {
        String query = searchInput.getText().toString().toLowerCase();
        List<SensorConfig> filtered = new ArrayList<>();
        
        for (SensorConfig sensor : sensorList) {
            boolean matchesQuery = query.isEmpty() || sensor.getName().toLowerCase().contains(query);
            boolean matchesKind = selectedKinds.isEmpty() || selectedKinds.contains(sensor.getKind());
            boolean matchesSpace = selectedSpaces.isEmpty() || selectedSpaces.contains(sensor.getSpaceName());

            if (matchesQuery && matchesKind && matchesSpace) {
                filtered.add(sensor);
            }
        }
        adapter.updateData(filtered);
        
        if (filtered.isEmpty() && !sensorList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    private void syncAccountSensors() {
        String token = tokenInput.getText().toString().trim();
        if (token.isEmpty()) {
            return;
        }

        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
        
        Methods methods = RetrofitClient.getMethods(this);
        methods.getMyHouses().enqueue(new Callback<List<HouseModel>>() {
            @Override
            public void onResponse(Call<List<HouseModel>> call, Response<List<HouseModel>> response) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<HouseModel> houses = response.body();
                    List<SensorConfig> newSensors = new ArrayList<>();
                    for (HouseModel house : houses) {
                        if (house.getSpaces() != null) {
                            for (HouseModel.Space space : house.getSpaces()) {
                                if (space.getSensors() != null) {
                                    for (HouseModel.Sensor sensor : space.getSensors()) {
                                        String displayName = house.getName() + ": " + space.getName() + " (" + sensor.getName() + ")";
                                        SensorConfig config = new SensorConfig(displayName, space.getUuid(), sensor.getUuid());
                                        config.setKind(sensor.getKind());
                                        config.setSpaceName(space.getName());
                                        newSensors.add(config);
                                    }
                                }
                            }
                        }
                    }
                    if (!newSensors.isEmpty()) {
                        sensorList.clear();
                        sensorList.addAll(newSensors);
                        saveSensors();
                        selectedKinds.clear();
                        selectedSpaces.clear();
                        updateFilterChips();
                        adapter.updateData(sensorList);
                        // refreshData(); // Removed auto-refresh after sync
                    }
                }
            }
            @Override
            public void onFailure(Call<List<HouseModel>> call, Throwable t) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void loadSensors() {
        String json = prefs.getString("sensors_json", "");
        if (json.isEmpty()) {
            sensorList = new ArrayList<>();
            sensorList.add(new SensorConfig("Living Room", "f3f279d9", "369883d4"));
            saveSensors();
        } else {
            sensorList = gson.fromJson(json, new TypeToken<List<SensorConfig>>() {}.getType());
        }
    }

    private void saveSensors() {
        prefs.edit().putString("sensors_json", gson.toJson(sensorList)).apply();
    }

    private void showSensorDialog(SensorConfig existing) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_sensor, null);
        EditText nameInput = dialogView.findViewById(R.id.edit_name);
        EditText spaceInput = dialogView.findViewById(R.id.edit_space_uuid);
        EditText sensorInput = dialogView.findViewById(R.id.edit_sensor_uuid);

        if (existing != null) {
            nameInput.setText(existing.getName());
            spaceInput.setText(existing.getSpaceUuid());
            sensorInput.setText(existing.getSensorUuid());
        }

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add Sensor" : "Edit Sensor")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String space = spaceInput.getText().toString().trim();
                    String sensor = sensorInput.getText().toString().trim();
                    if (name.isEmpty() || space.isEmpty() || sensor.isEmpty()) return;

                    if (existing == null) {
                        sensorList.add(new SensorConfig(name, space, sensor));
                    } else {
                        existing.setName(name);
                        existing.setSpaceUuid(space);
                        existing.setSensorUuid(sensor);
                    }
                    saveSensors();
                    updateFilterChips();
                    applyFilters();
                    // refreshData(); // Removed auto-refresh after save
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEdit(SensorConfig sensor) { showSensorDialog(sensor); }

    @Override
    public void onDelete(SensorConfig sensor) {
        sensorList.remove(sensor);
        saveSensors();
        updateFilterChips();
        applyFilters();
    }

    @Override
    public void onToggleHide(SensorConfig sensor) {
        sensor.setHidden(!sensor.isHidden());
        saveSensors();
        adapter.updateData(sensorList);
    }

    @Override
    public void onClick(SensorConfig sensor) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_sensor_details, null);
        
        ((TextView) view.findViewById(R.id.detail_name)).setText(sensor.getName());
        ((TextView) view.findViewById(R.id.detail_value)).setText(sensor.getLastValue());
        ((TextView) view.findViewById(R.id.detail_time)).setText("Last updated: " + sensor.getLastTime());
        ((TextView) view.findViewById(R.id.info_kind)).setText(sensor.getKind());
        ((TextView) view.findViewById(R.id.info_space)).setText(sensor.getSpaceName());
        ((TextView) view.findViewById(R.id.info_space_uuid)).setText(sensor.getSpaceUuid());
        ((TextView) view.findViewById(R.id.info_sensor_uuid)).setText(sensor.getSensorUuid());

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Close", null)
                .show();
    }

    private void refreshData() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
        btnRefresh.setEnabled(false);
        
        new Data(this, adapter, sensorList, new Data.DataUpdateListener() {
            @Override
            public void onFinished() {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                btnRefresh.setEnabled(true);
                updateFilterChips();
                applyFilters();
                updateWidget(); // Trigger widget update when manual refresh is done
            }
            @Override
            public void onError(String message) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                btnRefresh.setEnabled(true);
            }
        });
    }

    private void updateWidget() {
        Intent intent = new Intent(this, Logger.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new ComponentName(getApplication(), Logger.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }
}
