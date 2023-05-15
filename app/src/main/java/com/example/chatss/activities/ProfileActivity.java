package com.example.chatss.activities;

import static com.example.chatss.activities.SignUpActivity.encodeImage;
import static com.example.chatss.firebase.MessagingService.channelId;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.example.chatss.R;
import com.example.chatss.databinding.ActivityProfileBinding;
import com.example.chatss.models.User;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.FileNotFoundException;
import java.io.InputStream;


public class ProfileActivity extends BaseActivity {
    private ActivityProfileBinding binding;

    private PreferenceManager preferenceManager;

    private FirebaseFirestore db;

    DocumentReference documentReference ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager((getApplicationContext()));
        db = FirebaseFirestore.getInstance();
        documentReference =
                db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USED_ID));
        setListener();
        showUserInfo();
    }

    private void setListener(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.itemInfo.btnAdd.setOnClickListener(v -> onAddPhoneNumberPressed());
        binding.itemInfo.btnOK.setOnClickListener(v -> {
            if (isValidPhone(binding.itemInfo.editTextPhoneNumber.getText().toString().trim())){
                onOKPhoneNumberPressed();
            }
        });
        binding.itemInfo.btnHide.setOnClickListener(v -> onCancelPhoneNumberPressed());
        binding.itemEmail.icEdit.setOnClickListener(v -> onEditEmailPressed());
        binding.itemEmail.btnOK.setOnClickListener(v -> {
            if (isValidEmail(binding.itemEmail.editTextEmail.getText().toString().trim())){
                onOKEmailPressed();
            }
        });
        binding.itemEmail.btnCancel.setOnClickListener(v -> onCancelEmailPressed());
        binding.itemInfo.btnCancel.setOnClickListener(v -> onCancelPhoneEditPressed());
        binding.itemInfo.icEdit.setOnClickListener(v -> onEditPhonePressed());
        binding.include.btnChangePass.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), ChangePasswordActivity.class));
        });
        binding.include.updateProfileImg.setOnClickListener(view -> onUpdateProfileImgPressed());

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
                    documentReference.update(Constants.KEY_IMAGE, encodedImage)
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
        if(preferenceManager.getString(Constants.KEY_IMAGE) != null){
            byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            binding.include.imageProfile.setImageBitmap(bitmap);
        }

        if(preferenceManager.getString(Constants.KEY_EMAIL) != null){
            binding.itemEmail.textEmail.setText(preferenceManager.getString(Constants.KEY_EMAIL));
        }
        if(preferenceManager.getString(Constants.KEY_NAME) != null){
            binding.include.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        }
        if(preferenceManager.getString(Constants.KEY_PHONE) != null){

            binding.itemInfo.btnAdd.setVisibility(View.GONE);
            binding.itemInfo.btnHide.setVisibility(View.GONE);

            binding.itemInfo.textPhone.setText(preferenceManager.getString(Constants.KEY_PHONE));
        }else {
            binding.itemInfo.icEdit.setVisibility(View.GONE);
            binding.itemInfo.btnHide.setVisibility(View.GONE);
            binding.itemInfo.textPhone.setVisibility(View.GONE);
        }
        if(preferenceManager.getString(Constants.KEY_ADDRESS_CITY) != null ){
            binding.itemAdd.btnAdd.setVisibility(View.GONE);
            binding.itemAdd.textCity.setText(preferenceManager.getString(Constants.KEY_ADDRESS_CITY));
            if (preferenceManager.getString(Constants.KEY_ADDRESS_PROVINCE) != null){
                binding.itemAdd.textAddressProvince.setVisibility(View.VISIBLE);
                binding.itemAdd.textAddressProvince.setText(preferenceManager.getString(Constants.KEY_ADDRESS_PROVINCE));
            }
            if (preferenceManager.getString(Constants.KEY_ADDRESS_TOWN) != null){
                binding.itemAdd.textAddressTown.setVisibility(View.VISIBLE);
                binding.itemAdd.textAddressTown.setText(preferenceManager.getString(Constants.KEY_ADDRESS_TOWN));
            }
            if (preferenceManager.getString(Constants.KEY_ADDRESS_STREET) != null){
                binding.itemAdd.textAddressStreet.setVisibility(View.VISIBLE);
                binding.itemAdd.textAddressStreet.setText(preferenceManager.getString(Constants.KEY_ADDRESS_STREET));
            }
            if (preferenceManager.getString(Constants.KEY_ADDRESS_NUMBER) != null){
                binding.itemAdd.textAddressNumber.setVisibility(View.VISIBLE);
                binding.itemAdd.textAddressNumber.setText(preferenceManager.getString(Constants.KEY_ADDRESS_NUMBER));
            }
        }else {
            binding.itemAdd.icEdit.setVisibility(View.GONE);
            binding.itemAdd.containerAddress.setVisibility(View.GONE);
        }
    }


    private void onAddPhoneNumberPressed(){

//        TransitionManager.beginDelayedTransition(binding.itemInfo.cardview, new AutoTransition());
        binding.itemInfo.btnAdd.setVisibility(View.GONE);
        binding.itemInfo.btnHide.setVisibility(View.VISIBLE);
        TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());
        binding.itemInfo.editTextPhoneNumber.setVisibility(View.VISIBLE);
        binding.itemInfo.containerBtn.setVisibility(View.VISIBLE);
//        binding.itemInfo.container.startAnimation(slideUp);

    }

    private void onOKPhoneNumberPressed(){

        String textPhone = binding.itemInfo.editTextPhoneNumber.getText().toString().trim();

        documentReference.update(Constants.KEY_PHONE,textPhone)
                .addOnSuccessListener(v->{
                    preferenceManager.putString(Constants.KEY_PHONE, textPhone);
                    binding.itemInfo.btnHide.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());
                    binding.itemInfo.editTextPhoneNumber.setVisibility(View.GONE);
                    binding.itemInfo.containerBtn.setVisibility(View.GONE);
                    binding.itemInfo.btnAdd.setVisibility(View.GONE);
                    binding.itemInfo.textPhone.setText(textPhone);
                    binding.itemInfo.textPhone.setVisibility(View.VISIBLE);
                    binding.itemInfo.icEdit.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    showToast("Update Phone Number fail. Please try again!!");
                });

    }

    private Boolean isValidPhone(String textPhone){
        if (textPhone.isEmpty()){
            showToast("Enter your phone first!");
            return false;
        }else if(!Patterns.PHONE.matcher(textPhone).matches()){
            showToast("Enter valid phone number");
            return false;
        }
        return true;
    }
    private void onCancelPhoneNumberPressed(){

        binding.itemInfo.btnHide.setVisibility(View.GONE);
        binding.itemInfo.btnAdd.setVisibility(View.VISIBLE);
        TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());
        binding.itemInfo.editTextPhoneNumber.setVisibility(View.GONE);
        binding.itemInfo.containerBtn.setVisibility(View.GONE);
    }

    private void onEditEmailPressed(){
        TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());
        binding.itemEmail.textEmail.setVisibility(View.GONE);
        binding.itemEmail.icEdit.setVisibility(View.GONE);
        binding.itemEmail.editTextEmail.setVisibility(View.VISIBLE);
        binding.itemEmail.containerBtn.setVisibility(View.VISIBLE);
    }
    private void onOKEmailPressed(){
        String textEmail = binding.itemEmail.editTextEmail.getText().toString().trim();

        documentReference.update(Constants.KEY_EMAIL,textEmail)
                .addOnSuccessListener(v -> {
                    preferenceManager.putString(Constants.KEY_EMAIL, textEmail);

                    binding.itemEmail.textEmail.setText(textEmail);

                    TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());
                    binding.itemEmail.editTextEmail.setVisibility(View.GONE);
                    binding.itemEmail.containerBtn.setVisibility(View.GONE);

                    binding.itemEmail.textEmail.setVisibility(View.VISIBLE);
                    binding.itemEmail.icEdit.setVisibility(View.VISIBLE);


                })
                .addOnFailureListener(e -> {
                    showToast("Update Email fail! Please try again!!!");
                });
    }
    private void onCancelEmailPressed(){
        TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());
        binding.itemEmail.textEmail.setVisibility(View.VISIBLE);
        binding.itemEmail.icEdit.setVisibility(View.VISIBLE);
        binding.itemEmail.editTextEmail.setVisibility(View.GONE);
        binding.itemEmail.containerBtn.setVisibility(View.GONE);
    }
    private void onEditPhonePressed(){
        binding.itemInfo.btnCancel.setVisibility(View.VISIBLE);
        TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());
        binding.itemInfo.textPhone.setVisibility(View.GONE);
        binding.itemInfo.icEdit.setVisibility(View.GONE);
        binding.itemInfo.editTextPhoneNumber.setVisibility(View.VISIBLE);
        binding.itemInfo.containerBtn.setVisibility(View.VISIBLE);


    }

    private void onCancelPhoneEditPressed(){
        binding.itemInfo.editTextPhoneNumber.setVisibility(View.GONE);
        binding.itemInfo.btnCancel.setVisibility(View.GONE);
        TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());
        binding.itemInfo.textPhone.setVisibility(View.VISIBLE);
        binding.itemInfo.icEdit.setVisibility(View.VISIBLE);

        binding.itemInfo.containerBtn.setVisibility(View.GONE);

    }

    private Boolean isValidEmail(String textEmail){
        if (textEmail.isEmpty()){
            showToast("Enter your email first!");
            return false;
        }else if(!Patterns.EMAIL_ADDRESS.matcher(textEmail).matches()){
            showToast("Enter a valid email ");
            return false;
        }
        return true;
    }

    private void showToast (String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();;
    }
    private void loading(boolean isLoading) {
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
}