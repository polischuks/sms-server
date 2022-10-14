package org.smsserver.model;

import com.google.gson.Gson;

public class Device {

    private String title;
    private String model;
    private String token = "";

    public String toJson(){
        Gson gson = new Gson();
        String userJson = gson.toJson(this);
        return userJson;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "Device " +
                "title='" + title + '\'' +
                ", model='" + model + '\'' +
                ", token='" + token;
    }
}
