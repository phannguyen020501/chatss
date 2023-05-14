package com.example.chatss.models;

import java.io.Serializable;

public class UserGroup implements Serializable {
    public String id,checked;

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

    public UserGroup() {
    }

    public UserGroup(String id, String checked) {
        this.id = id;
        this.checked = checked;
    }
}
