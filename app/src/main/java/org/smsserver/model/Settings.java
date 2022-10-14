package org.smsserver.model;

public class Settings {

    private Integer numberOfThread = 1;
    private Integer interval = 3;

    public Settings() {
    }

    public Integer getNumberOfThread() {
        return numberOfThread;
    }

    public void setNumberOfThread(Integer numberOfThread) {
        this.numberOfThread = numberOfThread;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }
}
