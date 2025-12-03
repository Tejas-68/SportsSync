package com.project.sportssync;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuickBorrowConfigActivity extends AppCompatActivity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Spinner spinnerSports, spinnerEquipment;
    private Button btnAddWidget;
    private FirebaseFirestore db;
    
    private List<SportModel> sportsList = new ArrayList<>();
    private List<String> equipmentList = new ArrayList<>();
    private List<String> equipmentKeys = new ArrayList<>();
    
    private SportModel selectedSport;
    private String selectedEquipmentKey;
    private String selectedEquipmentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_config);

        // Set result to CANCELED. If the user backs out, the widget is not added
        setResult(RESULT_CANCELED);

        // Find the widget ID from the intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        spinnerSports = findViewById(R.id.spinner_sports);
        spinnerEquipment = findViewById(R.id.spinner_equipment);
        btnAddWidget = findViewById(R.id.btn_add_widget);
        
        db = FirebaseFirestore.getInstance();
        
        loadSports();
        
        btnAddWidget.setOnClickListener(v -> createWidget());
    }

    private void loadSports() {
        db.collection("sports").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> sportNames = new ArrayList<>();
                    sportsList.clear();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        SportModel sport = doc.toObject(SportModel.class);
                        sport.setId(doc.getId());
                        sportsList.add(sport);
                        sportNames.add(sport.getName());
                    }
                    
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                            android.R.layout.simple_spinner_item, sportNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSports.setAdapter(adapter);
                    
                    spinnerSports.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedSport = sportsList.get(position);
                            loadEquipment(selectedSport);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load sports", Toast.LENGTH_SHORT).show());
    }

    private void loadEquipment(SportModel sport) {
        equipmentList.clear();
        equipmentKeys.clear();
        
        if (sport.getEquipment() != null) {
            for (Map.Entry<String, SportModel.EquipmentItem> entry : sport.getEquipment().entrySet()) {
                SportModel.EquipmentItem itemData = entry.getValue();
                String name = itemData.getName();
                if (name != null) {
                    equipmentList.add(name);
                    equipmentKeys.add(entry.getKey());
                }
            }
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, equipmentList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEquipment.setAdapter(adapter);
        
        spinnerEquipment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedEquipmentName = equipmentList.get(position);
                selectedEquipmentKey = equipmentKeys.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void createWidget() {
        if (selectedSport == null || selectedEquipmentKey == null) {
            Toast.makeText(this, "Please select sport and equipment", Toast.LENGTH_SHORT).show();
            return;
        }

        Context context = QuickBorrowConfigActivity.this;
        
        // Save configuration
        SharedPreferences.Editor prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE).edit();
        prefs.putString("widget_" + appWidgetId + "_sportId", selectedSport.getId());
        prefs.putString("widget_" + appWidgetId + "_sport", selectedSport.getName());
        prefs.putString("widget_" + appWidgetId + "_equipmentKey", selectedEquipmentKey);
        prefs.putString("widget_" + appWidgetId + "_equipment", selectedEquipmentName);
        prefs.apply();

        // Update the widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        QuickBorrowWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId);

        // Return success
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
