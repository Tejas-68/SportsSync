package com.project.sportssync;

import com.google.firebase.Timestamp;
import java.util.List;

public class RequestModel {
    private String requestId;
    private String userId;
    private String uucms;
    private String studentName;
    private String sport;
    private String status;
    private Timestamp requestedAt;
    private String type; // "attendance" or "return"
    private String sportId;
    private List<BorrowRequest.BorrowedEquipment> equipmentList;

    public RequestModel() {}

    public RequestModel(String requestId, String userId, String uucms, String studentName, String sport, String status, Timestamp requestedAt) {
        this.requestId = requestId;
        this.userId = userId;
        this.uucms = uucms;
        this.studentName = studentName;
        this.sport = sport;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUucms() { return uucms; }
    public void setUucms(String uucms) { this.uucms = uucms; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Timestamp requestedAt) { this.requestedAt = requestedAt; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSportId() { return sportId; }
    public void setSportId(String sportId) { this.sportId = sportId; }

    public List<BorrowRequest.BorrowedEquipment> getEquipmentList() { return equipmentList; }
    public void setEquipmentList(List<BorrowRequest.BorrowedEquipment> equipmentList) { this.equipmentList = equipmentList; }
}