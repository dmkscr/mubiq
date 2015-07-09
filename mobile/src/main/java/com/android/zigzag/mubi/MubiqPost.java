package com.android.zigzag.mubi;

import com.google.android.gms.wearable.Asset;
import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

/**
 * Data model for a post.
 */
@ParseClassName("Posts")
public class MubiqPost extends ParseObject {

  public String getAlbum() { return getString("albumTitle"); }

  public void setAlbum(String value) { put("albumTitle", value); }


  public String getArtist() { return getString("artist"); }

  public void setArtist(String value) { put("artist", value); }


  public String getTrack() { return getString("track"); }

  public void setTrack(String value) { put("track", value); }


  public ParseUser getUser() { return getParseUser("user"); }

  public void setUser(ParseUser value) { put("user", value); }


  public ParseUser getCoverArtUrl() { return getParseUser("coverArtUrl"); }

  public void setCoverArtUrl(String value) { put("coverArtUrl", value); }


  public ParseGeoPoint getLocation() {
    return getParseGeoPoint("location");
  }

  public void setLocation(ParseGeoPoint value) {
    put("location", value);
  }


  public String getNearestAddress() {
    return getString("nearestAddress");
  }

  public void setNearestAddress(String value) {
    put("nearestAddress", value);
  }


  public static ParseQuery<MubiqPost> getQuery() {
    return ParseQuery.getQuery(MubiqPost.class);
  }
}
