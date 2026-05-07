package com.example.aplicatieandroidprocesare360.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.aplicatieandroidprocesare360.R;
import com.example.aplicatieandroidprocesare360.model.Panorama;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PanoramaListAdapter extends BaseAdapter {

    private final Context context;
    private List<Panorama> items;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public PanoramaListAdapter(Context context, List<Panorama> items) {
        this.context = context;
        this.items   = new ArrayList<>(items);
    }

    public void updateData(List<Panorama> newItems) {
        this.items = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    @Override public int     getCount()             { return items.size(); }
    @Override public Panorama getItem(int position) { return items.get(position); }
    @Override public long    getItemId(int position){ return items.get(position).getId(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_panorama, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Panorama p = items.get(position);
        holder.tvTitle.setText(p.getTitle() != null ? p.getTitle() : "Fără titlu");
        holder.tvDate.setText(sdf.format(new Date(p.getUploadDate())));
        holder.tvSource.setText(sourceLabel(p.getSourceType()));
        holder.tvStatus.setText(p.getStatus());
        holder.tvStatus.setBackgroundColor(statusColor(p.getStatus()));

        String imageUrl = p.getThumbnailUrl() != null ? p.getThumbnailUrl()
                        : (p.getFilePath() != null ? p.getFilePath() : null);
        if (imageUrl != null) {
            Glide.with(context).load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.imgThumbnail);
        } else {
            holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        return convertView;
    }

    private String sourceLabel(String src) {
        if (src == null) return "Local";
        switch (src) {
            case Panorama.SOURCE_MAPILLARY:  return "Mapillary";
            case Panorama.SOURCE_STREETVIEW: return "Street View";
            default:                          return "Local";
        }
    }

    private int statusColor(String status) {
        if (status == null) return context.getColor(R.color.status_pending);
        switch (status) {
            case Panorama.STATUS_DONE:       return context.getColor(R.color.status_done);
            case Panorama.STATUS_FAILED:     return context.getColor(R.color.status_failed);
            case Panorama.STATUS_PROCESSING: return context.getColor(R.color.status_processing);
            case Panorama.STATUS_UPLOADING:  return context.getColor(R.color.status_uploading);
            default:                          return context.getColor(R.color.status_pending);
        }
    }

    static class ViewHolder {
        ImageView imgThumbnail;
        TextView  tvTitle, tvDate, tvSource, tvStatus;

        ViewHolder(View v) {
            imgThumbnail = v.findViewById(R.id.img_thumbnail);
            tvTitle      = v.findViewById(R.id.tv_title);
            tvDate       = v.findViewById(R.id.tv_date);
            tvSource     = v.findViewById(R.id.tv_source);
            tvStatus     = v.findViewById(R.id.tv_status);
        }
    }
}
