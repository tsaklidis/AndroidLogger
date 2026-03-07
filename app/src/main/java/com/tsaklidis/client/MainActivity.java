package com.tsaklidis.client;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Button getData;
    private ArrayList<String> arrayList;
    private ArrayAdapter<String> adapter;
    private ListView list;
    private Data data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide the default action bar — we have our own header
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        getData = findViewById(R.id.latest);
        arrayList = new ArrayList<String>();
        list = findViewById(R.id.response);

        // Use custom card-style list item layout
        adapter = new ArrayAdapter<String>(this, R.layout.list_item_measurement, arrayList);

        list.setAdapter(adapter);

        data = new Data(adapter, arrayList);
        adapter.clear();

        getData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.clear();
                data = new Data(adapter, arrayList);
            }
        });
    }
}