package com.example.genzmusicapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class HealthConnectRationaleActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_health_rationale);
        findViewById(R.id.closeRationaleButton).setOnClickListener(view -> finish());
    }
}
