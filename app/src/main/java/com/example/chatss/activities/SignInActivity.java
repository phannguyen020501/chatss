package com.example.chatss.activities;

import static com.example.chatss.utilities.KeyUtils.tryr;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.chatss.BuildConfig;
import com.example.chatss.ECC.ECCc;
import com.example.chatss.R;
import com.example.chatss.databinding.ActivitySignInBinding;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.checkerframework.checker.units.qual.C;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

public class SignInActivity extends AppCompatActivity {
    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)){
            Intent intent = new Intent(getApplicationContext(), MainActivity2.class);
            startActivity(intent);
            finish();
        }
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        onEditTextStatusChange();
        setListeners();
    }

    private void setListeners(){
        binding.textCreateNewAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class))
        );
        binding.buttonSignIn.setOnClickListener(v->{
            if(isValidSignInDetails()){
                signIn();
            }

        });
    }

    private void showToast (String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();;
    }

    private Boolean isValidSignInDetails(){
        int colorEror = ContextCompat.getColor(this, R.color.error);
        if(binding.inputEmail.getText().toString().trim().isEmpty()){
            binding.inputEmail.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputEmail.setHintTextColor(colorEror);
            showToast("Enter email");
            return false;
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            binding.inputEmail.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputEmail.setHintTextColor(colorEror);
            showToast("Enter valid email");
            return false;
        } else if(binding.inputPassword.getText().toString().trim().isEmpty()){
            binding.inputPassword.setBackgroundResource(R.drawable.background_input_wrong);
            binding.inputPassword.setHintTextColor(colorEror);
            showToast("Enter password");
            return false;
        } else {
            return true;
        }
    }
    private void onEditTextStatusChange(){
        int colorFocus = ContextCompat.getColor(getApplicationContext(), R.color.primary_text);
        int colorDefault = ContextCompat.getColor(getApplicationContext(), R.color.secondary_text);
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
    }

    private void signIn() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0 ){
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USED_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                        preferenceManager.putString(Constants.KEY_EMAIL, documentSnapshot.getString(Constants.KEY_EMAIL));
                        preferenceManager.putString(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString());
                        preferenceManager.putString(Constants.KEY_PUBLIC_KEY, documentSnapshot.getString(Constants.KEY_PUBLIC_KEY));

                        //Lấy private Key từ KeyStore
                        PrivateKey priKey = ECCc.getPrivateKeyFromKeyStore(
                                getApplicationContext(),
                                binding.inputEmail.getText().toString(),
                                binding.inputPassword.getText().toString()
                        );
                        //Lưu privateKey vào Preference cho dễ gọi lại, tăng hiệu năng máy
                        if (priKey != null){
                            try {
                                String priKeyStr = ECCc.privateKeyToString(priKey);
                                preferenceManager.putString(Constants.KEY_PRIVATE_KEY,priKeyStr);
                            } catch (IOException e) {
                                e.printStackTrace();
                                showToast("Cannot parse PrivateKey to String");
                            }
                        }


                        if (documentSnapshot.getString(Constants.KEY_PHONE) != null){
                            preferenceManager.putString(Constants.KEY_PHONE, documentSnapshot.getString(Constants.KEY_PHONE));
                        }
                        if (documentSnapshot.getString(Constants.KEY_ADDRESS_CITY) != null){
                            preferenceManager.putString(Constants.KEY_ADDRESS_CITY, documentSnapshot.getString(Constants.KEY_ADDRESS_CITY));
                        }
                        if (documentSnapshot.getString(Constants.KEY_ADDRESS_PROVINCE) != null){
                            preferenceManager.putString(Constants.KEY_ADDRESS_PROVINCE, documentSnapshot.getString(Constants.KEY_ADDRESS_PROVINCE));
                        }
                        if (documentSnapshot.getString(Constants.KEY_ADDRESS_TOWN) != null){
                            preferenceManager.putString(Constants.KEY_ADDRESS_TOWN, documentSnapshot.getString(Constants.KEY_ADDRESS_TOWN));
                        }
                        if (documentSnapshot.getString(Constants.KEY_ADDRESS_STREET) != null){
                            preferenceManager.putString(Constants.KEY_ADDRESS_STREET, documentSnapshot.getString(Constants.KEY_ADDRESS_STREET));
                        }
                        if (documentSnapshot.getString(Constants.KEY_ADDRESS_NUMBER) != null){
                            preferenceManager.putString(Constants.KEY_ADDRESS_NUMBER, documentSnapshot.getString(Constants.KEY_ADDRESS_NUMBER));
                        }
                        Intent intent = new Intent(getApplicationContext(), MainActivity2.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        loading(false);
                        showToast("Unable to sign in");
                    }
                });
    }

    private void loading(boolean isLoading) {
        if(isLoading){
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignIn.setVisibility(View.VISIBLE);
        }
    }

}