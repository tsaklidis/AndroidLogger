package com.tsaklidis.client;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.ViewHolder> {

    private List<SensorConfig> allSensors;
    private List<SensorConfig> filteredSensors;
    private final OnSensorActionListener listener;

    public interface OnSensorActionListener {
        void onEdit(SensorConfig sensor);
        void onDelete(SensorConfig sensor);
        void onToggleHide(SensorConfig sensor);
        void onClick(SensorConfig sensor);
    }

    public SensorAdapter(List<SensorConfig> sensors, OnSensorActionListener listener) {
        this.allSensors = sensors;
        this.filteredSensors = new ArrayList<>(sensors);
        sortSensors(this.filteredSensors);
        this.listener = listener;
    }

    public void updateData(List<SensorConfig> sensors) {
        this.allSensors = sensors;
        this.filteredSensors = new ArrayList<>(sensors);
        sortSensors(this.filteredSensors);
        notifyDataSetChanged();
    }

    private void sortSensors(List<SensorConfig> list) {
        Collections.sort(list, (s1, s2) -> {
            if (s1.isHidden() && !s2.isHidden()) return 1;
            if (!s1.isHidden() && s2.isHidden()) return -1;
            return 0;
        });
    }

    public void filter(String query, String selectedKind) {
        filteredSensors.clear();
        for (SensorConfig sensor : allSensors) {
            boolean matchesQuery = query == null || query.isEmpty() || 
                    sensor.getName().toLowerCase().contains(query.toLowerCase());
            
            boolean matchesKind = selectedKind == null || selectedKind.isEmpty() || 
                    (sensor.getKind() != null && sensor.getKind().equalsIgnoreCase(selectedKind));

            if (matchesQuery && matchesKind) {
                filteredSensors.add(sensor);
            }
        }
        sortSensors(filteredSensors);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sensor_grid_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SensorConfig sensor = filteredSensors.get(position);
        holder.name.setText(sensor.getName());
        holder.value.setText(sensor.getLastValue());
        holder.time.setText("Last updated: " + sensor.getLastTime());

        if (sensor.isHidden()) {
            holder.itemRoot.setAlpha(0.5f);
            holder.btnHide.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            holder.itemRoot.setAlpha(1.0f);
            holder.btnHide.setImageResource(android.R.drawable.ic_menu_view);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(sensor));
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(sensor));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(sensor));
        holder.btnHide.setOnClickListener(v -> listener.onToggleHide(sensor));
    }

    @Override
    public int getItemCount() {
        return filteredSensors.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, value, time;
        ImageButton btnEdit, btnDelete, btnHide;
        View itemRoot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemRoot = itemView.findViewById(R.id.item_root);
            name = itemView.findViewById(R.id.sensor_name);
            value = itemView.findViewById(R.id.sensor_value);
            time = itemView.findViewById(R.id.sensor_time);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnHide = itemView.findViewById(R.id.btn_hide);
        }
    }
}
