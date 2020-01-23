package com.umoji.umoji.Models;

public class User {
    private String user_id;
    private String email;
    private String username;
    private String name;
    private String description;
    private String sex;
    private int birth_year;

    public User() {
        this.user_id = "";
        this.email = "";
        this.username = "";
        this.name = "";
        this.sex = "";
        setZero();
    }

    public User(String user_id, String email) {
        this.user_id = user_id;
        this.email = email;
        this.username = "";
        this.name = "";
        this.sex = "";
        setZero();
    }

    public User(String user_id, String email, String username) {
        this.user_id = user_id;
        this.email = email;
        this.username = username;
        this.name = username;
        this.sex = "";
        setZero();
    }

    private void setZero(){
        this.birth_year = 1995;
        this.description = "Description";
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
