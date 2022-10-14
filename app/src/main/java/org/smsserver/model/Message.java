package org.smsserver.model;

public class Message {

    private String id;
    private String message;
    private String recipient;
    private String status;
    private String code;
    private String sim;

    public Message(){}

    public Message(String id,
                   String message,
                   String recipient,
                   String status,
                   String code,
                   String sim) {
        this.id = id;
        this.message = message;
        this.recipient = recipient;
        this.status = status;
        this.code = code;
        this.sim = sim;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code.toString();
    }

    public int getSim() {
        return Integer. parseInt(sim);
    }

    public void setSim(String sim) {
        this.sim = sim;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", recipient='" + recipient + '\'' +
                ", status='" + status + '\'' +
                ", code='" + code + '\'' +
                ", sim='" + sim + '\'' +
                '}';
    }
}
