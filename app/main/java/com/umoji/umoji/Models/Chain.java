package com.umoji.umoji.Models;

public class Chain {
    private String chain_id;
    private String first_video;
    private String first_user;
    private String video_uri;
    private String video_format;
    private String title;

    private long date_created;
    private int responses;
    private int likes;
    private int views;

    public Chain() {
        this.responses = 0;
        this.likes = 0;
        this.views = 0;
    }

    public Chain(String chain_id, String first_video, String first_user) {
        this.chain_id = chain_id;
        this.first_video = first_video;
        this.first_user = first_user;
        this.responses = 0;
        this.likes = 0;
        this.views = 0;
    }

    public String getChain_id() {
        return chain_id;
    }

    public void setChain_id(String chain_id) {
        this.chain_id = chain_id;
    }

    public String getFirst_video() {
        return first_video;
    }

    public void setFirst_video(String first_video) {
        this.first_video = first_video;
    }

    public String getFirst_user() {
        return first_user;
    }

    public void setFirst_user(String first_user) {
        this.first_user = first_user;
    }

    public long getDate_created() {
        return date_created;
    }

    public void setDate_created(long date_created) {
        this.date_created = date_created;
    }

    public String getVideo_uri() {
        return video_uri;
    }

    public void setVideo_uri(String video_uri) {
        this.video_uri = video_uri;
    }

    public int getResponses() {
        return responses;
    }

    public void setResponses(int responses) {
        this.responses = responses;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVideo_format() {
        return video_format;
    }

    public void setVideo_format(String video_format) {
        this.video_format = video_format;
    }
}
