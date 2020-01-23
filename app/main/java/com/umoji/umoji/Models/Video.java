package com.umoji.umoji.Models;

public class Video {
    private String video_id;
    private String user_id;
    private String chain_id;
    private String video_uri;
    private String video_format;

    private Boolean is_main;
    private String title;
    private String story_tag;

    private long date_created;
    private int likes;
    private int views;

    public Video() {
        this.is_main = false;
        this.likes = 0;
    }

    public Video(String video_id, String user_id) {
        this.video_id = video_id;
        this.user_id = user_id;
        this.is_main = false;
        this.likes = 0;
    }

    public Video(String video_id, String user_id, String chain_id) {
        this.video_id = video_id;
        this.user_id = user_id;
        this.chain_id = chain_id;
        this.is_main = false;
        this.likes = 0;
    }

    public Video(String video_id, String user_id, long date_created, String video_uri) {
        this.video_id = video_id;
        this.user_id = user_id;
        this.date_created = date_created;
        this.video_uri = video_uri;
        this.is_main = false;
        this.likes = 0;
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

    public long getDate_created() {
        return date_created;
    }

    public void setDate_created(long date_created) {
        this.date_created = date_created;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getChain_id() {
        return chain_id;
    }

    public void setChain_id(String chain_id) {
        this.chain_id = chain_id;
    }

    public String getVideo_uri() {
        return video_uri;
    }

    public void setVideo_uri(String video_uri) {
        this.video_uri = video_uri;
    }

    public String getVideo_format() {
        return video_format;
    }

    public void setVideo_format(String video_format) {
        this.video_format = video_format;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public Boolean getIs_main() {
        return is_main;
    }

    public void setIs_main(Boolean is_main) {
        this.is_main = is_main;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStory_tag() {
        return story_tag;
    }

    public void setStory_tag(String story_tag) {
        this.story_tag = story_tag;
    }
}
