package com.example.chatss.activities;

import androidx.appcompat.app.AppCompatActivity;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.chatss.adapter.RecentConversationsAdapter;
import com.example.chatss.databinding.ActivityMainBinding;
import com.example.chatss.listeners.ConversionListener;
import com.example.chatss.models.ChatMessage;
import com.example.chatss.models.User;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class MainActivity extends BaseActivity implements ConversionListener {
    private float x1,x2;
    public static final int MY_REQUEST_NOTI_CODE = 0;
    static final int MIN_DISTANCE = 150;
    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        askNotificationPermission();
        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        loadUserDetails();
        getToken();
        setListeners();
        listenConversations();
        listenUserOnline();
    }
    // Declare the launcher at the top of your Activity/Fragment:
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // FCM SDK (and your app) can post notifications.
                } else {
                    // TODO: Inform user that that your app will not show notifications.
                }
            });

    private void askNotificationPermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU){

            return;
        }
        // This is only necessary for API level >= 33 (TIRAMISU)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // Directly ask for the permission
            String [] permission = {Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this,permission,MY_REQUEST_NOTI_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_REQUEST_NOTI_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            }
        }
    }

    public void init(){
        conversations = new ArrayList<>();
        conversationsAdapter = new RecentConversationsAdapter(conversations, this);
        binding.conversationRecyclerView.setAdapter(conversationsAdapter);
        database = FirebaseFirestore.getInstance();
    }
    private void setListeners(){
        binding.imageProfile.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
        });
        binding.imageSignOut.setOnClickListener(v -> signOut());
        binding.fabNewChat.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), UsersActivity.class));
        });

    }
    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }
    
    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message, Toast.LENGTH_SHORT).show();
    }

    private void listenUserOnline(){
        registration = database.collection(Constants.KEY_COLLECTION_USERS)
                .addSnapshotListener(eventListener1);
    }

    private  final EventListener<QuerySnapshot> eventListener1 = (value, error) -> {
        if (error != null){
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                String userId = documentChange.getDocument().getId();
                if (preferenceManager.getString(Constants.KEY_USED_ID) != null){
                    if (preferenceManager.getString(Constants.KEY_USED_ID).equals(userId)){
                        break;
                    }
                }

                if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < conversations.size(); i++) {
                        if (conversations.get(i).conversionId.equals(userId)) {

                            if (documentChange.getDocument().getLong(Constants.KEY_AVAILABILITY) != null){
                                conversations.get(i).avaibility = Objects.requireNonNull(
                                        documentChange.getDocument().getLong(Constants.KEY_AVAILABILITY)
                                ).intValue();

                            }
                            if (documentChange.getDocument().getString(Constants.KEY_IMAGE)!= null){
                                conversations.get(i).conversionImage = documentChange.getDocument().getString(Constants.KEY_IMAGE);
                            }
                            if (documentChange.getDocument().getString(Constants.KEY_NAME)!= null){
                                conversations.get(i).conversionName = documentChange.getDocument().getString(Constants.KEY_NAME);
                            }
                            conversationsAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }

        }
    };


    private void listenConversations(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USED_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USED_ID))
                .addSnapshotListener(eventListener);

    }

    private  final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null){
            return;
        }
        if (value != null){
            for (DocumentChange documentChange: value.getDocumentChanges()){

                String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                if (documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;
                    if (preferenceManager.getString(Constants.KEY_USED_ID).equals(senderId)){
                        getInitStatusUser(receiverId ,chatMessage);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                    }else {
                        getInitStatusUser(senderId ,chatMessage);
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);

                    }
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                }else if (documentChange.getType() == DocumentChange.Type.MODIFIED){
                    for (int i = 0; i < conversations.size(); i++){
                        if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)){

                            conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            Collections.sort(conversations, (obj1,obj2)->obj2.dateObject.compareTo(obj1.dateObject));
            conversationsAdapter.notifyDataSetChanged();
            binding.conversationRecyclerView.smoothScrollToPosition(0);
            binding.conversationRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };

    private void getInitStatusUser(String userId, ChatMessage chatMessage){
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                if (document.getLong(Constants.KEY_AVAILABILITY) != null) {
                                    chatMessage.avaibility = Objects.requireNonNull(document.getLong(Constants.KEY_AVAILABILITY)).intValue();
                                }
                                if (document.getString(Constants.KEY_IMAGE) != null){
                                    chatMessage.conversionImage = document.getString(Constants.KEY_IMAGE);
                                }
                                if (document.getString(Constants.KEY_NAME) != null){
                                    chatMessage.conversionName = document.getString(Constants.KEY_NAME);
                                }
                                conversationsAdapter.notifyDataSetChanged();
                            } else {
                                Log.d("TAG", "Tài liệu không tồn tại.");
                            }
                        } else {
                            Log.d("TAG", "Lỗi khi đọc tài liệu: ", task.getException());
                        }
                    }
                });
    }
    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToke);
    }

    private void updateToke(String token) {
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USED_ID));
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                //.addOnSuccessListener(unused -> showToast("Token updated successfully"))
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    private void signOut(){
        registration.remove();
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
    public void onConversionClicked(User user) {
        Intent it = new Intent(getApplicationContext(),ChatActivity.class);
        it.putExtra(Constants.KEY_USER,user);
//        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).whereEqualTo(Constants.KEY_SENDER_ID, user.id)
//                    .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USED_ID)).get()
//                    .addOnCompleteListener(task -> {
//                        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
//                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
//                            conversationId = documentSnapshot.getId();
//                            it.putExtra(Constants.KEY_CONVERSATION_ID,conversationId);
//                        }
//                    });
        startActivity(it);
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

    @Override
    protected void onResume() {
        super.onResume();
        loadUserDetails();
    }
}