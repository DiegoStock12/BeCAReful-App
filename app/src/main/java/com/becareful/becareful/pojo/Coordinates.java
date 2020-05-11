package com.becareful.becareful.pojo;

public class Coordinates {
    private float latitude;
    private float longitude;
    private long timestamp;

    public Coordinates(float latitude, float longitude, long timestamp){
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
