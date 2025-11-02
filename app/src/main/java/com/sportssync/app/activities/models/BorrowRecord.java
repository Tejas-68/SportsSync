package com.sportssync.app.activities.models;

public class BorrowRecord {
    private String recordId;
    private String studentId;
    private String studentName;
    private String sportId;
    private String sportName;
    private String equipmentId;
    private String equipmentName;
    private int quantity;
    private long borrowedAt;
    private long returnBy;
    private String status;
    private long returnedAt;

    public BorrowRecord() {
    }

    public BorrowRecord(String recordId, String studentId, String studentName,
                        String sportId, String sportName, String equipmentId,
                        String equipmentName, int quantity, long returnBy) {
        this.recordId = recordId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.sportId = sportId;
        this.sportName = sportName;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.quantity = quantity;
        this.borrowedAt = System.currentTimeMillis();
        this.returnBy = returnBy;
        this.status = "borrowed";
        this.returnedAt = 0;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getBorrowedAt() {
        return borrowedAt;
    }

    public void setBorrowedAt(long borrowedAt) {
        this.borrowedAt = borrowedAt;
    }

    public long getReturnBy() {
        return returnBy;
    }

    public void setReturnBy(long returnBy) {
        this.returnBy = returnBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(long returnedAt) {
        this.returnedAt = returnedAt;
    }
}