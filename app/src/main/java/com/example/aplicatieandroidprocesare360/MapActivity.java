package com.example.aplicatieandroidprocesare360;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.aplicatieandroidprocesare360.database.DatabaseHelper;
import com.example.aplicatieandroidprocesare360.model.Panorama;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap           map;
    private DatabaseHelper      db;
    private List<Panorama>      allPanoramas;
    private Map<Marker, Panorama> markerMap = new HashMap<>();
    private Spinner             spinnerFilter;

    private static final String[] FILTER_LABELS = {"Toate", "Procesate", "În așteptare", "Eșuate"};
    private static final String[] FILTER_VALUES = {null, Panorama.STATUS_DONE,
                                                   Panorama.STATUS_PENDING, Panorama.STATUS_FAILED};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = DatabaseHelper.getInstance(this);

        spinnerFilter = findViewById(R.id.spinner_filter);
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, FILTER_LABELS);
        filterAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                refreshMarkers(FILTER_VALUES[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);

        double focusLat = getIntent().getDoubleExtra("focus_lat", 0);
        double focusLng = getIntent().getDoubleExtra("focus_lng", 0);
        if (focusLat != 0 || focusLng != 0) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(focusLat, focusLng), 15f));
        }

        map.setOnMarkerClickListener(marker -> {
            Panorama p = markerMap.get(marker);
            if (p != null) {
                Intent i = new Intent(this, DetailActivity.class);
                i.putExtra("panorama_id", p.getId());
                startActivity(i);
            }
            return true;
        });

        refreshMarkers(null);
    }

    private void refreshMarkers(String statusFilter) {
        if (map == null) return;
        map.clear();
        markerMap.clear();

        allPanoramas = statusFilter != null
                ? db.getPanoramasByStatus(statusFilter)
                : db.getAllPanoramas();

        List<LatLng> positions = new ArrayList<>();
        for (Panorama p : allPanoramas) {
            if (!p.hasLocation()) continue;
            LatLng pos = new LatLng(p.getLatitude(), p.getLongitude());
            positions.add(pos);

            Marker marker = map.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(p.getTitle())
                    .snippet(p.getStatus())
                    .icon(BitmapDescriptorFactory.defaultMarker(statusHue(p.getStatus()))));
            if (marker != null) markerMap.put(marker, p);
        }

        if (positions.size() > 1) {
            map.addPolyline(new PolylineOptions()
                    .addAll(positions)
                    .color(0x552196F3)
                    .width(3f));
        }

        if (!positions.isEmpty() && getIntent().getDoubleExtra("focus_lat", 0) == 0) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(positions.get(0), 12f));
        }
    }

    private float statusHue(String status) {
        if (status == null) return BitmapDescriptorFactory.HUE_YELLOW;
        switch (status) {
            case Panorama.STATUS_DONE:       return BitmapDescriptorFactory.HUE_GREEN;
            case Panorama.STATUS_FAILED:     return BitmapDescriptorFactory.HUE_RED;
            case Panorama.STATUS_PROCESSING: return BitmapDescriptorFactory.HUE_BLUE;
            default:                          return BitmapDescriptorFactory.HUE_YELLOW;
        }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
