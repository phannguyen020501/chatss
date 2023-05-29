package com.example.chatss.models;

import java.io.Serializable;
import java.util.Date;

public class RoomChat implements Serializable {
    public String id,name,lastMessage, dateTime, image;
    public Date dateObject;
    public String senderName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public RoomChat() {
    }

    public RoomChat(String id, String name, String lastMessage) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
    }

    @Override
    public String toString() {
        return "RoomChat{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", lastMessage='" + lastMessage + '\'' +
                '}';
    }
}
