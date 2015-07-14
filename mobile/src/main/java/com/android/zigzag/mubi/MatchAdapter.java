package com.android.zigzag.mubi;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;


public class MatchAdapter extends ArrayAdapter<MubiqPost> {
    private final Activity context;
    private  ArrayList<MubiqPost> posts;

    static class ViewHolder {
        public TextView artistName;
        public TextView track;
        public TextView albumTitle;
        public TextView nearestAddress;
        public ImageView cover;
    }

    public MatchAdapter(Activity context,  ArrayList<MubiqPost> posts) {
        super(context, R.layout.match_item, posts);
        this.context = context;
        this.posts = posts;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.match_item, null);
            // configure view holder
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.artistName = (TextView) rowView.findViewById(R.id.listview_artist);
            viewHolder.track = (TextView) rowView.findViewById(R.id.listview_track);
            viewHolder.albumTitle = (TextView) rowView.findViewById(R.id.listview_albumTitle);
            viewHolder.nearestAddress = (TextView) rowView.findViewById(R.id.listview_nearestAddress);
            viewHolder.cover = (ImageView) rowView.findViewById(R.id.listview_cover);
            rowView.setTag(viewHolder);
        }

        // fill data
        ViewHolder holder = (ViewHolder) rowView.getTag();
        MubiqPost post = posts.get(position);

        if(post.getArtist() != null && post.getNearestAddress().length() > 3)
        holder.artistName.setText(post.getArtist());
        if(post.getTrack() != null && post.getNearestAddress().length() > 3)
        holder.track.setText(post.getTrack());
        if(post.getAlbum() != null && post.getNearestAddress().length() > 3)
        holder.albumTitle.setText(post.getAlbum());
        if(post.getNearestAddress() != null && post.getNearestAddress().length() > 3)
        holder.nearestAddress.setText(post.getNearestAddress());

        ImageLoader.getInstance().displayImage(post.getCoverArtUrl(),holder.cover);
        return rowView;
    }
}
