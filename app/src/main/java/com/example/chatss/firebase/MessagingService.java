package com.example.chatss.firebase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.chatss.BuildConfig;
import com.example.chatss.ECC.ECCc;
import com.example.chatss.R;
import com.example.chatss.activities.ChatActivity;
import com.example.chatss.activities.ChatGroupActivity;
import com.example.chatss.models.RoomChat;
import com.example.chatss.models.User;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Random;

import javax.crypto.SecretKey;

public class MessagingService extends FirebaseMessagingService {
    public static String channelId = "chat_message";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "Token: " + token);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> data = message.getData();

        // Kiểm tra các trường dữ liệu tùy chỉnh để phân biệt các loại thông báo
        if (data.containsKey("Type")) {
            String notificationType = data.get("Type");
            if (notificationType.equals("Group")) {
                // Xử lý thông báo loại nhóm
                RoomChat roomChat = new RoomChat();
                roomChat.id = message.getData().get("id");
                roomChat.name = message.getData().get(Constants.KEY_NAME);


                int notificationId = new Random().nextInt();

                // Create an Intent for the activity you want to start
                Intent resultIntent = new Intent(this, ChatGroupActivity.class);
                resultIntent.putExtra(Constants.KEY_ROOM, roomChat);
                // Create the TaskStackBuilder and add the intent, which inflates the back stack
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addParentStack(ChatGroupActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                // Get the PendingIntent containing the entire back stack
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


//        Intent intent = new Intent(this, ChatActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        intent.putExtra(Constants.KEY_USER, user);
//        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent, 0);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
                builder.setSmallIcon(R.mipmap.logo)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.logo));
                builder.setContentTitle(roomChat.name);

                builder.setContentText(data.get(Constants.KEY_MESSAGE)) ;
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(
                        data.get(Constants.KEY_MESSAGE)
                ));
                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                builder.setContentIntent(resultPendingIntent);
                builder.setAutoCancel(true);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                notificationManager.notify(notificationId, builder.build());
            } else if (notificationType.equals("Indivisual")) {
                // Xử lý thông báo loại cá nhân
                User user = new User();
                user.id = message.getData().get(Constants.KEY_USED_ID);
                user.name = message.getData().get(Constants.KEY_NAME);
                user.token = message.getData().get(Constants.KEY_FCM_TOKEN);
                user.publicKey = message.getData().get(Constants.KEY_PUBLIC_KEY);

                int notificationId = new Random().nextInt();

                // Create an Intent for the activity you want to start
                Intent resultIntent = new Intent(this, ChatActivity.class);
                resultIntent.putExtra(Constants.KEY_USER, user);
                // Create the TaskStackBuilder and add the intent, which inflates the back stack
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addParentStack(ChatActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                // Get the PendingIntent containing the entire back stack
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


//        Intent intent = new Intent(this, ChatActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        intent.putExtra(Constants.KEY_USER, user);
//        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent, 0);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
                builder.setSmallIcon(R.mipmap.logo);
                builder.setContentTitle(user.name);

                //Giải mã văn bản
                String plainMess = "";
                try {
                    String priKeyStr = new PreferenceManager(getApplicationContext()).getString(Constants.KEY_PRIVATE_KEY);
                    PrivateKey priKey = ECCc.stringToPrivateKey(priKeyStr);
                    PublicKey pubKey = ECCc.stringToPublicKey(user.publicKey);
                    SecretKey secretKey = ECCc.generateSharedSecret(priKey, pubKey);
                    plainMess = ECCc.decryptString(secretKey,message.getData().get(Constants.KEY_MESSAGE));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                builder.setContentText(plainMess) ;
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(
                        plainMess
                ));
                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                builder.setContentIntent(resultPendingIntent);
                builder.setAutoCancel(true);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                notificationManager.notify(notificationId, builder.build());
            }
        }


    }
}
