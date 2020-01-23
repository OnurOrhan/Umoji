package com.umoji.umoji.Models;

public class Chat {
        private String user_id;
        private String username;
        private String last_message;

        private long last_date;
        private Boolean from_self;

        public Chat() {
            this.last_date = 0;
            this.from_self = true;
        }

        public Chat(String username){
            this.username = username;
            this.last_date = 0;
            this.from_self = true;
        }

        public Chat(String user_id, String last_message) {
            this.user_id = user_id;
            this.last_message = last_message;
            this.last_date = 0;
            this.from_self = true;
        }

        public Chat(String user_id, String last_message, long last_date) {
            this.user_id = user_id;
            this.last_message = last_message;
            this.last_date = last_date;
            this.from_self = true;
        }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getLast_message() {
        return last_message;
    }

    public void setLast_message(String last_message) {
        this.last_message = last_message;
    }

    public long getLast_date() {
        return last_date;
    }

    public void setLast_date(long last_date) {
        this.last_date = last_date;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getFrom_self() {
        return from_self;
    }

    public void setFrom_self(Boolean from_self) {
        this.from_self = from_self;
    }
}
