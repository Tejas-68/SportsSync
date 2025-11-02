package com.sportssync.app.activities.models;

public class Equipment {
    private String equipmentId;
    private String equipmentName;
    private int totalQuantity;
    private int availableQuantity;

    public Equipment() {
    }

    public Equipment(String equipmentId, String equipmentName, int totalQuantity) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = totalQuantity;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(String equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}