package com.example.plantmap.model;

public class Plant {
    public int id = 0;
    public String name;
    public String type;
    public Integer potVolume;
    public String flowerColor;
    public String group;            // plant_group
    public String additionalInfo;   // additional_info

    public Plant() {}
    @Override
    public String toString() {
        return name;
    }
    public Plant(String name,
                 String type,
                 Integer potVolume,
                 String flowerColor,
                 String group,
                 String additionalInfo) {
        this.name = name;
        this.type = type;
        this.potVolume = potVolume;
        this.flowerColor = flowerColor;
        this.group = group;
        this.additionalInfo = additionalInfo;
    }
}
