package com.example.chatss.utilities;

import java.util.HashMap;

public class Constants {
    public static final String KEY_COLLECTION_USERS = "users";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_PREFERENCE_NAME = "chatAppPreference";
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";
    public static final String KEY_USED_ID = "userId";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String KEY_USER = "user";
    public static final String KEY_COLLECTION_CHAT = "chat";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_RECEIVER_ID = "receiverId";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_COLLECTION_CONVERSATIONS = "conversations";
    public static final String KEY_SENDER_NAME = "senderName";
    public static final String KEY_RECEIVER_NAME = "receiverName";
    public static final String KEY_SENDER_IMAGE = "senderImage";
    public static final String KEY_RECEIVER_IMAGE = "receiverImage";
    public static final String KEY_LAST_MESSAGE = "lastMessage";
    public static final String KEY_AVAILABILITY = "availability";
    public static final String REMOTE_MSG_AUTHORIZATION = "Authorization";

    public static final String REMOTE_MSG_CONTENT_TYPE = "Content-type";

    public static final String REMOTE_MSG_DATA = "data";

    public static final String TYPE_MESSAGES_SEND = "type";

    public static final String REMOTE_MSG_REGISTRATION_IDS = "registration_ids";

    public static HashMap<String, String> remotoMsgHeaders = null;

    public static HashMap<String, String> getRemoteMsgHeaders(){
        if(remotoMsgHeaders == null){
            remotoMsgHeaders = new HashMap<>();
            remotoMsgHeaders.put(
                    REMOTE_MSG_AUTHORIZATION,
                    "key=AAAANdlZ-WY:APA91bFJJ0vstI2-8E-OQeelSbg45jjWBrUT4vyVeGCb1-nEaqjqMuCspO0rBPL-e5EmbS9gD0ybmXfFyr4VUb6lnPgz0b1LcMYDZMF68D8KTaL4jDrIkLaNlayJ7Pj0oOYUAJIc7N1m"
            );
            remotoMsgHeaders.put(
                    REMOTE_MSG_CONTENT_TYPE,
                    "application/json"
            );
        }
        return remotoMsgHeaders;
    }

}
