package org.smsserver.model;

import static org.smsserver.config.Const.SMS_SERVER;

import com.google.gson.Gson;

public class Events {

    private String from = SMS_SERVER;
    private String token;
    private String event;
    private String data;


    public String toJson(){
        Gson gson = new Gson();
        String eventJson = gson.toJson(this);
        return eventJson;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public void setData(String data) {
        this.data = data;
    }
}
