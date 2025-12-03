package com.project.sportssync;

public class AttendanceRecord {
    private String uucms;
    private String studentName;
    private String sport;
    private String status;
    private String timestamp;

    public AttendanceRecord() {
    }

    public AttendanceRecord(String uucms, String studentName, String sport, String status, String timestamp) {
        this.uucms = uucms;
        this.studentName = studentName;
        this.sport = sport;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getUucms() {
        return uucms;
    }

    public void setUucms(String uucms) {
        this.uucms = uucms;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
