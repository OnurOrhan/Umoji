package com.umoji.umoji.Models;

public class Notification {
    private String not_id;
    private int type; // 1-Replied, 2-Liked, 3-SentFriendRequest, 4-AcceptedFriend, 5-Followed
    private Boolean seen;

    private String user1;
    private String user2;
    private String key1;
    private String key2;
    private long date_created;

    public Notification() {
        seen = false;
    }

    public Notification(String not_id) {
        this.not_id = not_id;
        seen = false;
    }

    public Notification(int type, String not_id, String user1, String user2) {
        this.type = type;
        this.not_id = not_id;
        this.user1 = user1;
        this.user2 = user2;
        seen = false;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getNot_id() {
        return not_id;
    }

    public void setNot_id(String not_id) {
        this.not_id = not_id;
    }

    public String getUser1() {
        return user1;
    }

    public void setUser1(String user1) {
        this.user1 = user1;
    }

    public String getUser2() {
        return user2;
    }

    public void setUser2(String user2) {
        this.user2 = user2;
    }

    public long getDate_created() {
        return date_created;
    }

    public void setDate_created(long date_created) {
        this.date_created = date_created;
    }

    public String getKey1() {
        return key1;
    }

    public void setKey1(String key1) {
        this.key1 = key1;
    }

    public String getKey2() {
        return key2;
    }

    public void setKey2(String key2) {
        this.key2 = key2;
    }

    public Boolean getSeen() {
        return seen;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }
}
