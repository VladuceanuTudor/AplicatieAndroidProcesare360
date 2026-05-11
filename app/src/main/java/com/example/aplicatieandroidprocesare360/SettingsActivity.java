package com.example.aplicatieandroidprocesare360;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.aplicatieandroidprocesare360.api.ApiClient;
import com.example.aplicatieandroidprocesare360.api.ProcessingService;
import com.example.aplicatieandroidprocesare360.api.model.LoginRequest;
import com.example.aplicatieandroidprocesare360.api.model.LoginResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private EditText  etApiUrl, etUsername, etPassword;
    private Spinner   spinnerDefaultQuality;
    private RadioButton rbList, rbGrid;
    private SeekBar   seekBarIpd;
    private TextView  tvIpdLabel;
    private CalendarView calendarView;
    private DatePicker   datePicker;
    private Button    btnClearCache, btnSave;

    private SharedPreferences prefs;

    private static final String[] QUALITY_LABELS = {"Scăzută", "Medie", "Înaltă", "Ultra"};
    private static final String[] QUALITY_VALUES = {"low", "medium", "high", "ultra"};

    private static final int IPD_MIN_MM = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = getSharedPreferences("panorama_prefs", MODE_PRIVATE);

        etApiUrl             = findViewById(R.id.et_api_url);
        etUsername           = findViewById(R.id.et_username);
        etPassword           = findViewById(R.id.et_password);
        spinnerDefaultQuality= findViewById(R.id.spinner_default_quality);
        rbList               = findViewById(R.id.rb_list);
        rbGrid               = findViewById(R.id.rb_grid);
        seekBarIpd           = findViewById(R.id.seekbar_ipd);
        tvIpdLabel           = findViewById(R.id.tv_ipd_label);
        calendarView         = findViewById(R.id.calendar_view);
        datePicker           = findViewById(R.id.date_picker);
        btnClearCache        = findViewById(R.id.btn_clear_cache);
        btnSave              = findViewById(R.id.btn_save);

        ArrayAdapter<String> qualityAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, QUALITY_LABELS);
        qualityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerDefaultQuality.setAdapter(qualityAdapter);

        loadPreferences();

        seekBarIpd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int ipdMm = IPD_MIN_MM + progress;
                tvIpdLabel.setText(getString(R.string.settings_ipd, ipdMm));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        calendarView.setBackgroundColor(getColor(R.color.surface_dark));

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selected = String.format(Locale.getDefault(), "%02d.%02d.%d",
                    dayOfMonth, month + 1, year);
            Toast.makeText(this, "Dată selectată: " + selected, Toast.LENGTH_SHORT).show();
        });

        btnClearCache.setOnClickListener(v -> {
            Glide.get(this).clearMemory();
            new Thread(() -> Glide.get(this).clearDiskCache()).start();
            Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> savePreferences());
    }

    private void loadPreferences() {
        etApiUrl.setText(prefs.getString("api_url", ""));
        etUsername.setText(prefs.getString("api_username", ""));
        // Password is intentionally not pre-filled; re-enter to change or re-authenticate.

        String quality = prefs.getString("default_quality", "medium");
        for (int i = 0; i < QUALITY_VALUES.length; i++) {
            if (QUALITY_VALUES[i].equals(quality)) {
                spinnerDefaultQuality.setSelection(i);
                break;
            }
        }

        boolean isGrid = prefs.getBoolean("display_grid", false);
        rbList.setChecked(!isGrid);
        rbGrid.setChecked(isGrid);

        int ipdMm = prefs.getInt("ipd_mm", 65);
        int progress = Math.max(0, ipdMm - IPD_MIN_MM);
        seekBarIpd.setProgress(progress);
        tvIpdLabel.setText(getString(R.string.settings_ipd, ipdMm));

        int filterYear  = prefs.getInt("filter_year",  -1);
        int filterMonth = prefs.getInt("filter_month", -1);
        int filterDay   = prefs.getInt("filter_day",   -1);
        if (filterYear != -1) {
            datePicker.updateDate(filterYear, filterMonth, filterDay);
        }
    }

    private void savePreferences() {
        String apiUrl  = etApiUrl.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (!apiUrl.isEmpty()) {
            // Normalize: prepend http:// if missing, then ensure /api/ suffix
            if (!apiUrl.startsWith("http")) apiUrl = "http://" + apiUrl;
            apiUrl = apiUrl.replaceAll("/+$", "");
            if (!apiUrl.endsWith("/api")) {
                try {
                    java.net.URL parsed = new java.net.URL(apiUrl);
                    apiUrl = parsed.getProtocol() + "://" + parsed.getAuthority() + "/api";
                } catch (Exception ignored) {
                    apiUrl = apiUrl + "/api";
                }
            }
            apiUrl = apiUrl + "/";
        }

        int ipdMm = IPD_MIN_MM + seekBarIpd.getProgress();

        prefs.edit()
                .putString("api_url",          apiUrl)
                .putString("api_username",      username)
                .putString("default_quality",   QUALITY_VALUES[spinnerDefaultQuality.getSelectedItemPosition()])
                .putBoolean("display_grid",      rbGrid.isChecked())
                .putInt("ipd_mm",                ipdMm)
                .putInt("filter_year",           datePicker.getYear())
                .putInt("filter_month",          datePicker.getMonth())
                .putInt("filter_day",            datePicker.getDayOfMonth())
                .apply();

        // Show the normalized URL back in the field so the user can confirm it
        etApiUrl.setText(apiUrl);

        ApiClient.resetProcessingClient();
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();

        if (!apiUrl.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
            attemptLogin(apiUrl, username, password);
        }
    }

    private void attemptLogin(String apiUrl, String username, String password) {
        Toast.makeText(this, R.string.settings_connecting, Toast.LENGTH_SHORT).show();
        ProcessingService service = ApiClient.getProcessingService(apiUrl);
        service.login(new LoginRequest(username, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            ApiClient.saveToken(SettingsActivity.this, r.body().getAccessToken());
                            runOnUiThread(() -> Toast.makeText(SettingsActivity.this,
                                    R.string.settings_login_success, Toast.LENGTH_LONG).show());
                        } else {
                            runOnUiThread(() -> Toast.makeText(SettingsActivity.this,
                                    R.string.settings_login_failed, Toast.LENGTH_LONG).show());
                        }
                    }
                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        runOnUiThread(() -> Toast.makeText(SettingsActivity.this,
                                R.string.error_network, Toast.LENGTH_LONG).show());
                    }
                });
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
