package com.android.zigzag.mubi;

import com.google.android.gms.wearable.DataMap;

public class AlbumDataMap {

    public  String albumTitle;
    public  String artist;
    public  String track;
    public  String nearestAddress;

    public AlbumDataMap(String albumTitle, String artist, String track, String nearestAddress) {
        this.albumTitle = albumTitle;
        this.artist = artist;
        this.track = track;
        this.nearestAddress = nearestAddress;
    }

    public AlbumDataMap(DataMap map) {
        this(map.getString("albumTitle"),
             map.getString("artist"),
             map.getString("track"),
             map.getString("nearestAddress")
        );
    }

    public DataMap putToDataMap(DataMap map) {
        map.putString("albumTitle", albumTitle);
        map.putString("artist", artist);
        map.putString("track", track);
        map.putString("nearestAddress", nearestAddress);
        return map;
    }

}
