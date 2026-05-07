package com.example.aplicatieandroidprocesare360;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.aplicatieandroidprocesare360.database.DatabaseHelper;
import com.example.aplicatieandroidprocesare360.model.Panorama;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private BarChart       chartUploads, chartQuality;
    private PieChart       chartStatus;
    private LineChart      chartProcTime;
    private TextView       tvAvgTime, tvAvgQuality;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db             = DatabaseHelper.getInstance(this);
        chartUploads   = findViewById(R.id.chart_uploads);
        chartStatus    = findViewById(R.id.chart_status);
        chartProcTime  = findViewById(R.id.chart_proc_time);
        chartQuality   = findViewById(R.id.chart_quality);
        tvAvgTime      = findViewById(R.id.tv_avg_time);
        tvAvgQuality   = findViewById(R.id.tv_avg_quality);

        loadStats();
    }

    private void loadStats() {
        List<Panorama> all = db.getAllPanoramas();

        // Summary
        double avgTime    = db.getAverageProcessingTime();
        double avgQuality = db.getAverageQualityScore();
        tvAvgTime.setText(String.format(Locale.getDefault(), "%d ms", (long) avgTime));
        tvAvgQuality.setText(String.format(Locale.getDefault(), "%.2f", avgQuality));

        setupUploadBarChart(all);
        setupStatusPieChart();
        setupProcTimeLineChart(all);
        setupQualityBarChart(all);
    }

    private void setupUploadBarChart(List<Panorama> all) {
        // Group uploads by day (last 7 days)
        long now = System.currentTimeMillis();
        long day = 86400000L;
        float[] counts = new float[7];
        for (Panorama p : all) {
            long diff = now - p.getUploadDate();
            int  dayIdx = (int)(diff / day);
            if (dayIdx >= 0 && dayIdx < 7) counts[6 - dayIdx]++;
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) entries.add(new BarEntry(i, counts[i]));

        BarDataSet ds = new BarDataSet(entries, "Încărcări");
        ds.setColor(0xFF2196F3);
        ds.setValueTextColor(Color.WHITE);

        styleBarChart(chartUploads);
        chartUploads.setData(new BarData(ds));
        chartUploads.invalidate();
    }

    private void setupStatusPieChart() {
        int done      = db.countDone();
        int failed    = db.countFailed();
        int pending   = db.countPending();
        int total     = db.countAll();
        int other     = total - done - failed - pending;

        List<PieEntry> entries = new ArrayList<>();
        if (done    > 0) entries.add(new PieEntry(done,    "Finalizate"));
        if (failed  > 0) entries.add(new PieEntry(failed,  "Eșuate"));
        if (pending > 0) entries.add(new PieEntry(pending, "În așteptare"));
        if (other   > 0) entries.add(new PieEntry(other,   "Altele"));

        if (entries.isEmpty()) {
            chartStatus.setNoDataText(getString(R.string.stats_no_data));
            chartStatus.setNoDataTextColor(Color.WHITE);
            chartStatus.invalidate();
            return;
        }

        int[] colors = {0xFF4CAF50, 0xFFF44336, 0xFFFFC107, 0xFF9E9E9E};
        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(colors);
        ds.setValueTextColor(Color.WHITE);
        ds.setValueTextSize(12f);

        chartStatus.setUsePercentValues(true);
        chartStatus.getDescription().setEnabled(false);
        chartStatus.setHoleColor(0xFF252525);
        chartStatus.setHoleRadius(40f);
        chartStatus.getLegend().setTextColor(Color.WHITE);
        chartStatus.setData(new PieData(ds));
        chartStatus.invalidate();
    }

    private void setupProcTimeLineChart(List<Panorama> all) {
        List<Entry> entries = new ArrayList<>();
        int idx = 0;
        for (Panorama p : all) {
            if (p.getProcessingTimeMs() > 0) {
                entries.add(new Entry(idx++, p.getProcessingTimeMs()));
            }
        }
        if (entries.isEmpty()) {
            chartProcTime.setNoDataText(getString(R.string.stats_no_data));
            chartProcTime.setNoDataTextColor(Color.WHITE);
            chartProcTime.invalidate();
            return;
        }

        LineDataSet ds = new LineDataSet(entries, "Timp procesare (ms)");
        ds.setColor(0xFF00BCD4);
        ds.setCircleColor(0xFF00BCD4);
        ds.setValueTextColor(Color.WHITE);
        ds.setDrawFilled(true);
        ds.setFillColor(0x3000BCD4);
        ds.setLineWidth(2f);

        chartProcTime.getDescription().setEnabled(false);
        chartProcTime.getXAxis().setTextColor(Color.WHITE);
        chartProcTime.getAxisLeft().setTextColor(Color.WHITE);
        chartProcTime.getAxisRight().setEnabled(false);
        chartProcTime.getLegend().setTextColor(Color.WHITE);
        chartProcTime.setBackgroundColor(0xFF252525);
        chartProcTime.setData(new LineData(ds));
        chartProcTime.invalidate();
    }

    private void setupQualityBarChart(List<Panorama> all) {
        List<BarEntry> entries = new ArrayList<>();
        int idx = 0;
        for (Panorama p : all) {
            if (p.getQualityScore() > 0) {
                entries.add(new BarEntry(idx++, p.getQualityScore()));
            }
        }
        if (entries.isEmpty()) {
            chartQuality.setNoDataText(getString(R.string.stats_no_data));
            chartQuality.setNoDataTextColor(Color.WHITE);
            chartQuality.invalidate();
            return;
        }

        BarDataSet ds = new BarDataSet(entries, "Scor calitate");
        ds.setColor(0xFF9C27B0);
        ds.setValueTextColor(Color.WHITE);

        styleBarChart(chartQuality);
        chartQuality.setData(new BarData(ds));
        chartQuality.invalidate();
    }

    private void styleBarChart(BarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setBackgroundColor(0xFF252525);
        chart.getXAxis().setTextColor(Color.WHITE);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setTextColor(Color.WHITE);
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
