package com.example.aplicatieandroidprocesare360;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import com.example.aplicatieandroidprocesare360.adapter.PanoramaGridAdapter;
import com.example.aplicatieandroidprocesare360.adapter.PanoramaListAdapter;
import com.example.aplicatieandroidprocesare360.database.DatabaseHelper;
import com.example.aplicatieandroidprocesare360.model.Panorama;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LibraryActivity extends AppCompatActivity {

    private ListView            listView;
    private GridView            gridView;
    private LinearLayout        layoutEmpty;
    private PanoramaListAdapter listAdapter;
    private PanoramaGridAdapter gridAdapter;
    private DatabaseHelper      db;

    private boolean isGridMode    = false;
    private String  currentFilter = null;
    private String  currentQuery  = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db          = DatabaseHelper.getInstance(this);
        listView    = findViewById(R.id.list_panoramas);
        gridView    = findViewById(R.id.grid_panoramas);
        layoutEmpty = findViewById(R.id.layout_empty);

        listAdapter = new PanoramaListAdapter(this, new ArrayList<>());
        gridAdapter = new PanoramaGridAdapter(this, new ArrayList<>());
        listView.setAdapter(listAdapter);
        gridView.setAdapter(gridAdapter);

        AdapterView.OnItemClickListener clickListener = (parent, view, pos, id) -> {
            Panorama p = (Panorama) parent.getItemAtPosition(pos);
            Intent i = new Intent(this, DetailActivity.class);
            i.putExtra("panorama_id", p.getId());
            startActivity(i);
        };
        listView.setOnItemClickListener(clickListener);
        gridView.setOnItemClickListener(clickListener);

        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                loadData();
                return true;
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v ->
                startActivity(new Intent(this, UploadActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        List<Panorama> all = currentFilter != null
                ? db.getPanoramasByStatus(currentFilter)
                : db.getAllPanoramas();

        if (!currentQuery.isEmpty()) {
            String q = currentQuery.toLowerCase();
            all = all.stream()
                    .filter(p -> p.getTitle() != null && p.getTitle().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        listAdapter.updateData(all);
        gridAdapter.updateData(all);
        layoutEmpty.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);
        listView.setVisibility(all.isEmpty() || isGridMode ? View.GONE : View.VISIBLE);
        gridView.setVisibility(all.isEmpty() || !isGridMode ? View.GONE : View.VISIBLE);
    }

    private void toggleViewMode() {
        isGridMode = !isGridMode;
        loadData();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_library, menu);
        MenuItem toggleItem = menu.findItem(R.id.action_toggle_view);
        toggleItem.setTitle(isGridMode ? R.string.menu_view_list : R.string.menu_view_grid);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) { finish(); return true; }
        if (id == R.id.action_toggle_view) { toggleViewMode(); return true; }
        if (id == R.id.action_filter_all)     { currentFilter = null;                       loadData(); return true; }
        if (id == R.id.action_filter_done)    { currentFilter = Panorama.STATUS_DONE;        loadData(); return true; }
        if (id == R.id.action_filter_pending) { currentFilter = Panorama.STATUS_PENDING;     loadData(); return true; }
        if (id == R.id.action_filter_failed)  { currentFilter = Panorama.STATUS_FAILED;      loadData(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
