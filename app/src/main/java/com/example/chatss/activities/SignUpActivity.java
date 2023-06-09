package com.example.chatss.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

import com.example.chatss.R;
import com.example.chatss.databinding.ActivitySignInBinding;
import com.example.chatss.databinding.ActivitySignUpBinding;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.Contract;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {
    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;

    FirebaseFirestore db;
    Boolean isExistEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        db = FirebaseFirestore.getInstance();
        onEditTextStatusChange();
        setListeners();

    }

    private void setListeners(){
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.buttonSignUp.setOnClickListener(v-> {
            if(isValidSignUpDetails()){
                loading(true);
                db.collection(Constants.KEY_COLLECTION_USERS).whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                QuerySnapshot querySnapshot = task.getResult();

                                // Email đã tồn tại
                                // Email không tồn tại
                                if(querySnapshot != null && !querySnapshot.isEmpty()){
                                    loading(false);
                                    binding.inputEmail.setBackgroundResource(R.drawable.background_input_wrong);
                                    int colorEror = ContextCompat.getColor(this, R.color.error);
                                    binding.inputEmail.setHintTextColor(colorEror);
                                    showToast("Email is exist. Please change other email!");
                                }else {
                                    signUp();
                                }
                            } else {
                                // Xử lý lỗi truy vấn
                                loading(false);
                                showToast("Error checking email");
                            }
                        });

            }
        });
        binding.layoutImage.setOnClickListener(v ->{
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);

        });

    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp(){
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.inputName.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString());
        user.put(Constants.KEY_IMAGE, encodedImage);
        db.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USED_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME,binding.inputName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    preferenceManager.putString(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                })
                .addOnFailureListener(exception ->{
                    loading(false);
                    showToast(exception.getMessage());
                });

    }

    public static String encodeImage(Bitmap bitmap){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight()*previewWidth/bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getEncoder().encodeToString(bytes);
        }else{
            return null;
        }
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result->{
                if(result.getResultCode() == RESULT_OK){
                    if(result.getData() != null){
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        }catch (FileNotFoundException e ){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private Boolean isValidSignUpDetails(){
        int colorEror = ContextCompat.getColor(this, R.color.error);
        if(encodedImage == null){
            showToast("select profile image");
            return false;
        }else if(binding.inputName.getText().toString().trim().isEmpty()){
            binding.inputName.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputName.setHintTextColor(colorEror);
            showToast("Enter name");
            return false;
        }else if(binding.inputEmail.getText().toString().trim().isEmpty()){
            binding.inputEmail.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputEmail.setHintTextColor(colorEror);
            showToast("Enter email");
            return false;
        }else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            binding.inputEmail.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputEmail.setHintTextColor(colorEror);
            showToast("Enter valid email");
            return false;
        }else if(binding.inputPassword.getText().toString().isEmpty()){
            binding.inputPassword.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputPassword.setHintTextColor(colorEror);
            showToast("Enter password");
            return false;
        }else if(binding.inputConfirmPassword.getText().toString().isEmpty()){
            binding.inputConfirmPassword.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputConfirmPassword.setHintTextColor(colorEror);
            showToast("Confirm your password");
            return false;
        }else if(!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())){
            binding.inputPassword.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputPassword.setHintTextColor(colorEror);
            binding.inputConfirmPassword.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputConfirmPassword.setHintTextColor(colorEror);
            showToast("Password and confirm password must be same");
            return false;
        }else{
            return true;
        }
    }
    private void onEditTextStatusChange(){
        int colorFocus = ContextCompat.getColor(getApplicationContext(), R.color.primary_text);
        int colorDefault = ContextCompat.getColor(getApplicationContext(), R.color.secondary_text);
        binding.inputName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    binding.inputName.setBackgroundResource(R.drawable.background_input_good);
                    binding.inputName.setHintTextColor(colorFocus);
                }
                else {
                    binding.inputName.setBackgroundResource(R.drawable.background_input);
                    binding.inputName.setHintTextColor(colorDefault);
//                    if (binding.inputCurrentPass.getText().toString().isEmpty()) {
//                        binding.inputCurrentPass.setBackgroundResource(R.drawable.background_input_wrong);
//                    } else {
//                        binding.inputCurrentPass.setBackgroundResource(R.drawable.background_input_good);
//                    }
                }
            }
        });
        binding.inputEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    binding.inputEmail.setBackgroundResource(R.drawable.background_input_good);
                    binding.inputEmail.setHintTextColor(colorFocus);
                }
                else {
                    binding.inputEmail.setBackgroundResource(R.drawable.background_input);
                    binding.inputEmail.setHintTextColor(colorDefault);
                }
            }
        });
        binding.inputPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    binding.inputPassword.setBackgroundResource(R.drawable.background_input_good);
                    binding.inputPassword.setHintTextColor(colorFocus);
                }
                else {
                    binding.inputPassword.setBackgroundResource(R.drawable.background_input);
                    binding.inputPassword.setHintTextColor(colorDefault);
                }
            }
        });
        binding.inputConfirmPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    binding.inputConfirmPassword.setBackgroundResource(R.drawable.background_input_good);
                    binding.inputConfirmPassword.setHintTextColor(colorFocus);
                }
                else {
                    binding.inputConfirmPassword.setBackgroundResource(R.drawable.background_input);
                    binding.inputConfirmPassword.setHintTextColor(colorDefault);
                }
            }
        });
    }
    private Boolean isExistEmail(){

        db.collection(Constants.KEY_COLLECTION_USERS).whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                // Email đã tồn tại
                // Email không tồn tại
                isExistEmail = querySnapshot != null && !querySnapshot.isEmpty();
            } else {
                // Xử lý lỗi truy vấn
                isExistEmail = null;
                showToast("Error checking email");
            }
        });

        return isExistEmail != null && isExistEmail;
    }
    private void loading(boolean isLoading){
        if(isLoading){
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.buttonSignUp.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

}