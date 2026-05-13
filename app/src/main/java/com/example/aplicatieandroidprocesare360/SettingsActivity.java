package com.example.aplicatieandroidprocesare360;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

public class SettingsActivity extends AppCompatActivity {

    private Spinner     spinnerDefaultQuality;
    private RadioButton rbList, rbGrid;
    private SeekBar     seekBarIpd;
    private TextView    tvIpdLabel;
    private DatePicker  datePicker;
    private Button      btnClearCache, btnSave;

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

        spinnerDefaultQuality = findViewById(R.id.spinner_default_quality);
        rbList                = findViewById(R.id.rb_list);
        rbGrid                = findViewById(R.id.rb_grid);
        seekBarIpd            = findViewById(R.id.seekbar_ipd);
        tvIpdLabel            = findViewById(R.id.tv_ipd_label);
        datePicker            = findViewById(R.id.date_picker);
        btnClearCache         = findViewById(R.id.btn_clear_cache);
        btnSave               = findViewById(R.id.btn_save);

        ArrayAdapter<String> qualityAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, QUALITY_LABELS);
        qualityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerDefaultQuality.setAdapter(qualityAdapter);

        loadPreferences();

        seekBarIpd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                tvIpdLabel.setText(getString(R.string.settings_ipd, IPD_MIN_MM + progress));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        btnClearCache.setOnClickListener(v -> {
            Glide.get(this).clearMemory();
            new Thread(() -> Glide.get(this).clearDiskCache()).start();
            Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> savePreferences());
    }

    private void loadPreferences() {
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
        seekBarIpd.setProgress(Math.max(0, ipdMm - IPD_MIN_MM));
        tvIpdLabel.setText(getString(R.string.settings_ipd, ipdMm));

        int filterYear  = prefs.getInt("filter_year",  -1);
        int filterMonth = prefs.getInt("filter_month", -1);
        int filterDay   = prefs.getInt("filter_day",   -1);
        if (filterYear != -1) {
            datePicker.updateDate(filterYear, filterMonth, filterDay);
        }
    }

    private void savePreferences() {
        int ipdMm = IPD_MIN_MM + seekBarIpd.getProgress();

        prefs.edit()
                .putString("default_quality",   QUALITY_VALUES[spinnerDefaultQuality.getSelectedItemPosition()])
                .putBoolean("display_grid",      rbGrid.isChecked())
                .putInt("ipd_mm",                ipdMm)
                .putInt("filter_year",           datePicker.getYear())
                .putInt("filter_month",          datePicker.getMonth())
                .putInt("filter_day",            datePicker.getDayOfMonth())
                .apply();

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
