package com.biodex.model;

/** A suburb a sighting can be recorded in. Mirrors the {@code suburbs} table. */
public class Suburb {

    private int suburbId;
    private String name;
    private String postcode;
    private double latitude;
    private double longitude;

    public Suburb() {
    }

    public Suburb(String name, String postcode, double latitude, double longitude) {
        this.name = name;
        this.postcode = postcode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getSuburbId() {
        return suburbId;
    }

    public void setSuburbId(int suburbId) {
        this.suburbId = suburbId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
