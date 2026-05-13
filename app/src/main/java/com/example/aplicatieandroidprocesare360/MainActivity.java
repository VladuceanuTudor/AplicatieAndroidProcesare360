package com.example.aplicatieandroidprocesare360;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.aplicatieandroidprocesare360.api.ApiClient;
import com.example.aplicatieandroidprocesare360.database.DatabaseHelper;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ApiClient.loadToken(this);

        SharedPreferences prefs = getSharedPreferences("panorama_prefs", MODE_PRIVATE);
        String apiUrl = prefs.getString("api_url", "");

        if (apiUrl.isEmpty()) {
            startActivity(new Intent(this, SetupUrlActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return;
        }

        if (ApiClient.getAuthToken() == null) {
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return;
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = DatabaseHelper.getInstance(this);

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
        findViewById(R.id.nav_logout).setOnClickListener(v -> {
            ApiClient.clearToken(this);
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) refreshStats();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            ApiClient.clearToken(this);
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return true;
        }
        if (id == R.id.action_change_server) {
            ApiClient.clearToken(this);
            getSharedPreferences("panorama_prefs", MODE_PRIVATE)
                    .edit().remove("api_url").apply();
            ApiClient.resetProcessingClient();
            startActivity(new Intent(this, SetupUrlActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return true;
        }
        return super.onOptionsItemSelected(item);
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
