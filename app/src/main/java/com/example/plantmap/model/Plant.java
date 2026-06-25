package com.example.plantmap.model;

import java.util.ArrayList;
import java.util.List;

public class Plant {
    public int id = 0;
    public String name;
    public String type;
    public String group;
    public String additionalInfo;
    public Integer flowerColorId;
    public String imagePublicKey;
    public List<Integer> availablePotVolumes = new ArrayList<>();;

    public Plant() {}
    @Override
    public String toString() {
        return name;
    }
    public Plant(String name,
                 String type,
                 Integer flowerColorId,
                 String group,
                 String additionalInfo,
                 String imagePublicKey) {
        this.name = name;
        this.type = type;
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
