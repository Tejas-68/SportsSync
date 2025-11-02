package com.sportssync.app.activities.models;

import java.util.ArrayList;
import java.util.List;

public class Sport {
    private String sportId;
    private String sportName;
    private List<Equipment> equipmentList;
    private boolean isActive;
    private long createdAt;

    public Sport() {
        this.equipmentList = new ArrayList<>();
    }

    public Sport(String sportId, String sportName) {
        this.sportId = sportId;
        this.sportName = sportName;
        this.equipmentList = new ArrayList<>();
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
    }

    public String getSportId() {
        return sportId;
    }

    public void setSportId(String sportId) {
        this.sportId = sportId;
    }

    public String getSportName() {
        return sportName;
    }

    public void setSportName(String sportName) {
        this.sportName = sportName;
    }

    public List<Equipment> getEquipmentList() {
        return equipmentList;
    }

    public void setEquipmentList(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}