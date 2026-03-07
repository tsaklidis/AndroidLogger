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

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
    private ProgressBar progressBar;
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
        progressBar = findViewById(R.id.loading_progress);
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
                updateWidgets();
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

        // Load sensors
        loadSensors();

        adapter = new SensorAdapter(sensorList, this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        updateFilterChips();
        refreshData();

        btnRefresh.setOnClickListener(v -> refreshData());
    }

    private void deleteAllSensors() {
        if (sensorList.isEmpty()) {
            Toast.makeText(this, "No sensors to delete", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "All sensors deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateFilterChips() {
        // Update Kind Chips
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

        // Update Space Chips
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
    }

    private void syncAccountSensors() {
        String token = tokenInput.getText().toString().trim();
        if (token.isEmpty()) {
            Toast.makeText(this, "Please enter a token first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Syncing sensors...", Toast.LENGTH_SHORT).show();
        
        Methods methods = RetrofitClient.getMethods(this);
        methods.getMyHouses().enqueue(new Callback<List<HouseModel>>() {
            @Override
            public void onResponse(Call<List<HouseModel>> call, Response<List<HouseModel>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<HouseModel> houses = response.body();
                    List<SensorConfig> newSensors = new ArrayList<>();
                    for (HouseModel house : houses) {
                        if (house.getSpaces() != null) {
                            for (HouseModel.Space space : house.getSpaces()) {
                                if (space.getSensors() != null) {
                                    for (HouseModel.Sensor sensor : space.getSensors()) {
                                        SensorConfig config = new SensorConfig(sensor.getName(), space.getUuid(), sensor.getUuid());
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
                        refreshData();
                        Toast.makeText(MainActivity.this, "Synced " + newSensors.size() + " sensors", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<List<HouseModel>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadSensors() {
        String json = prefs.getString("sensors_json", "");
        if (json.isEmpty()) {
            sensorList = new ArrayList<>();
            sensorList.add(new SensorConfig("Example Sensor", "f3f279d9", "369883d4"));
            saveSensors();
        } else {
            sensorList = gson.fromJson(json, new TypeToken<List<SensorConfig>>() {}.getType());
        }
    }

    private void saveSensors() {
        prefs.edit().putString("sensors_json", gson.toJson(sensorList)).apply();
        updateWidgets();
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
                    refreshData();
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
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(SensorConfig sensor) {
        String details = "Name: " + sensor.getName() + "\nSpace: " + sensor.getSpaceName() + "\nKind: " + sensor.getKind() + 
                        "\nValue: " + sensor.getLastValue() + "\nTime: " + sensor.getLastTime();
        new AlertDialog.Builder(this).setTitle("Sensor Details").setMessage(details).setPositiveButton("Close", null).show();
    }

    private void updateWidgets() {
        Intent intent = new Intent(this, Logger.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), Logger.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void refreshData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnRefresh.setEnabled(false);
        Toast.makeText(this, "Updating sensors...", Toast.LENGTH_SHORT).show();
        
        new Data(this, adapter, sensorList, new Data.DataUpdateListener() {
            @Override
            public void onFinished() {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnRefresh.setEnabled(true);
                updateFilterChips();
                applyFilters();
                Toast.makeText(MainActivity.this, "Sensors updated", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String message) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnRefresh.setEnabled(true);
            }
        });
    }
}
