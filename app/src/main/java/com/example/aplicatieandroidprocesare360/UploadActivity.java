package com.example.aplicatieandroidprocesare360;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.aplicatieandroidprocesare360.api.ApiClient;
import com.example.aplicatieandroidprocesare360.api.ProcessingService;
import com.example.aplicatieandroidprocesare360.api.model.JobCreateResponse;
import com.example.aplicatieandroidprocesare360.database.DatabaseHelper;
import com.example.aplicatieandroidprocesare360.model.Panorama;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1001;

    private ImageView    imgPreview;
    private View         tvNoFile;
    private Switch       switchAutoGps;
    private View         layoutManualGps;
    private android.widget.EditText etTitle, etDescription, etLat, etLng;
    private Spinner      spinnerQuality;
    private CheckBox     cbDepth, cbMesh, cbColor, cbHdr;
    private ProgressBar  progressUpload;
    private TextView     tvUploadStatus;
    private Button       btnSelectFile, btnUpload;

    private Uri              selectedFileUri;
    private DatabaseHelper   db;
    private SharedPreferences prefs;
    private FusedLocationProviderClient locationClient;

    private final String[] QUALITY_LABELS = {"Scăzută (rapid)", "Medie", "Înaltă", "Ultra (lent)"};
    private final String[] QUALITY_VALUES = {"low", "medium", "high", "ultra"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db             = DatabaseHelper.getInstance(this);
        prefs          = getSharedPreferences("panorama_prefs", MODE_PRIVATE);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        bindViews();
        setupQualitySpinner();
        setupListeners();
    }

    private void bindViews() {
        imgPreview      = findViewById(R.id.img_preview);
        tvNoFile        = findViewById(R.id.tv_no_file);
        switchAutoGps   = findViewById(R.id.switch_auto_gps);
        layoutManualGps = findViewById(R.id.layout_manual_gps);
        etTitle         = findViewById(R.id.et_title);
        etDescription   = findViewById(R.id.et_description);
        etLat           = findViewById(R.id.et_latitude);
        etLng           = findViewById(R.id.et_longitude);
        spinnerQuality  = findViewById(R.id.spinner_quality);
        cbDepth         = findViewById(R.id.cb_depth);
        cbMesh          = findViewById(R.id.cb_mesh);
        cbColor         = findViewById(R.id.cb_color);
        cbHdr           = findViewById(R.id.cb_hdr);
        progressUpload  = findViewById(R.id.progress_upload);
        tvUploadStatus  = findViewById(R.id.tv_upload_status);
        btnSelectFile   = findViewById(R.id.btn_select_file);
        btnUpload       = findViewById(R.id.btn_upload);
    }

    private void setupQualitySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, QUALITY_LABELS);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerQuality.setAdapter(adapter);

        String defaultQuality = prefs.getString("default_quality", "medium");
        for (int i = 0; i < QUALITY_VALUES.length; i++) {
            if (QUALITY_VALUES[i].equals(defaultQuality)) {
                spinnerQuality.setSelection(i);
                break;
            }
        }
    }

    private void setupListeners() {
        btnSelectFile.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            startActivityForResult(Intent.createChooser(i, "Selectează fișier"), PICK_FILE_REQUEST);
        });

        switchAutoGps.setOnCheckedChangeListener((v, checked) ->
                layoutManualGps.setVisibility(checked ? View.GONE : View.VISIBLE));

        btnUpload.setOnClickListener(v -> startUpload());
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_FILE_REQUEST && res == Activity.RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            tvNoFile.setVisibility(View.GONE);
            imgPreview.setVisibility(View.VISIBLE);
            Glide.with(this).load(selectedFileUri).centerCrop().into(imgPreview);
        }
    }

    private void startUpload() {
        String apiUrl = prefs.getString("api_url", "");
        if (apiUrl.isEmpty()) {
            Toast.makeText(this, R.string.error_api_url, Toast.LENGTH_LONG).show();
            return;
        }
        if (selectedFileUri == null) {
            Toast.makeText(this, R.string.no_file_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) title = "Panoramă " + System.currentTimeMillis();

        Panorama p = new Panorama();
        p.setTitle(title);
        p.setDescription(etDescription.getText().toString().trim());
        p.setFilePath(selectedFileUri.toString());
        p.setSourceType(Panorama.SOURCE_LOCAL);
        p.setStatus(Panorama.STATUS_UPLOADING);

        if (!switchAutoGps.isChecked()) {
            try {
                p.setLatitude(Double.parseDouble(etLat.getText().toString()));
                p.setLongitude(Double.parseDouble(etLng.getText().toString()));
            } catch (NumberFormatException ignored) {}
        }

        long id = db.insertPanorama(p);
        p.setId((int) id);

        setUploading(true);

        if (switchAutoGps.isChecked()) {
            try {
                locationClient.getLastLocation().addOnSuccessListener(loc -> {
                    if (loc != null) {
                        p.setLatitude(loc.getLatitude());
                        p.setLongitude(loc.getLongitude());
                        db.updatePanorama(p);
                    }
                });
            } catch (SecurityException ignored) {}
        }

        doUpload(p, apiUrl);
    }

    private static boolean isImageMime(String mimeType) {
        return mimeType.startsWith("image/");
    }

    private void doUpload(Panorama p, String apiUrl) {
        try {
            File file = copyToCache(selectedFileUri);
            String mimeType = getMimeType(file.getName());
            boolean isImage = isImageMime(mimeType);
            RequestBody reqFile = RequestBody.create(file, MediaType.parse(mimeType));
            String fieldName = isImage ? "file" : "video";
            MultipartBody.Part body = MultipartBody.Part.createFormData(fieldName, file.getName(), reqFile);

            p.setJobType(isImage ? Panorama.TYPE_IMAGE : Panorama.TYPE_VIDEO);

            ProcessingService svc = ApiClient.getProcessingService(apiUrl);
            Call<JobCreateResponse> call = isImage
                    ? svc.createImageJob(body)
                    : svc.createVideoJob(body);
            call.enqueue(new Callback<JobCreateResponse>() {
                @Override public void onResponse(Call<JobCreateResponse> call, Response<JobCreateResponse> r) {
                    if (r.isSuccessful() && r.body() != null) {
                        p.setJobId(r.body().getJobId());
                        p.setStatus(Panorama.STATUS_PROCESSING);
                        db.updatePanorama(p);
                        runOnUiThread(() -> {
                            setUploading(false);
                            Intent detail = new Intent(UploadActivity.this, DetailActivity.class);
                            detail.putExtra("panorama_id", p.getId());
                            startActivity(detail);
                            finish();
                        });
                    } else {
                        handleUploadError(p);
                    }
                }
                @Override public void onFailure(Call<JobCreateResponse> call, Throwable t) {
                    handleUploadError(p);
                }
            });
        } catch (Exception e) {
            handleUploadError(p);
        }
    }

    private void handleUploadError(Panorama p) {
        db.updateStatus(p.getId(), Panorama.STATUS_FAILED);
        runOnUiThread(() -> {
            setUploading(false);
            Toast.makeText(this, R.string.error_network, Toast.LENGTH_LONG).show();
        });
    }

    private void setUploading(boolean uploading) {
        progressUpload.setVisibility(uploading ? View.VISIBLE : View.GONE);
        tvUploadStatus.setVisibility(uploading ? View.VISIBLE : View.GONE);
        tvUploadStatus.setText(uploading ? getString(R.string.uploading) : "");
        btnUpload.setEnabled(!uploading);
    }

    private File copyToCache(Uri uri) throws Exception {
        String name = "upload_" + System.currentTimeMillis();
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        }
        File out = new File(getCacheDir(), name);
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) fos.write(buf, 0, len);
        }
        return out;
    }

    private String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi") ||
                lower.endsWith(".mkv")) return "video/mp4";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
