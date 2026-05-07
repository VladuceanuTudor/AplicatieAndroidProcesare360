package com.example.aplicatieandroidprocesare360;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.aplicatieandroidprocesare360.api.ApiClient;
import com.example.aplicatieandroidprocesare360.api.ProcessingService;
import com.example.aplicatieandroidprocesare360.api.model.ProcessingJob;
import com.example.aplicatieandroidprocesare360.database.DatabaseHelper;
import com.example.aplicatieandroidprocesare360.model.Panorama;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

public class DetailActivity extends AppCompatActivity {

    private static final int POLL_INTERVAL_MS = 5000;

    private DatabaseHelper   db;
    private Panorama         panorama;
    private Handler          handler      = new Handler(Looper.getMainLooper());
    private Runnable         pollRunnable;
    private boolean          vrModeEnabled = false;

    private android.widget.ImageView imgPreview;
    private TextView   tvTitle, tvStatus, tvDate, tvSource,
                       tvLocation, tvProcTime, tvQuality, tvStatusBadge;
    private ProgressBar progressProcessing;
    private RatingBar   ratingBar;
    private Switch      switchVr;
    private Button      btnOpenVr, btnSendToPipeline, btnViewResult, btnViewMap, btnDelete;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = DatabaseHelper.getInstance(this);

        imgPreview         = findViewById(R.id.img_preview);
        tvTitle            = findViewById(R.id.tv_title);
        tvStatus           = findViewById(R.id.tv_status);
        tvDate             = findViewById(R.id.tv_date);
        tvSource           = findViewById(R.id.tv_source);
        tvLocation         = findViewById(R.id.tv_location);
        tvProcTime         = findViewById(R.id.tv_proc_time);
        tvQuality          = findViewById(R.id.tv_quality);
        tvStatusBadge      = findViewById(R.id.tv_status_badge);
        progressProcessing = findViewById(R.id.progress_processing);
        ratingBar          = findViewById(R.id.rating_bar);
        switchVr           = findViewById(R.id.switch_vr);
        btnOpenVr          = findViewById(R.id.btn_open_vr);
        btnSendToPipeline  = findViewById(R.id.btn_send_to_pipeline);
        btnViewResult      = findViewById(R.id.btn_view_result);
        btnViewMap         = findViewById(R.id.btn_view_map);
        btnDelete          = findViewById(R.id.btn_delete);

        int id = getIntent().getIntExtra("panorama_id", -1);
        if (id == -1) { finish(); return; }

        panorama = db.getPanoramaById(id);
        if (panorama == null) { finish(); return; }

        populateUI();
        setupListeners();

        if (Panorama.STATUS_PROCESSING.equals(panorama.getStatus()) ||
            Panorama.STATUS_UPLOADING.equals(panorama.getStatus())) {
            startPolling();
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────

    private void populateUI() {
        tvTitle.setText(panorama.getTitle() != null ? panorama.getTitle() : "Fără titlu");
        tvDate.setText(sdf.format(new Date(panorama.getUploadDate())));
        tvSource.setText(sourceLabel(panorama.getSourceType()));
        tvStatus.setText(panorama.getStatus());
        tvStatusBadge.setText(panorama.getStatus());
        tvStatusBadge.setBackgroundColor(statusColor(panorama.getStatus()));
        ratingBar.setRating(panorama.getRating());

        if (panorama.hasLocation()) {
            tvLocation.setText(String.format(Locale.US, "%.5f, %.5f",
                    panorama.getLatitude(), panorama.getLongitude()));
        } else {
            tvLocation.setText(getString(R.string.no_location));
        }

        if (panorama.getProcessingTimeMs() > 0)
            tvProcTime.setText(panorama.getProcessingTimeMs() + " ms");

        if (panorama.getQualityScore() > 0)
            tvQuality.setText(String.format(Locale.US, "%.2f", panorama.getQualityScore()));

        boolean isActive = Panorama.STATUS_PROCESSING.equals(panorama.getStatus()) ||
                           Panorama.STATUS_UPLOADING.equals(panorama.getStatus());
        progressProcessing.setVisibility(isActive ? View.VISIBLE : View.GONE);

        // "Trimite la procesare" — vizibil doar pentru PENDING (nu a fost trimis încă)
        boolean canSend = Panorama.STATUS_PENDING.equals(panorama.getStatus()) ||
                          Panorama.STATUS_FAILED.equals(panorama.getStatus());
        btnSendToPipeline.setVisibility(canSend ? View.VISIBLE : View.GONE);

        // "Vezi rezultat" — vizibil doar când procesarea e completă
        btnViewResult.setVisibility(Panorama.STATUS_DONE.equals(panorama.getStatus()) &&
                                    panorama.getResultUrl() != null ? View.VISIBLE : View.GONE);

        String imageUrl = panorama.getResultUrl() != null ? panorama.getResultUrl()
                        : (panorama.getThumbnailUrl() != null ? panorama.getThumbnailUrl()
                        : panorama.getFilePath());
        if (imageUrl != null) {
            Glide.with(this).load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imgPreview);
        }
    }

    private void setupListeners() {
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (fromUser) db.updateRating(panorama.getId(), rating);
        });

        switchVr.setOnCheckedChangeListener((v, checked) -> vrModeEnabled = checked);

        btnOpenVr.setOnClickListener(v -> {
            Intent i = new Intent(this, VRViewerActivity.class);
            i.putExtra("panorama_id", panorama.getId());
            i.putExtra("cardboard_mode", vrModeEnabled);
            startActivity(i);
        });

        btnSendToPipeline.setOnClickListener(v -> sendToPipeline());

        btnViewResult.setOnClickListener(v -> {
            if (panorama.getResultUrl() != null)
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(panorama.getResultUrl())));
        });

        btnViewMap.setOnClickListener(v -> {
            if (!panorama.hasLocation()) {
                Toast.makeText(this, R.string.map_no_location, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, MapActivity.class);
            i.putExtra("focus_lat", panorama.getLatitude());
            i.putExtra("focus_lng", panorama.getLongitude());
            startActivity(i);
        });

        btnDelete.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    db.deletePanorama(panorama.getId());
                    finish();
                })
                .setNegativeButton(R.string.no, null)
                .show()
        );
    }

    // ── Pipeline ─────────────────────────────────────────────────────────

    private void sendToPipeline() {
        String apiUrl = getSharedPreferences("panorama_prefs", MODE_PRIVATE)
                .getString("api_url", "");
        if (apiUrl.isEmpty()) {
            Toast.makeText(this, R.string.error_api_url, Toast.LENGTH_LONG).show();
            return;
        }
        String filePath = panorama.getFilePath();
        if (filePath == null) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            return;
        }

        setProcessingState(true);
        db.updateStatus(panorama.getId(), Panorama.STATUS_UPLOADING);
        panorama.setStatus(Panorama.STATUS_UPLOADING);
        uploadFile(new File(filePath.replace("content://", "")), apiUrl);
    }

    private void uploadFile(File file, String apiUrl) {
        String quality = getSharedPreferences("panorama_prefs", MODE_PRIVATE)
                .getString("default_quality", "medium");

        RequestBody reqFile = RequestBody.create(file, MediaType.parse("image/*"));
        MultipartBody.Part body = MultipartBody.Part.createFormData(
                "file", file.getName(), reqFile);

        ProcessingService service = ApiClient.getProcessingService(apiUrl);
        service.uploadFile(
                body,
                RequestBody.create(quality,  MediaType.parse("text/plain")),
                RequestBody.create("true",   MediaType.parse("text/plain")),
                RequestBody.create("false",  MediaType.parse("text/plain")),
                RequestBody.create("true",   MediaType.parse("text/plain")),
                RequestBody.create("false",  MediaType.parse("text/plain"))
        ).enqueue(new Callback<ProcessingJob>() {
            @Override
            public void onResponse(Call<ProcessingJob> call,
                                   retrofit2.Response<ProcessingJob> r) {
                if (r.isSuccessful() && r.body() != null) {
                    String jobId = r.body().getJobId();
                    panorama.setJobId(jobId);
                    panorama.setStatus(Panorama.STATUS_PROCESSING);
                    db.updatePanorama(panorama);
                    runOnUiThread(() -> {
                        setProcessingState(false);
                        populateUI();
                        Toast.makeText(DetailActivity.this,
                                getString(R.string.success_upload), Toast.LENGTH_SHORT).show();
                        startPolling();
                    });
                } else {
                    handleUploadFailure();
                }
            }

            @Override
            public void onFailure(Call<ProcessingJob> call, Throwable t) {
                handleUploadFailure();
            }
        });
    }

    private void handleUploadFailure() {
        db.updateStatus(panorama.getId(), Panorama.STATUS_FAILED);
        panorama.setStatus(Panorama.STATUS_FAILED);
        runOnUiThread(() -> {
            setProcessingState(false);
            populateUI();
            Toast.makeText(this, R.string.error_network, Toast.LENGTH_LONG).show();
        });
    }

    private void setProcessingState(boolean processing) {
        progressProcessing.setVisibility(processing ? View.VISIBLE : View.GONE);
        btnSendToPipeline.setEnabled(!processing);
        btnSendToPipeline.setText(processing
                ? getString(R.string.uploading)
                : getString(R.string.btn_send_to_pipeline));
    }

    // ── Polling ──────────────────────────────────────────────────────────

    private void startPolling() {
        String apiUrl = getSharedPreferences("panorama_prefs", MODE_PRIVATE)
                .getString("api_url", "");
        if (apiUrl.isEmpty() || panorama.getJobId() == null) return;

        ProcessingService service = ApiClient.getProcessingService(apiUrl);
        pollRunnable = new Runnable() {
            @Override public void run() {
                service.getStatus(panorama.getJobId()).enqueue(new Callback<ProcessingJob>() {
                    @Override
                    public void onResponse(Call<ProcessingJob> call,
                                           retrofit2.Response<ProcessingJob> r) {
                        if (!r.isSuccessful() || r.body() == null) {
                            handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                            return;
                        }
                        ProcessingJob job = r.body();
                        if (job.isDone()) {
                            db.updateJobResult(panorama.getId(), job.getJobId(),
                                    job.getResultUrl(), job.getDepthMapUrl(),
                                    job.getQualityScore(), job.getProcessingTimeMs());
                            db.insertLog(panorama.getId(), "processing_complete",
                                    job.getProcessingTimeMs(), true);
                            panorama = db.getPanoramaById(panorama.getId());
                            runOnUiThread(() -> {
                                populateUI();
                                Toast.makeText(DetailActivity.this,
                                        "Procesare finalizată!", Toast.LENGTH_LONG).show();
                            });
                        } else if (job.isFailed()) {
                            db.updateStatus(panorama.getId(), Panorama.STATUS_FAILED);
                            db.insertLog(panorama.getId(), "processing_failed", 0, false);
                            panorama = db.getPanoramaById(panorama.getId());
                            runOnUiThread(() -> populateUI());
                        } else {
                            handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                        }
                    }
                    @Override
                    public void onFailure(Call<ProcessingJob> call, Throwable t) {
                        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                    }
                });
            }
        };
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollRunnable != null) handler.removeCallbacks(pollRunnable);
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String sourceLabel(String src) {
        if (src == null) return getString(R.string.source_local);
        switch (src) {
            case Panorama.SOURCE_STREETVIEW: return getString(R.string.source_streetview);
            default:                          return getString(R.string.source_local);
        }
    }

    private int statusColor(String status) {
        if (status == null) return getColor(R.color.status_pending);
        switch (status) {
            case Panorama.STATUS_DONE:       return getColor(R.color.status_done);
            case Panorama.STATUS_FAILED:     return getColor(R.color.status_failed);
            case Panorama.STATUS_PROCESSING: return getColor(R.color.status_processing);
            case Panorama.STATUS_UPLOADING:  return getColor(R.color.status_uploading);
            default:                          return getColor(R.color.status_pending);
        }
    }
}
