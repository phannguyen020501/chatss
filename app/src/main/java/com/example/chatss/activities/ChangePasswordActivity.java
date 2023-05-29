package com.example.chatss.activities;

import static com.example.chatss.R.color.black;
import static com.example.chatss.utilities.Constants.hideSoftKeyboard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chatss.R;
import com.example.chatss.databinding.ActivityChangePasswordBinding;
import com.example.chatss.databinding.ActivityProfileBinding;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class ChangePasswordActivity extends BaseActivity {

    private ActivityChangePasswordBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    private DocumentReference documentReference ;
    private String currPass = null;
    private String newPass = null;
    private FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI(binding.activityChangePasswordLayout);

        preferenceManager = new PreferenceManager((getApplicationContext()));
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        documentReference =
                db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USED_ID));
        getCurrPass();
        setListener();
        onEditTextStatusChange();
    }

    private void onChangePassPressed(){
        loading(true);
//        documentReference.update(Constants.KEY_PASSWORD,newPass)
//                .addOnSuccessListener(v -> {
//                    loading(false);
//                    showToast("Change Password success");
//                    onBackPressed();
//                })
//                .addOnFailureListener(e -> {
//                    loading(false);
//                    showToast(e.getMessage());
//                    binding.inputNewPassword.getText().clear();
//                    binding.inputConfirmPassword.getText().clear();
//                    binding.inputCurrentPass.getText().clear();
//                    showToast("Change Password failure. Please try again!!");
//                });
        firebaseAuth.getCurrentUser().updatePassword(newPass)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Xử lý khi thay đổi mật khẩu thành công
                            loading(false);
                            showToast("Change Password success");
                            onBackPressed();
                        } else {
                            // Xử lý khi thay đổi mật khẩu không thành công
                            loading(false);
                            showToast(task.getResult().toString());
                            binding.inputNewPassword.getText().clear();
                            binding.inputConfirmPassword.getText().clear();
                            binding.inputCurrentPass.getText().clear();
                            showToast("Change Password failure. Please try again!!");
                        }
                    }
                });
    }

    private void getCurrPass(){
//        documentReference
//                .get()
//                .addOnCompleteListener(task -> {
//                    if(task.isSuccessful() && task.getResult() != null ){
//                        DocumentSnapshot document = task.getResult();
//                        if (document != null && document.exists()) {
//                            if (document.getString(Constants.KEY_PASSWORD) != null) {
//                                currPass = document.getString(Constants.KEY_PASSWORD);
//                            }
//                        } else {
//                            Log.d("TAG", "User not exist!");
//                        }
//                    } else {
//                        showToast("Unable to get current password");
//                    }
//                });
        System.out.println("curren password");
        currPass = preferenceManager.getString(Constants.KEY_PASSWORD);
        System.out.println(currPass);
    }
    private void onEditTextStatusChange(){
        int colorFocus = ContextCompat.getColor(getApplicationContext(), R.color.primary_text);
        int colorDefault = ContextCompat.getColor(getApplicationContext(), R.color.secondary_text);
        binding.inputCurrentPass.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    binding.inputCurrentPass.setBackgroundResource(R.drawable.background_input_good);
                    binding.inputCurrentPass.setHintTextColor(colorFocus);
                }
                else {
                    binding.inputCurrentPass.setBackgroundResource(R.drawable.background_input);
                    binding.inputCurrentPass.setHintTextColor(colorDefault);
//                    if (binding.inputCurrentPass.getText().toString().isEmpty()) {
//                        binding.inputCurrentPass.setBackgroundResource(R.drawable.background_input_wrong);
//                    } else {
//                        binding.inputCurrentPass.setBackgroundResource(R.drawable.background_input_good);
//                    }
                }
            }
        });
        binding.inputNewPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    binding.inputNewPassword.setBackgroundResource(R.drawable.background_input_good);
                    binding.inputNewPassword.setHintTextColor(colorFocus);
                }
                else {
                    binding.inputNewPassword.setBackgroundResource(R.drawable.background_input);
                    binding.inputNewPassword.setHintTextColor(colorDefault);
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
    private Boolean isValidPassDetails(){
        int colorEror = ContextCompat.getColor(this, R.color.error);
        if(binding.inputCurrentPass.getText().toString().trim().isEmpty()){
            binding.inputCurrentPass.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputCurrentPass.setHintTextColor(colorEror);
            showToast("Enter your current password");
            return false;
        }else if(binding.inputNewPassword.getText().toString().trim().isEmpty()){
            binding.inputNewPassword.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputNewPassword.setHintTextColor(colorEror);
            showToast("Enter your new password");
            return false;
        }else if(binding.inputConfirmPassword.getText().toString().isEmpty()){
            binding.inputConfirmPassword.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputConfirmPassword.setHintTextColor(colorEror);
            showToast("Confirm your new password");
            return false;
        }else if(!binding.inputNewPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())){
            binding.inputNewPassword.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputNewPassword.setHintTextColor(colorEror);
            binding.inputConfirmPassword.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputConfirmPassword.setHintTextColor(colorEror);
            showToast("Password and confirm password must be same");
            return false;
        }else if (binding.inputCurrentPass.getText().toString().equals(binding.inputNewPassword.getText().toString())){
            binding.inputNewPassword.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputNewPassword.setHintTextColor(colorEror);
            showToast("New password is same with current pass");
            return false;
        }else if (!binding.inputCurrentPass.getText().toString().equals(currPass)) {
            binding.inputCurrentPass.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputCurrentPass.setHintTextColor(colorEror);
            showToast("Current password is not correct");
            return false;
        }
        else{
            return true;
        }

    }
    private void loading(boolean isLoading) {
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
    private void showToast (String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void setListener(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.buttonChangePass.setOnClickListener(view -> {
            if (isValidPassDetails()){
                newPass = binding.inputNewPassword.getText().toString().trim();
                onChangePassPressed();
            }
        });
    }
}