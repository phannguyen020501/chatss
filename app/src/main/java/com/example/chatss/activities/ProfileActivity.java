package com.example.chatss.activities;

import static com.example.chatss.activities.SignUpActivity.encodeImage;
import static com.example.chatss.firebase.MessagingService.channelId;
import static com.example.chatss.utilities.Constants.hideSoftKeyboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Base64;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;


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

        setupUI(binding.profileActivityLayout);

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
        binding.include.icEdit.setOnClickListener(v -> onEditNamePressed());

        binding.itemInfo.btnCancel.setOnClickListener(v -> onCancelPhoneEditPressed());
        binding.itemInfo.icEdit.setOnClickListener(v -> onEditPhonePressed());
        binding.include.btnChangePass.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), ChangePasswordActivity.class));
        });
        binding.include.updateProfileImg.setOnClickListener(view -> onUpdateProfileImgPressed());
        binding.include.imageProfile.setOnClickListener(view -> {

        });

        binding.itemAdd.btnAdd.setOnClickListener(view -> onAddAdressPressed());
        binding.itemAdd.btnOK.setOnClickListener(view -> {
            if (isValidAdress()) {
                onOKAdressPressed();
            }
        });
        binding.itemAdd.icEdit.setOnClickListener(view -> onEditAdressPressed());
        binding.itemAdd.btnCancel.setOnClickListener(view -> onCancelAdressPressed());
        binding.itemAdd.icHide.setOnClickListener(view -> onHideAdressPressed());
    }

    private void onEditNamePressed() {

        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setTitle("Change Name");
        final EditText input = new EditText(ProfileActivity.this);
        builder.setView(input);
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog alertDialog = builder.create();

        // Ngăn người dùng đóng Alert Dialog bằng cách bấm ra bên ngoài
        alertDialog.setCanceledOnTouchOutside(false);

        // Ngăn người dùng đóng Alert Dialog bằng cách bấm nút Back
        alertDialog.setCancelable(false);
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button buttonPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty() && !Patterns.DOMAIN_NAME.matcher(input.getText().toString()).matches()) {
                            // Thay đổi tên người dùng thành newName ở đây

                            documentReference.update(Constants.KEY_NAME,input.getText().toString())
                                    .addOnSuccessListener(unused -> {
                                        preferenceManager.putString(Constants.KEY_NAME, input.getText().toString());
                                        binding.include.textName.setText(input.getText().toString());
                                        alertDialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> {
                                        showToast("Update name false, please try again!!");
                                    });

                        } else {
                            Toast.makeText(ProfileActivity.this, "Please enter a valid name", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void onAddAdressPressed() {
        binding.itemAdd.btnAdd.setVisibility(View.GONE);
        binding.itemAdd.btnCancel.setVisibility(View.GONE);
        binding.itemAdd.icHide.setVisibility(View.VISIBLE);
        TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());
        binding.itemAdd.containerEditAddres.setVisibility(View.VISIBLE);
        binding.itemAdd.containerBtn.setVisibility(View.VISIBLE);
    }
    private void onEditAdressPressed() {
        if(binding.itemAdd.btnCancel.getVisibility() == View.GONE){
            binding.itemAdd.btnCancel.setVisibility(View.VISIBLE);
        }
        binding.itemAdd.icEdit.setVisibility(View.GONE);
        TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());
        binding.itemAdd.containerAddress.setVisibility(View.GONE);
        binding.itemAdd.containerEditAddres.setVisibility(View.VISIBLE);
        binding.itemAdd.containerBtn.setVisibility(View.VISIBLE);
    }
    private void onCancelAdressPressed() {

        binding.itemAdd.icEdit.setVisibility(View.VISIBLE);
        TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());

        binding.itemAdd.containerEditAddres.setVisibility(View.GONE);
        binding.itemAdd.containerBtn.setVisibility(View.GONE);
        binding.itemAdd.containerAddress.setVisibility(View.VISIBLE);
    }
    private void onHideAdressPressed() {

        binding.itemAdd.icHide.setVisibility(View.GONE);
        binding.itemAdd.btnAdd.setVisibility(View.VISIBLE);
        TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());
        binding.itemAdd.containerEditAddres.setVisibility(View.GONE);
        binding.itemAdd.containerBtn.setVisibility(View.GONE);
    }
    private void onOKAdressPressed(){

        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_ADDRESS_CITY, binding.itemAdd.editTextCity.getText().toString());
        user.put(Constants.KEY_ADDRESS_PROVINCE, binding.itemAdd.editTextProvince.getText().toString());
        user.put(Constants.KEY_ADDRESS_TOWN, binding.itemAdd.editTextTown.getText().toString());
        user.put(Constants.KEY_ADDRESS_STREET, binding.itemAdd.editTextStreet.getText().toString());
        user.put(Constants.KEY_ADDRESS_NUMBER, binding.itemAdd.editTextNumber.getText().toString());


        documentReference.update(user)
                .addOnSuccessListener(v->{
                    preferenceManager.putString(Constants.KEY_ADDRESS_CITY, binding.itemAdd.editTextCity.getText().toString());
                    preferenceManager.putString(Constants.KEY_ADDRESS_PROVINCE, binding.itemAdd.editTextProvince.getText().toString());
                    preferenceManager.putString(Constants.KEY_ADDRESS_TOWN, binding.itemAdd.editTextTown.getText().toString());
                    preferenceManager.putString(Constants.KEY_ADDRESS_STREET, binding.itemAdd.editTextStreet.getText().toString());
                    preferenceManager.putString(Constants.KEY_ADDRESS_NUMBER, binding.itemAdd.editTextNumber.getText().toString());
                    binding.itemAdd.icHide.setVisibility(View.GONE);
                    TransitionManager.beginDelayedTransition(binding.layoutscroll, new AutoTransition());
                    binding.itemAdd.containerEditAddres.setVisibility(View.GONE);
                    binding.itemAdd.containerBtn.setVisibility(View.GONE);
                    binding.itemAdd.btnAdd.setVisibility(View.GONE);
                    binding.itemAdd.textCity.setText(binding.itemAdd.editTextCity.getText().toString());
                    binding.itemAdd.textAddressProvince.setText(binding.itemAdd.editTextProvince.getText().toString());
                    binding.itemAdd.textAddressTown.setText(binding.itemAdd.editTextTown.getText().toString());
                    binding.itemAdd.textAddressStreet.setText(binding.itemAdd.editTextStreet.getText().toString());
                    binding.itemAdd.textAddressNumber.setText(binding.itemAdd.editTextNumber.getText().toString());
                    binding.itemAdd.containerAddress.setVisibility(View.VISIBLE);
                    binding.itemAdd.icEdit.setVisibility(View.VISIBLE);
                    binding.itemAdd.editTextCity.getText().clear();
                    binding.itemAdd.editTextProvince.getText().clear();
                    binding.itemAdd.editTextTown.getText().clear();
                    binding.itemAdd.editTextStreet.getText().clear();
                    binding.itemAdd.editTextNumber.getText().clear();
                })
                .addOnFailureListener(e -> {
                    showToast("Update Phone Number fail. Please try again!!");
                });
    }

    private void onUpdateProfileImgPressed() {
        ImagePicker.with(this)
                .cropSquare()//Crop image(Optional), Check Customization for more option
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
                                preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
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
            binding.itemAdd.icHide.setVisibility(View.GONE);

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
            binding.itemAdd.icHide.setVisibility(View.GONE);
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
    private Boolean isValidAdress(){
        if (binding.itemAdd.editTextCity.getText().toString().trim().isEmpty()){
            showToast("Enter your City");
            return false;
        }else if(binding.itemAdd.editTextProvince.getText().toString().trim().isEmpty()){
            showToast("Enter your Province");
            return false;
        }else if(binding.itemAdd.editTextTown.getText().toString().trim().isEmpty()){
            showToast("Enter your Town");
            return false;
        }else if(binding.itemAdd.editTextStreet.getText().toString().trim().isEmpty()){
            showToast("Enter your Street");
            return false;
        }else if(binding.itemAdd.editTextNumber.getText().toString().trim().isEmpty()){
            showToast("Enter your No. ");
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


    private void showToast (String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void loading(boolean isLoading) {
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
}