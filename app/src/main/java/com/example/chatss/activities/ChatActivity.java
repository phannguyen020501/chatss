package com.example.chatss.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.example.chatss.R;
import com.example.chatss.adapter.ChatAdapter;
import com.example.chatss.databinding.ActivityChatBinding;
import com.example.chatss.listeners.DownloadImageListener;
import com.example.chatss.models.ChatMessage;
import com.example.chatss.models.User;
import com.example.chatss.network.ApiClient;
import com.example.chatss.network.ApiService;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firestore.v1.Document;
import com.squareup.picasso.Picasso;

import org.checkerframework.checker.units.qual.C;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity implements DownloadImageListener{

    private static final int REQUEST_PERMISSION_CODE = 10;
    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversionId = null;

    private Boolean isReceiverAvailable = false;

    private String encodedImage, imgUrl, myId;

    private Bitmap bitmapImg ;

    private Uri imgUri;

    @Override
    protected void onStart() {
        super.onStart();
        isChat(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        updateIsSeen();
        listenMessages();
//        setChange();
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                preferenceManager.getString(Constants.KEY_USED_ID),
                getBitmapFromEncodedString(receiverUser.image),
                ChatActivity.this
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
        myId = preferenceManager.getString(Constants.KEY_USED_ID);
    }

    private void isChat(Boolean on){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                        .whereEqualTo(Constants.KEY_RECEIVER_ID, myId).get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                                DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                                String conversionId = documentSnapshot.getId();
                                HashMap<String, Object> data = new HashMap<>();
                                data.put(myId, on);
                                DocumentReference documentReference =
                                        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
                                documentReference.set(data, SetOptions.merge());
                            }
                        });
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).whereEqualTo(Constants.KEY_SENDER_ID, myId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        String conversionId = documentSnapshot.getId();
                        HashMap<String, Object> data = new HashMap<>();
                        data.put(myId, on);
                        DocumentReference documentReference =
                                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
                        documentReference.set(data, SetOptions.merge());
                    }
                });
    }


    private void sendMessage(){
        HashMap<String , Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USED_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.TYPE_MESSAGES_SEND, "text");
        message.put(Constants.KEY_TIMESTAMP, new Date());
        //message.put(Constants.isSeen, false);
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        if (conversionId != null){
            updateConversion(binding.inputMessage.getText().toString());
        }else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USED_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.isSeen, false);
            conversion.put(myId, true);
            conversion.put(receiverUser.id, false);
            conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            conversion.put(Constants.MESS_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.MESS_SENDER_ID, preferenceManager.getString(Constants.KEY_USED_ID));
            addConversion(conversion);
        }
        if (!isReceiverAvailable){
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USED_ID, preferenceManager.getString(Constants.KEY_USED_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA,data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);

                sendNotification(body.toString());
            }catch (Exception e){
                showToast(e.getMessage());
            }
        }
        binding.inputMessage.setText(null);
    }
    private  void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String messageBody){
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if(response.isSuccessful()){
                    try{
                        if(response.body() != null){
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if(responseJson.getInt("failure") == 1){
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                    showToast("Notification sent successfully");
                }else {
                    showToast("Error" + response.code());
                    Log.d("demo", ""+ response.code());
                }
            }

            @Override
            public void onFailure(@NonNull  Call<String> call, @NonNull Throwable t) {

                showToast(t.getMessage());
            }
        });

    }
    private void listenAvailabilityOfReceiver(){
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
            if (error != null){
                return;
            }
            if (value != null){
                if (value.getLong(Constants.KEY_AVAILABILITY) != null){
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable = availability == 1;
                }
                receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                if (receiverUser.image == null){
                    receiverUser.image = value.getString(Constants.KEY_IMAGE);
                    chatAdapter.setReceiverProfileImage(getBitmapFromEncodedString(receiverUser.image));
                    chatAdapter.notifyItemRangeChanged(0,chatMessages.size());
                }
            }
            if (isReceiverAvailable){
                binding.textAvailability.setVisibility(View.VISIBLE);
            }else {
                binding.textAvailability.setVisibility(View.GONE);
            }
        });
    }



    private void listenMessages(){
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USED_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USED_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null){
            return;
        }
        if (value != null){
            int count = chatMessages.size();
            int k=0;
            for (DocumentChange documentChange : value.getDocumentChanges()){
               if (documentChange.getType() == DocumentChange.Type.ADDED){
                   ChatMessage chatMessage = new ChatMessage();
                   chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                   chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                   chatMessage.type = documentChange.getDocument().getString(Constants.TYPE_MESSAGES_SEND);
                   chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                   chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                   chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);

                   chatMessages.add(chatMessage);
                   k=k+1;
               }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0){
                chatAdapter.notifyDataSetChanged();
            }else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                //chatAdapter.notifyDataSetChanged();
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if (conversionId == null){
            checkForConversion();
        }
    };


    private Bitmap getBitmapFromEncodedString(String encodedImage){
        if (encodedImage != null){
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        }else {
            return null;
        }

    }
    private  void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
    }

    private void setListeners(){
        binding.imageBack.setOnClickListener(view -> onBackPressed());
        binding.layoutSend.setOnClickListener(view -> {
                    if (!binding.inputMessage.getText().toString().trim().isEmpty()) {
                        sendMessage();
                    } else {
                        showToast("Type message first!!");
                    }
                });
        binding.layoutSendImage.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });

    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result->{
                if(result.getResultCode() == RESULT_OK){
                    LayoutInflater inflater = LayoutInflater.from(ChatActivity.this);
                    View view = inflater.inflate(R.layout.send_image_in_chat,null);

                    AlertDialog alertDialog = new AlertDialog.Builder(ChatActivity.this)
                            .setView(view)
                            .create();

                    alertDialog.show();

                    ImageView imageView = view.findViewById(R.id.preview_image);
                    Button uploadbtn = view.findViewById(R.id.preview_upload_img_btn);

                    if(result.getData() != null){
                        Uri imageUri = result.getData().getData();

                        imgUrl = imageUri.toString();

                        Picasso.get().load(imageUri).into(imageView);

                        uploadbtn.setOnClickListener(view1 -> {
                                sendImage();
                                alertDialog.dismiss();
                            });
                    }
                    alertDialog.show();

                }
            }
    );

    private void sendImage() {

        String filepath = "ChatImages/" +  System.currentTimeMillis();

        StorageReference reference = FirebaseStorage.getInstance().getReference(filepath);
        reference.putFile(Uri.parse(imgUrl)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> task = taskSnapshot.getMetadata().getReference().getDownloadUrl();

                task.addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        imgUrl = uri.toString();

                        HashMap<String , Object> message = new HashMap<>();
                        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USED_ID));
                        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                        message.put(Constants.KEY_MESSAGE, imgUrl);
                        message.put(Constants.KEY_TIMESTAMP, new Date());
                        message.put(Constants.TYPE_MESSAGES_SEND, "image");
                        //message.put(Constants.isSeen, false);
                        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

                        if (conversionId != null){
                            updateConversion("*Hình ảnh");
                        }else {
                            HashMap<String, Object> conversion = new HashMap<>();
                            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USED_ID));
                            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
                            conversion.put(Constants.isSeen, false);
                            conversion.put(myId, true);
                            conversion.put(receiverUser.id, false);
                            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
                            conversion.put(Constants.KEY_LAST_MESSAGE, "*Hình ảnh");
                            conversion.put(Constants.KEY_TIMESTAMP, new Date());
                            conversion.put(Constants.MESS_RECEIVER_ID, receiverUser.id);
                            conversion.put(Constants.MESS_SENDER_ID, preferenceManager.getString(Constants.KEY_USED_ID));
                            addConversion(conversion);
                        }
                    }
                });
            }
        });

    }


    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String , Object> converion){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(converion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message){
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        Boolean isChat = documentSnapshot.getBoolean(receiverUser.id);
                        documentReference.update(
                                Constants.isSeen, isChat);
                    }
                });
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date(),
                Constants.MESS_RECEIVER_ID, receiverUser.id,
                Constants.MESS_SENDER_ID, preferenceManager.getString(Constants.KEY_USED_ID)
        );
    }

    private void setChange(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USED_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener1);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USED_ID))
                .addSnapshotListener(eventListener1);
    }

    private final EventListener<QuerySnapshot> eventListener1 = (value, error) -> {
        if (error != null){
            return;
        }
        if (value != null) {
            chatAdapter.notifyDataSetChanged();
        }
    };


    private void updateIsSeen(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).whereEqualTo(Constants.MESS_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.MESS_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USED_ID)).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        String conversationId = documentSnapshot.getId();
                        DocumentReference documentReference =
                                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
                        documentReference.update(
                                Constants.isSeen, true);
                    }
                });
    }

    private  void checkForConversion(){
        if (chatMessages.size() != 0){
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USED_ID),
                    receiverUser.id
            );
            checkForConversionRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constants.KEY_USED_ID)

            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };
    @Override
    protected void onPostResume() {
        super.onPostResume();
        listenAvailabilityOfReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isChat(false);
    }

    @Override
    public void onItemClick(ChatMessage chatMessage) {
        LayoutInflater inflater = LayoutInflater.from(ChatActivity.this);
        View view = inflater.inflate(R.layout.send_image_in_chat,null);

        AlertDialog alertDialog = new AlertDialog.Builder(ChatActivity.this)
                .setView(view)
                .create();

        alertDialog.show();

        ImageView imageView = view.findViewById(R.id.preview_image);
        Button uploadbtn = view.findViewById(R.id.preview_upload_img_btn);

        uploadbtn.setText("Download");
        imgUri = Uri.parse(chatMessage.message);

        Picasso.get().load(imgUri).into(imageView);

        uploadbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermission();
                alertDialog.dismiss();
            }
        });


    }

    private void checkPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                String [] permission = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permission, REQUEST_PERMISSION_CODE);
            } else {
                startDownload();
            }
        } else {
            startDownload();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload();
            } else {
                showToast("Permission Denied");
            }
        }
    }

    private void startDownload() {

        DownloadManager.Request request = new DownloadManager.Request(imgUri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        request.setTitle("Download");
        request.setDescription("Dowmload file...");

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, String.valueOf(System.currentTimeMillis()) + ".jpg");

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if(downloadManager != null){
            downloadManager.enqueue(request);
            showToast("Download successful");
        }
    }

}