package com.example.chatss.activities;

import static com.example.chatss.utilities.Constants.hideSoftKeyboard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI(binding.activityChangePasswordLayout);

        preferenceManager = new PreferenceManager((getApplicationContext()));
        db = FirebaseFirestore.getInstance();
        documentReference =
                db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USED_ID));
        getCurrPass();
        setListener();

    }

    private void onChangePassPressed(){
        loading(true);
        documentReference.update(Constants.KEY_PASSWORD,newPass)
                .addOnSuccessListener(v -> {
                    loading(false);
                    showToast("Change Password success");
                    onBackPressed();
                })
                .addOnFailureListener(e -> {
                    loading(false);
                    showToast(e.getMessage());
                    binding.inputNewPassword.getText().clear();
                    binding.inputConfirmPassword.getText().clear();
                    binding.inputCurrentPass.getText().clear();
                    showToast("Change Password failure. Please try again!!");
                });
    }

    private void getCurrPass(){
        documentReference
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() != null ){
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            if (document.getString(Constants.KEY_PASSWORD) != null) {
                                currPass = document.getString(Constants.KEY_PASSWORD);
                            }
                        } else {
                            Log.d("TAG", "Người dùng không tồn tại.");
                        }
                    } else {
                        showToast("Unable to get curr password");
                    }
                });
    }
    private Boolean isValidSignUpDetails(){

        if(binding.inputCurrentPass.getText().toString().trim().isEmpty()){
            showToast("Enter your current password");
            return false;
        }else if(binding.inputNewPassword.getText().toString().trim().isEmpty()){
            showToast("Enter your new password");
            return false;
        }else if(binding.inputConfirmPassword.getText().toString().isEmpty()){
            showToast("Confirm your new password");
            return false;
        }else if(!binding.inputNewPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())){
            showToast("Password and confirm password must be same");
            return false;
        }else if (binding.inputCurrentPass.getText().toString().equals(binding.inputNewPassword.getText().toString())){
            showToast("New pass word is same with current pass");
            return false;
        }else if (!binding.inputCurrentPass.getText().toString().equals(currPass)) {
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
            if (isValidSignUpDetails()){
                newPass = binding.inputNewPassword.getText().toString().trim();
                onChangePassPressed();
            }
        });
    }
}