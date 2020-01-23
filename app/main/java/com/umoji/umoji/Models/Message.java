package com.umoji.umoji.Models;

public class Message {
    private String message_text;
    private long message_date;
    private Boolean from_self;

    public Message() {
        this.from_self = true;
    }

    public Message(String message_text) {
        this.message_text = message_text;
        this.from_self = true;
    }

    public Message(String message_text, long message_date) {
        this.message_text = message_text;
        this.message_date = message_date;
        this.from_self = true;
    }

    public String getMessage_text() {
        return message_text;
    }

    public void setMessage_text(String message_text) {
        this.message_text = message_text;
    }

    public long getMessage_date() {
        return message_date;
    }

    public void setMessage_date(long message_date) {
        this.message_date = message_date;
    }

    public Boolean getFrom_self() {
        return from_self;
    }

    public void setFrom_self(Boolean from_self) {
        this.from_self = from_self;
    }
}
