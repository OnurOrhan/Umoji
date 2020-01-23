package com.umoji.umoji.Models;

public class Like {
    private String video_id;
    private String user_id;

    public Like() {
    }

    public Like(String video_id, String user_id) {
        this.video_id = video_id;
        this.user_id = user_id;
    }

    public String getVideo_id() {
        return video_id;
    }

    public void setVideo_id(String video_id) {
        this.video_id = video_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
}
