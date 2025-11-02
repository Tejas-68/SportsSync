package com.sportssync.app.activities.models;

public class AttendanceRequest {
    private String requestId;
    private String studentId;
    private String studentName;
    private String uucmsId;
    private long requestedAt;
    private String status;
    private long respondedAt;
    private String respondedBy;

    public AttendanceRequest() {
    }

    public AttendanceRequest(String requestId, String studentId, String studentName, String uucmsId) {
        this.requestId = requestId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.uucmsId = uucmsId;
        this.requestedAt = System.currentTimeMillis();
        this.status = "pending";
        this.respondedAt = 0;
        this.respondedBy = "";
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public String getUucmsId() {
        return uucmsId;
    }

    public void setUucmsId(String uucmsId) {
        this.uucmsId = uucmsId;
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

    public String getRespondedBy() {
        return respondedBy;
    }

    public void setRespondedBy(String respondedBy) {
        this.respondedBy = respondedBy;
    }
}