package com.example.aplicatieandroidprocesare360;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.aplicatieandroidprocesare360.database.DatabaseHelper;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = DatabaseHelper.getInstance(this);
        com.example.aplicatieandroidprocesare360.api.ApiClient.loadToken(this);

        findViewById(R.id.nav_library).setOnClickListener(v ->
                startActivity(new Intent(this, LibraryActivity.class)));
        findViewById(R.id.nav_upload).setOnClickListener(v ->
                startActivity(new Intent(this, UploadActivity.class)));
        findViewById(R.id.nav_map).setOnClickListener(v ->
                startActivity(new Intent(this, MapActivity.class)));
        findViewById(R.id.nav_stats).setOnClickListener(v ->
                startActivity(new Intent(this, StatsActivity.class)));
        findViewById(R.id.nav_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStats();
    }

    private void refreshStats() {
        ((TextView) findViewById(R.id.tv_total_count))
                .setText(String.valueOf(db.countAll()));
        ((TextView) findViewById(R.id.tv_processed_count))
                .setText(String.valueOf(db.countDone()));
        ((TextView) findViewById(R.id.tv_failed_count))
                .setText(String.valueOf(db.countFailed()));
    }
}
