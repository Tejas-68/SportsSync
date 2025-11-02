package com.sportssync.app.activities.models;

public class ReturnRequest {
    private String requestId;
    private String recordId;
    private String studentId;
    private String studentName;
    private String equipmentName;
    private int quantity;
    private long requestedAt;
    private String status;
    private long respondedAt;

    public ReturnRequest() {
    }

    public ReturnRequest(String requestId, String recordId, String studentId,
                         String studentName, String equipmentName, int quantity) {
        this.requestId = requestId;
        this.recordId = recordId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.equipmentName = equipmentName;
        this.quantity = quantity;
        this.requestedAt = System.currentTimeMillis();
        this.status = "pending";
        this.respondedAt = 0;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public long getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(long requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(long respondedAt) {
        this.respondedAt = respondedAt;
    }
}