package com.project.sportssync;

public class RequestModel {
    private String id;
    private String uucms;
    private String sport;
    private String status;

    public RequestModel(String id, String uucms, String sport, String status) {
        this.id = id;
        this.uucms = uucms;
        this.sport = sport;
        this.status = status;
    }

    public String getId() { return id; }
    public String getUucms() { return uucms; }
    public String getSport() { return sport; }
    public String getStatus() { return status; }
}