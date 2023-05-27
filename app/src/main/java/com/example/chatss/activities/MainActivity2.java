package com.example.chatss.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.chatss.R;
import com.example.chatss.adapter.ChatViewPagerAdapter;
import com.example.chatss.adapter.RecentConversationsAdapter;
import com.example.chatss.databinding.ActivityMain1Binding;
import com.example.chatss.databinding.ActivityMainBinding;
import com.example.chatss.fragment.GroupFragment;
import com.example.chatss.fragment.IndivisualFragment;
import com.example.chatss.models.ChatMessage;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity2 extends BaseActivity {

    private ActivityMain1Binding binding;
    private ChatViewPagerAdapter chatViewPagerAdapter;
    public static final int MY_REQUEST_NOTI_CODE = 0;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;
    private ListenerRegistration registration;
    private String priKeyStr;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMain1Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        chatViewPagerAdapter = new ChatViewPagerAdapter(this);
        binding.viewPaper.setAdapter(chatViewPagerAdapter);

        new TabLayoutMediator(binding.tabLayout,binding.viewPaper,(tab, position) -> {
            switch (position){
                case 0:
                    tab.setText("Chats");
                    break;
                case 1:
                    tab.setText("Groups");
                    break;
            }
        }).attach();

        askNotificationPermission();
        preferenceManager = new PreferenceManager(getApplicationContext());
        priKeyStr = preferenceManager.getString(Constants.KEY_PRIVATE_KEY);
        loadUserDetails();
        if (preferenceManager.getString(Constants.KEY_PRIVATE_KEY) == null){
            binding.imageSignOut.setOnClickListener(v -> signOut());
            return;
        }
        getToken();
        setListeners();
    }


    private void setListeners(){
        binding.imageProfile.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
        });
        binding.imageSignOut.setOnClickListener(v -> signOut());

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
            String [] permission = {Manifest.permission.POST_NOTIFICATIONS};
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
    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);

        //for group chat
        Constants.userCurrent.setId(preferenceManager.getString(Constants.KEY_USED_ID));
        Constants.userCurrent.setImage(preferenceManager.getString(Constants.KEY_IMAGE));
        Constants.userCurrent.setEmail(preferenceManager.getString(Constants.KEY_EMAIL));
        Constants.userCurrent.setName(preferenceManager.getString(Constants.KEY_NAME));
        Constants.userCurrent.setChecked("1");
    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message, Toast.LENGTH_SHORT).show();
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
        if (preferenceManager.getString(Constants.KEY_PRIVATE_KEY) == null){
            showToast("Signing out...");
            preferenceManager.clear();
            startActivity(new Intent(getApplicationContext(), SignInActivity.class));
            finish();
            return;

        }
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
        loadUserDetails();
    }
}