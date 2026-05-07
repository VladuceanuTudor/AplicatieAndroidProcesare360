package com.example.aplicatieandroidprocesare360;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.aplicatieandroidprocesare360.database.DatabaseHelper;
import com.example.aplicatieandroidprocesare360.model.Panorama;
import com.example.aplicatieandroidprocesare360.vr.SphericalRenderer;
import com.example.aplicatieandroidprocesare360.vr.VideoSphericalRenderer;

import java.util.Locale;

public class VRViewerActivity extends AppCompatActivity
        implements SensorEventListener, VideoSphericalRenderer.OnSurfaceTextureReadyListener {

    // Views
    private GLSurfaceView glSurfaceView;
    private View          layoutLoading, layoutControls, layoutVideoControls, dividerVr, tvHint;
    private ToggleButton  btnToggleCardboard;
    private ImageButton   btnPlayPause;
    private SeekBar       seekbarVideo;
    private TextView      tvVideoTime;

    // Renderers (only one is used at a time)
    private SphericalRenderer      imageRenderer;
    private VideoSphericalRenderer videoRenderer;
    private boolean                isVideoMode = false;

    // ExoPlayer
    private ExoPlayer exoPlayer;

    // Sensors
    private SensorManager sensorManager;
    private Sensor        rotationSensor;
    private final float[] rotationMatrix = new float[16];

    // Video seek update handler
    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private final Runnable seekUpdater = new Runnable() {
        @Override public void run() {
            if (exoPlayer != null && exoPlayer.isPlaying()) {
                updateSeekBar();
                seekHandler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_vr_viewer);

        glSurfaceView        = findViewById(R.id.gl_surface);
        layoutLoading        = findViewById(R.id.layout_loading);
        layoutControls       = findViewById(R.id.layout_controls);
        layoutVideoControls  = findViewById(R.id.layout_video_controls);
        dividerVr            = findViewById(R.id.divider_vr);
        tvHint               = findViewById(R.id.tv_hint);
        btnToggleCardboard   = findViewById(R.id.btn_toggle_cardboard);
        btnPlayPause         = findViewById(R.id.btn_play_pause);
        seekbarVideo         = findViewById(R.id.seekbar_video);
        tvVideoTime          = findViewById(R.id.tv_video_time);

        SharedPreferences prefs      = getSharedPreferences("panorama_prefs", MODE_PRIVATE);
        int               ipdMm      = prefs.getInt("ipd_mm", 65);
        boolean           cardboard  = getIntent().getBooleanExtra("cardboard_mode", false);

        int panoramaId = getIntent().getIntExtra("panorama_id", -1);
        if (panoramaId == -1) {
            Toast.makeText(this, R.string.vr_no_panorama, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        Panorama p = db.getPanoramaById(panoramaId);
        if (p == null) {
            Toast.makeText(this, R.string.vr_no_panorama, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String mediaUrl = p.getResultUrl() != null ? p.getResultUrl()
                        : (p.getThumbnailUrl() != null ? p.getThumbnailUrl()
                        : p.getFilePath());
        if (mediaUrl == null) {
            Toast.makeText(this, R.string.vr_no_panorama, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        isVideoMode = isVideoFile(mediaUrl);

        glSurfaceView.setEGLContextClientVersion(2);

        if (isVideoMode) {
            videoRenderer = new VideoSphericalRenderer(this, this);
            videoRenderer.setCardboardMode(cardboard);
            videoRenderer.setIpd(ipdMm / 1000f);
            glSurfaceView.setRenderer(videoRenderer);
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            setupVideoControls(mediaUrl);
        } else {
            imageRenderer = new SphericalRenderer();
            imageRenderer.setCardboardMode(cardboard);
            imageRenderer.setIpd(ipdMm / 1000f);
            glSurfaceView.setRenderer(imageRenderer);
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            loadImage(mediaUrl);
        }

        btnToggleCardboard.setChecked(cardboard);
        dividerVr.setVisibility(cardboard ? View.VISIBLE : View.GONE);

        btnToggleCardboard.setOnCheckedChangeListener((v, checked) -> {
            if (isVideoMode && videoRenderer != null) videoRenderer.setCardboardMode(checked);
            else if (imageRenderer != null) imageRenderer.setCardboardMode(checked);
            dividerVr.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        glSurfaceView.setOnClickListener(v ->
                layoutControls.setVisibility(
                        layoutControls.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        sensorManager  = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        new Handler(Looper.getMainLooper()).postDelayed(
                () -> tvHint.setVisibility(View.GONE), 4000);
    }

    // ── Image mode ───────────────────────────────────────────────────────

    private void loadImage(String url) {
        Glide.with(this).asBitmap().load(url)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> t) {
                        glSurfaceView.queueEvent(() -> imageRenderer.setPanoramaBitmap(resource));
                        layoutLoading.setVisibility(View.GONE);
                        layoutControls.setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onLoadCleared(android.graphics.drawable.Drawable p) {}
                });
    }

    // ── Video mode ───────────────────────────────────────────────────────

    private void setupVideoControls(String mediaUrl) {
        layoutVideoControls.setVisibility(View.VISIBLE);

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(mediaUrl)));
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        exoPlayer.prepare();

        exoPlayer.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    layoutLoading.setVisibility(View.GONE);
                    layoutControls.setVisibility(View.VISIBLE);
                    seekbarVideo.setMax((int) exoPlayer.getDuration());
                    exoPlayer.play();
                    seekHandler.post(seekUpdater);
                }
            }
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                btnPlayPause.setImageResource(isPlaying
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play);
            }
        });

        btnPlayPause.setOnClickListener(v -> {
            if (exoPlayer.isPlaying()) exoPlayer.pause();
            else                       exoPlayer.play();
        });

        seekbarVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && exoPlayer != null) exoPlayer.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { seekHandler.removeCallbacks(seekUpdater); }
            @Override public void onStopTrackingTouch(SeekBar sb)  { seekHandler.post(seekUpdater); }
        });
    }

    /** Called from GL thread via OnSurfaceTextureReadyListener — attach ExoPlayer to surface. */
    @Override
    public void onSurfaceTextureReady(SurfaceTexture surfaceTexture) {
        if (exoPlayer != null) {
            exoPlayer.setVideoSurface(new Surface(surfaceTexture));
        }
    }

    private void updateSeekBar() {
        if (exoPlayer == null) return;
        long pos      = exoPlayer.getCurrentPosition();
        long duration = exoPlayer.getDuration();
        seekbarVideo.setProgress((int) pos);
        tvVideoTime.setText(String.format(Locale.US, "%s / %s",
                formatTime(pos), formatTime(duration)));
    }

    private String formatTime(long ms) {
        long s = ms / 1000;
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    private boolean isVideoFile(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mov") ||
               lower.endsWith(".mkv") || lower.endsWith(".webm") ||
               lower.endsWith(".avi") || lower.contains("video/");
    }

    // ── Sensors ──────────────────────────────────────────────────────────

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            if (isVideoMode && videoRenderer != null) videoRenderer.setRotationMatrix(rotationMatrix);
            else if (imageRenderer != null)           imageRenderer.setRotationMatrix(rotationMatrix);
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        if (rotationSensor != null)
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        if (exoPlayer != null && isVideoMode) exoPlayer.play();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        sensorManager.unregisterListener(this);
        if (exoPlayer != null) exoPlayer.pause();
        seekHandler.removeCallbacks(seekUpdater);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        seekHandler.removeCallbacks(seekUpdater);
        if (exoPlayer != null) {
            exoPlayer.setVideoSurface(null);
            exoPlayer.release();
            exoPlayer = null;
        }
        if (videoRenderer != null) {
            glSurfaceView.queueEvent(() -> videoRenderer.release());
        }
    }
}
