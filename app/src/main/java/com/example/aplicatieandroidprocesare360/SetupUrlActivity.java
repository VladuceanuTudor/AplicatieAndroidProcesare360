package com.example.aplicatieandroidprocesare360;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicatieandroidprocesare360.api.ApiClient;

public class SetupUrlActivity extends AppCompatActivity {

    private EditText etServerUrl;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_url);

        prefs = getSharedPreferences("panorama_prefs", MODE_PRIVATE);
        etServerUrl = findViewById(R.id.et_server_url);
        Button btnNext = findViewById(R.id.btn_next);

        etServerUrl.setText(prefs.getString("api_url", ""));

        btnNext.setOnClickListener(v -> {
            String url = etServerUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, R.string.error_url_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            String normalized = ApiClient.normalizeApiUrl(url);
            prefs.edit().putString("api_url", normalized).apply();
            ApiClient.resetProcessingClient();

            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
