package com.project.sportssync;

import java.util.HashMap;
import java.util.Map;

public class SportModel {
    private String id;
    private String name;
    private Map<String, EquipmentItem> equipment;

    public SportModel() {
        this.equipment = new HashMap<>();
    }

    public SportModel(String id, String name) {
        this.id = id;
        this.name = name;
        this.equipment = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, EquipmentItem> getEquipment() {
        return equipment;
    }

    public void setEquipment(Map<String, EquipmentItem> equipment) {
        this.equipment = equipment;
    }

    public void addEquipment(String key, EquipmentItem item) {
        this.equipment.put(key, item);
    }

    public static class EquipmentItem {
        private String name;
        private int totalQuantity;
        private int availableQuantity;

        public EquipmentItem() {}

        public EquipmentItem(String name, int totalQuantity, int availableQuantity) {
            this.name = name;
            this.totalQuantity = totalQuantity;
            this.availableQuantity = availableQuantity;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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
}
