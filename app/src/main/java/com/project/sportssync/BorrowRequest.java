package com.project.sportssync;

import com.google.firebase.Timestamp;

import java.util.List;

public class BorrowRequest {
    private String id;
    private String userId;
    private String uucms;
    private String sport;
    private List<BorrowedEquipment> equipment;
    private String status;
    private Timestamp borrowedAt;
    private Timestamp returnedAt;
    private String type;

    public BorrowRequest() {}

    public BorrowRequest(String userId, String uucms, String sport, 
                        List<BorrowedEquipment> equipment, String type) {
        this.userId = userId;
        this.uucms = uucms;
        this.sport = sport;
        this.equipment = equipment;
        this.status = "pending";
        this.borrowedAt = Timestamp.now();
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUucms() {
        return uucms;
    }

    public void setUucms(String uucms) {
        this.uucms = uucms;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public List<BorrowedEquipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(List<BorrowedEquipment> equipment) {
        this.equipment = equipment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getBorrowedAt() {
        return borrowedAt;
    }

    public void setBorrowedAt(Timestamp borrowedAt) {
        this.borrowedAt = borrowedAt;
    }

    public Timestamp getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(Timestamp returnedAt) {
        this.returnedAt = returnedAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // New fields for deadline management
    private Timestamp borrowedUntil;
    private int penaltyPoints = 0;
    private boolean reminderSent = false;

    public Timestamp getBorrowedUntil() {
        return borrowedUntil;
    }

    public void setBorrowedUntil(Timestamp borrowedUntil) {
        this.borrowedUntil = borrowedUntil;
    }

    public int getPenaltyPoints() {
        return penaltyPoints;
    }

    public void setPenaltyPoints(int penaltyPoints) {
        this.penaltyPoints = penaltyPoints;
    }

    public boolean isReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }

    /**
     * Check if the borrowed equipment is overdue
     * @return true if current time is past borrowedUntil
     */
    public boolean isOverdue() {
        if (borrowedUntil == null || !"borrowed".equals(status)) {
            return false;
        }
        return System.currentTimeMillis() > borrowedUntil.toDate().getTime();
    }

    /**
     * Get number of days overdue (negative if not yet due)
     * @return days overdue, or negative days remaining
     */
    public int getDaysOverdue() {
        if (borrowedUntil == null) {
            return 0;
        }
        long diffInMillis = System.currentTimeMillis() - borrowedUntil.toDate().getTime();
        return (int) (diffInMillis / (1000 * 60 * 60 * 24));
    }

    /**
     * Get number of days remaining until deadline
     * @return days remaining, or 0 if overdue
     */
    public int getDaysRemaining() {
        if (borrowedUntil == null) {
            return 0;
        }
        long diffInMillis = borrowedUntil.toDate().getTime() - System.currentTimeMillis();
        int days = (int) (diffInMillis / (1000 * 60 * 60 * 24));
        return Math.max(0, days);
    }

    /**
     * Get status color based on deadline
     * @return color code: green (safe), yellow (due soon), red (overdue)
     */
    public String getDeadlineStatus() {
        if (borrowedUntil == null || !"borrowed".equals(status)) {
            return "none";
        }
        if (isOverdue()) {
            return "overdue";  // Red
        }
        int daysRemaining = getDaysRemaining();
        if (daysRemaining <= 1) {
            return "due_soon";  // Yellow
        }
        return "safe";  // Green
    }

    public static class BorrowedEquipment {
        private String name;
        private int quantity;

        public BorrowedEquipment() {}

        public BorrowedEquipment(String name, int quantity) {
            this.name = name;
            this.quantity = quantity;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}
