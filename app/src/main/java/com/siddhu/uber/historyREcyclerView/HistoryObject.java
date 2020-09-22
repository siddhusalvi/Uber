package com.siddhu.uber.historyREcyclerView;

public class HistoryObject {
    private String rideId;
    String time;
    public HistoryObject(String rideId,String time){
        this.rideId = rideId;
        this.time = time;
    }

    public String getRideId() {
        return rideId;
    }

    public String getTime() {
        return time;
    }
}
