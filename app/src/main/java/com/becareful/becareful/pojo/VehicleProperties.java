package com.becareful.becareful.pojo;


public class VehicleProperties {

    private String color;
    private String model;
    private String plate;

    public VehicleProperties(String color, String model, String plate){
        this.color = color;
        this.plate = plate;
        this.model = model;
    }

    public String getColor() {
        return color;
    }

    public String getModel() {
        return model;
    }

    public String getPlate() {
        return plate;
    }
}
