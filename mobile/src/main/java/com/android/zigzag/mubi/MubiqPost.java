package com.android.zigzag.mubi;

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
  public String getText() {
    return getString("text");
  }

  // becomes song information split into artist, song title, album, (coverart), etc
  public void setText(String value) {
    put("text", value);
  }

  public ParseUser getUser() {
    return getParseUser("user");
  }

  public void setUser(ParseUser value) {
    put("user", value);
  }

  public ParseGeoPoint getLocation() {
    return getParseGeoPoint("location");
  }

  public void setLocation(ParseGeoPoint value) {
    put("location", value);
  }

  public static ParseQuery<MubiqPost> getQuery() {
    return ParseQuery.getQuery(MubiqPost.class);
  }
}
