package com.sportssync.app.activities.models;

public class User {
    private String uid;
    private String uucmsId;
    private String name;
    private String userType;
    private boolean isRegistered;
    private long registeredAt;

    public User() {
    }

    public User(String uid, String uucmsId, String name, String userType) {
        this.uid = uid;
        this.uucmsId = uucmsId;
        this.name = name;
        this.userType = userType;
        this.isRegistered = false;
        this.registeredAt = 0;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUucmsId() {
        return uucmsId;
    }

    public void setUucmsId(String uucmsId) {
        this.uucmsId = uucmsId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean registered) {
        isRegistered = registered;
    }

    public long getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(long registeredAt) {
        this.registeredAt = registeredAt;
    }
}