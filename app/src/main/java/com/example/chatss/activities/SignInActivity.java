package com.example.chatss.activities;

import static com.example.chatss.utilities.KeyUtils.tryr;

import androidx.annotation.NonNull;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
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
    private FirebaseAuth firebaseAuth;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        firebaseAuth = FirebaseAuth.getInstance();
        setContentView(binding.getRoot());
        onEditTextStatusChange();
        setListeners();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setListeners(){
        binding.textCreateNewAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class))
//        {
//            try {
//                readKeyStore();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//
//        }
        );
        binding.buttonSignIn.setOnClickListener(v->{
            if(isValidSignInDetails()){
                signIn();
            }
//            try {
//                tryr();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//            try {
//                main();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }

        });
    }

    private void main() throws Exception {
//        Security.addProvider(new BouncyCastleProvider());
//
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
//        keyPairGenerator.initialize(256); // Độ dài khóa ECDSA, ví dụ 256-bit
//
//        KeyPair keyPair = keyPairGenerator.generateKeyPair();
//
//        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
//        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
//
//        System.out.println("Public Key: " + Hex.toHexString(publicKeyBytes));
//        System.out.println("Private Key: " + Hex.toHexString(privateKeyBytes));
        // Khởi tạo KeyStore
        KeyStore keyStore = KeyStore.getInstance("BKS");
        keyStore.load(null, null); // Load hoặc tạo mới KeyStore

        // Tạo và lưu trữ khóa bí mật và công khai với alias và mật khẩu
        String alias = "my_alias";
        char[] password = "my_password".toCharArray();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X509Certificate certificate = generateCertificate(keyPair);
        Certificate[] certificates = new Certificate[]{certificate};

        //Để in ra private key dạng string test, không cần thiết
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String privateKeyString = Base64.getEncoder().encodeToString(privateKeyBytes);
            Log.d("Private Key 0:", privateKeyString);
        }

        KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), certificates);
        KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(password);
        keyStore.setEntry(alias, privateKeyEntry, passwordProtection);


        String fileName = "my_keystore.bks";
        File file = new File(Environment.getExternalStorageDirectory(), fileName);
        String filePath = file.getAbsolutePath();
        //Cấp quyền đoc, ghi file đối với API >= 23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 123);
            }
        }
        //Cấp quyền ghi file đối với API >= 30
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
            startActivity(intent);
        }

//        // Lưu KeyStore vào Internal (Khi xóa app sẽ mất)
//        FileOutputStream fos = openFileOutput(fileName, MODE_APPEND);
//        keyStore.store(fos, "123456".toCharArray());
//        fos.close();

        // Lưu KeyStore vào SD Card (Khi xóa app sẽ không bị mat)
        OutputStream outputStream = new FileOutputStream(filePath);
        keyStore.store(outputStream, "123456".toCharArray());
        outputStream.close();
    }

    private void readKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("BKS");

        String fileName = "my_keystore.bks";
        File file = new File(Environment.getExternalStorageDirectory(), fileName);
        String filePath = file.getAbsolutePath();
        InputStream inputStream = new FileInputStream(filePath);
        keyStore.load(inputStream, "123456".toCharArray());
        inputStream.close();
        PrivateKey privateKey = (PrivateKey) keyStore.getKey

                ("my_alias","my_password".toCharArray());

        // Chuyển private key thành chuỗi Base64 (Để in ra, trong trường hợp giải mã thì không cần thiết)
        byte[] privateKeyBytes = privateKey.getEncoded();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String privateKeyString = Base64.getEncoder().encodeToString(privateKeyBytes);
            Log.d("Private Key:", privateKeyString);
        }

    }
    // Phương thức tạo chứng chỉ tự ký
//    private static X509Certificate createSelfSignedCertificate(KeyPair keyPair) throws Exception {
//        Date startDate = new Date(); // Ngày bắt đầu chứng chỉ
//        Date expiryDate = new Date(startDate.getTime() + 365 * 24 * 60 * 60 * 1000L); // Ngày hết hạn chứng chỉ
//
//        JcaX509v3CertificateBuilder  certBuilder = new JcaX509v3CertificateBuilder(
//                new X500Name("CN=Issuer"), // Thông tin người phát hành
//                BigInteger.ONE, // Số serial của chứng chỉ
//                startDate,
//                expiryDate,
//                new X500Name("CN=Subject"), // Thông tin chủ thể
//                keyPair.getPublic() // Khóa công khai
//        );
//
//        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate()); // Thuật toán ký
//        X509CertificateHolder certHolder = certBuilder.build(contentSigner);
//        return new JcaX509CertificateConverter().getCertificate(certHolder);
//    }
    public X509Certificate generateCertificate(KeyPair keyPair) throws CertificateEncodingException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        Date startDate = new Date(); // Ngày bắt đầu chứng chỉ
        Date expiryDate = new Date(startDate.getTime() + 365 * 24 * 60 * 60 * 1000L); // Ngày hết hạn chứng chỉ
        X509V3CertificateGenerator cert = new X509V3CertificateGenerator();
        cert.setSerialNumber(BigInteger.valueOf(1));   //or generate a random number
        cert.setSubjectDN(new X509Principal("CN=localhost"));  //see examples to add O,OU etc
        cert.setIssuerDN(new X509Principal("CN=localhost")); //same since it is self-signed
        cert.setPublicKey(keyPair.getPublic());
        cert.setNotBefore(startDate);
        cert.setNotAfter(expiryDate);
        cert.setSignatureAlgorithm("SHA256withECDSA");
        PrivateKey signingKey = keyPair.getPrivate();
        return cert.generate(signingKey);
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
    }

    private void signIn() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        firebaseAuth.signInWithEmailAndPassword(binding.inputEmail.getText().toString(), binding.inputPassword.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task1) {
                        database.collection(Constants.KEY_COLLECTION_USERS)
                                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                                .get()
                                .addOnCompleteListener(task -> {
                                    if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0 ){
                                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                        preferenceManager.putString(Constants.KEY_USED_ID, documentSnapshot.getId());
                                        preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                                        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                                        preferenceManager.putString(Constants.KEY_EMAIL, documentSnapshot.getString(Constants.KEY_EMAIL));
                                        preferenceManager.putString(Constants.KEY_PUBLIC_KEY, documentSnapshot.getString(Constants.KEY_PUBLIC_KEY));

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
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    } else {
                                        loading(false);
                                        showToast("Unable to sign in");
                                    }
                                });
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

//    private void addDataToFirestore(){
//        FirebaseFirestore database = FirebaseFirestore.getInstance();
//        HashMap<String, Object> data = new HashMap<>();
//        data.put("first_name","phan");
//        data.put("last_name", "nguyen");
//        database.collection("users")
//                .add(data)
//                .addOnSuccessListener(documentReference -> {
//                    Toast.makeText(getApplicationContext(), "data inserted", Toast.LENGTH_SHORT).show();
//        })
//                .addOnFailureListener(exception->{
//                    Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }
}