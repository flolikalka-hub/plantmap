package com.example.plantmap.model;

public class Plant {
    public int id = 0;
    public String name;
    public String type;
    public Integer potVolume;
    public String group;
    public String additionalInfo;
    public Integer flowerColorId;
    public String imagePublicKey;

    public Plant() {}
    @Override
    public String toString() {
        return name;
    }
    public Plant(String name,
                 String type,
                 Integer potVolume,
                 Integer flowerColorId,
                 String group,
                 String additionalInfo,
                 String imagePublicKey) {
        this.name = name;
        this.type = type;
        this.potVolume = potVolume;
        this.flowerColorId = flowerColorId;
        this.group = group;
        this.additionalInfo = additionalInfo;
        this.imagePublicKey = imagePublicKey;
    }
    public String getImagePublicKey() {
        return imagePublicKey;
    }

    public void setImagePublicKey(String imagePublicKey) {
        this.imagePublicKey = imagePublicKey;
    }
}
