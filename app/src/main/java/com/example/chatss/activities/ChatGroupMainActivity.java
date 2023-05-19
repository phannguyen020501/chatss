package com.example.chatss.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.chatss.adapter.ChatRoomAdapter;
import com.example.chatss.adapter.RecentConversationsAdapter;
import com.example.chatss.adapter.UsersAdapter;
import com.example.chatss.databinding.ActivityChatGroupMainBinding;
import com.example.chatss.databinding.ActivityMainBinding;
import com.example.chatss.listeners.ConversionListener;
import com.example.chatss.listeners.RoomChatListener;
import com.example.chatss.listeners.UserListener;
import com.example.chatss.models.ChatMessage;
import com.example.chatss.models.RoomChat;
import com.example.chatss.models.User;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ChatGroupMainActivity extends BaseActivity implements RoomChatListener {
    private ActivityChatGroupMainBinding binding;
    private PreferenceManager preferenceManager;
    private float x1,x2;
    static final int MIN_DISTANCE = 150;
    //private FirebaseFirestore database;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatGroupMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        loadUserDetails();
        getToken();
        setListeners();
        getRooms();
    }
    public void init(){
        //database = FirebaseFirestore.getInstance();
    }
    private void loadUserDetails() {
        binding.textName.setText("Group");
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Constants.userCurrent.setId(preferenceManager.getString(Constants.KEY_USED_ID));
        Constants.userCurrent.setImage(preferenceManager.getString(Constants.KEY_IMAGE));
        Constants.userCurrent.setEmail(preferenceManager.getString(Constants.KEY_EMAIL));
        Constants.userCurrent.setName(preferenceManager.getString(Constants.KEY_NAME));
        Constants.userCurrent.setChecked("1");
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void setListeners(){
        binding.imageSignOut.setOnClickListener(v -> signOut());
        binding.fabNewChat.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), CreateGroupActivity.class));
        });

    }
    private void getRooms(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection("ListRoomUser").document(preferenceManager.getString(Constants.KEY_USED_ID)).collection("ListRoom")
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if(task.isSuccessful() && task.getResult() != null){
                        List<RoomChat> roomChats = new ArrayList<>();
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){

                            RoomChat roomChat = new RoomChat();
                            roomChat.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            roomChat.id = queryDocumentSnapshot.getString("id");
                            roomChat.lastMessage = queryDocumentSnapshot.getString("lastMessage");
                            Toast.makeText(getApplicationContext(),roomChat.lastMessage, Toast.LENGTH_SHORT).show();
                            roomChats.add(roomChat);
                        }
                        if(roomChats.size() > 0){
                            ChatRoomAdapter chatRoomAdapter = new ChatRoomAdapter(roomChats,  this);
                            binding.groupRecyclerView.setAdapter(chatRoomAdapter);
                            binding.groupRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }
    private void loading(boolean isLoading) {
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
    private void showErrorMessage() {

    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message, Toast.LENGTH_SHORT).show();
    }


    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToke);
    }

    private void updateToke(String token) {
        preferenceManager.putString(Constants.KEY_FCM_TOKEN,token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USED_ID));
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                //.addOnSuccessListener(unused -> showToast("Token updated successfully"))
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    private void signOut(){
        showToast("Signing out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
                preferenceManager.getString(Constants.KEY_USED_ID)
        );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        getRooms();
    }

    @Override
    public void onRoomChatClicked(RoomChat roomChat) {
        Intent intent = new Intent(getApplicationContext(), ChatGroupActivity.class);
        intent.putExtra(Constants.KEY_ROOM, roomChat);
        startActivity(intent);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;

                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    // Left to Right swipe action
                    if (x2 > x1)
                    {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        finish();

                    }

                    // Right to left swipe action
                    else
                    {
                        Intent intent = new Intent(getApplicationContext(), ChatGroupMainActivity.class);
                        startActivity(intent);
                        finish();

                    }

                }
                else
                {
                    // consider as something else - a screen tap for example
                }
                break;
        }
        return super.onTouchEvent(event);
    }
}