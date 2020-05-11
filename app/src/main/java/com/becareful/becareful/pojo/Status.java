package com.becareful.becareful.pojo;

public class Status {

    private long timestamp;
    private double temperature;
    private int presence;
    private Coordinates coordinates;
    private boolean alarmSounding;

    public Status(long timestamp, double temperature, int presence, Coordinates coordinates, boolean alarmSounding) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.presence = presence;
        this.coordinates = coordinates;
        this.alarmSounding = alarmSounding;
    }

    @Override
    public String toString() {
        return "Status Data: \n"
                + "Timestamp = " + timestamp
                + "\nTemperature = " + temperature
                + "\nPresence = " + presence
                + "\nCoordinates " + coordinates.getLatitude()
                + ", " + coordinates.getLongitude()
                + "("+coordinates.getTimestamp()+")";

    }

    public long getTimestamp() {
        return timestamp;
    }


    public double getTemperature() {
        return temperature;
    }

    public int getPresence() {
        return presence;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public boolean isAlarmSounding() {
        return alarmSounding;
    }
}


