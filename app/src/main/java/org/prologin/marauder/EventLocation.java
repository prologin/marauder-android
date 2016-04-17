package org.prologin.marauder;

import android.location.Location;

/**
 * The location of an event, as returned by the Marauder API.
 */
public class EventLocation {
  private double lat;
  private double lon;
  private double radiusInMeters;

  public EventLocation(double lat, double lon, double radiusInMeters) {
    this.lat = lat;
    this.lon = lon;
    this.radiusInMeters = radiusInMeters;
  }

  public double getLat() {
    return lat;
  }

  public double getLon() {
    return lon;
  }

  public double getRadiusInMeters() {
    return radiusInMeters;
  }

  public Location toAndroidLocation() {
    Location l = new Location("");
    l.setLatitude(lat);
    l.setLongitude(lon);
    return l;
  }
}
