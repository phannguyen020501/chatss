package com.example.chatss.activities;

import static com.example.chatss.activities.SignUpActivity.encodeImage;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.chatss.adapter.UsersAdapter;
import com.example.chatss.databinding.ActivityChatGroupBinding;
import com.example.chatss.databinding.ActivitySettingGroupBinding;
import com.example.chatss.listeners.UserListener;
import com.example.chatss.models.RoomChat;
import com.example.chatss.models.User;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SettingGroupActivity extends BaseActivity implements UserListener {
    private ActivitySettingGroupBinding binding;
    private PreferenceManager preferenceManager;
    private RoomChat roomChatCurrent = new RoomChat();
    private FirebaseFirestore db;

    DocumentReference documentReference ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        roomChatCurrent = (RoomChat) getIntent().getSerializableExtra(Constants.KEY_ROOM);
        setupUI(binding.profileActivityLayout);

        preferenceManager = new PreferenceManager((getApplicationContext()));
        db = FirebaseFirestore.getInstance();
        documentReference = db.collection("RoomChat").document(roomChatCurrent.getId());
        setListener();
        showUserInfo();
        getUsers();
    }

    private void setListener(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.include.itemEmail.icEdit.setOnClickListener(v -> onEditEmailPressed());
        binding.include.itemEmail.btnOK.setOnClickListener(v -> {
            if (isValidEmail(binding.include.itemEmail.editTextEmail.getText().toString().trim())){
                onOKEmailPressed();
            }
        });
        binding.include.itemEmail.btnCancel.setOnClickListener(v -> onCancelEmailPressed());

        binding.include.updateProfileImg.setOnClickListener(view -> onUpdateProfileImgPressed());
        binding.include.imageProfile.setOnClickListener(view -> {

        });


    }



    private void onUpdateProfileImgPressed() {
        ImagePicker.with(this)
                .crop()	    			//Crop image(Optional), Check Customization for more option
                .compress(1024)			//Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            //Image Uri will not be null for RESULT_OK
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                // Use Uri object instead of File to avoid storage permissions

                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    String encodedImage = encodeImage(bitmap);
                    Uri finalUri = uri;
                    documentReference.update("image", encodedImage)
                            .addOnSuccessListener(unused -> {
                                binding.include.imageProfile.setImageURI(finalUri);
                            })
                            .addOnFailureListener(e -> {
                                showToast("Update image false!!");
                            });
                }catch (FileNotFoundException e ){
                    e.printStackTrace();
                }

            }
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Task Update Cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUserInfo(){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection("RoomChat")
                .whereEqualTo("id", roomChatCurrent.id)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() != null){
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){

                            if(queryDocumentSnapshot.getString("image") != null){
                                byte[] bytes = Base64.decode(queryDocumentSnapshot.getString("image"), Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                binding.include.imageProfile.setImageBitmap(bitmap);
                            }
                            if(queryDocumentSnapshot.getString("name") != null){
                                binding.include.itemEmail.textEmail.setText(queryDocumentSnapshot.getString("name"));
                            }

                        }
                    }
                    else {

                    }
                });


    }



    private void showToast (String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }



    private void onEditEmailPressed(){
        TransitionManager.beginDelayedTransition(binding.profileActivityLayout, new AutoTransition());
        binding.include.itemEmail.textEmail.setVisibility(View.GONE);
        binding.include.itemEmail.icEdit.setVisibility(View.GONE);
        binding.include.itemEmail.editTextEmail.setVisibility(View.VISIBLE);
        binding.include.itemEmail.containerBtn.setVisibility(View.VISIBLE);
    }
    private void onOKEmailPressed(){
        String textEmail = binding.include.itemEmail.editTextEmail.getText().toString().trim();

        documentReference.update("name",textEmail)
                .addOnSuccessListener(v -> {
                    preferenceManager.putString(Constants.KEY_EMAIL, textEmail);

                    binding.include.itemEmail.textEmail.setText(textEmail);

                    TransitionManager.beginDelayedTransition(binding.profileActivityLayout, new AutoTransition());
                    binding.include.itemEmail.editTextEmail.setVisibility(View.GONE);
                    binding.include.itemEmail.containerBtn.setVisibility(View.GONE);

                    binding.include.itemEmail.textEmail.setVisibility(View.VISIBLE);
                    binding.include.itemEmail.icEdit.setVisibility(View.VISIBLE);


                })
                .addOnFailureListener(e -> {
                    showToast("Update name group fail! Please try again!!!");
                });
    }
    private void onCancelEmailPressed(){
        TransitionManager.beginDelayedTransition(binding.profileActivityLayout, new AutoTransition());
        binding.include.itemEmail.textEmail.setVisibility(View.VISIBLE);
        binding.include.itemEmail.icEdit.setVisibility(View.VISIBLE);
        binding.include.itemEmail.editTextEmail.setVisibility(View.GONE);
        binding.include.itemEmail.containerBtn.setVisibility(View.GONE);
    }

    private Boolean isValidEmail(String textEmail){
        if (textEmail.isEmpty()){
            showToast("Enter your name group first!");
            return false;
        }
        return true;
    }

    private void getUsers(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection("Participants").document(roomChatCurrent.getId()).collection("Users")
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USED_ID);
                    if(task.isSuccessful() && task.getResult() != null){
                        List<User> users = new ArrayList<>();
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
                            User user = new User();
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            users.add(user);
                        }
                        if(users.size() > 0){
                            Toast.makeText(getApplicationContext(), String.valueOf(users.size()), Toast.LENGTH_SHORT).show();
                            UsersAdapter usersAdapter = new UsersAdapter(users,this);
                            binding.usersRecyclerView.setAdapter(usersAdapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {

                        }
                    } else {

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

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
}