package com.example.chatss.models;

import java.io.Serializable;

public class UserGroup implements Serializable {
    public String id,checked,name, email, image, token;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChecked() {
        return checked;
    }

    public void setChecked(String checked) {
        this.checked = checked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserGroup() {
    }

    public UserGroup(String id, String checked, String name, String email, String image, String token) {
        this.id = id;
        this.checked = checked;
        this.name = name;
        this.email = email;
        this.image = image;
        this.token = token;
    }
}
