package com.example.chatss.activities;

import android.Manifest;
import android.app.AlertDialog;

import android.app.DownloadManager;
import android.content.Context;
import android.app.Dialog;
import android.content.DialogInterface;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.example.chatss.R;
import com.example.chatss.adapter.ChatGroupAdapter;
import com.example.chatss.databinding.ActivityChatGroupBinding;
import com.example.chatss.listeners.DownloadImageListener;
import com.example.chatss.models.ChatMessage;
import com.example.chatss.models.RoomChat;
import com.example.chatss.network.ApiClient;
import com.example.chatss.network.ApiService;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatGroupActivity extends BaseActivity implements DownloadImageListener {
    private String idUserCreate = new String();

    private ActivityChatGroupBinding binding;
    private RoomChat roomChat = new RoomChat();
    private List<ChatMessage> chatMessages;
    private ChatGroupAdapter chatGroupAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private int cntMessage=0;
    private JSONArray tokens ;
    private Uri imgUri;

    private static final int REQUEST_PERMISSION_CODE = 10;

    private Boolean isReceiverAvailable = false;

    private String encodedImage, imgUrl;

    private Bitmap bitmapImg ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        tokens = new JSONArray();
        loadReceiverDetails();
        initData();
        setListeners();
        init();
        listenMessages();
    }
    private void initData() {
        CollectionReference collection = database.collection("RoomChat").document(roomChat.getId()).collection("messages");
        AggregateQuery countQuery = collection.count();
        countQuery.get(AggregateSource.SERVER).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AggregateQuerySnapshot snapshot = task.getResult();
                //Log.d("a", "Count: " + snapshot.getCount());
                cntMessage = (int) (snapshot.getCount()+1);
                //Toast.makeText(getApplicationContext(), String.valueOf(cntMessage), Toast.LENGTH_SHORT).show();
            } else {
                Log.d("a", "Count failed: ", task.getException());
            }
        });
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatGroupAdapter = new ChatGroupAdapter(
                chatMessages,
                preferenceManager.getString(Constants.KEY_USED_ID),
                ChatGroupActivity.this
        );
        binding.chatRecyclerView.setAdapter(chatGroupAdapter);

    }

    private void sendMessage(){
        HashMap<String , Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USED_ID));
        message.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
        message.put("roomId", roomChat.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        message.put(Constants.TYPE_MESSAGES_SEND, "text");
        message.put(Constants.KEY_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
        //database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        database.collection("RoomChat").document(roomChat.getId()).collection("messages").document(String.valueOf(cntMessage))
                .set(message)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        initData();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
        database.collection("RoomChat").document(roomChat.getId())
                .update(
                        "senderName", preferenceManager.getString(Constants.KEY_NAME),
                        "lastMessage", binding.inputMessage.getText().toString(),
                        Constants.KEY_TIMESTAMP, new Date()
                );
        database.collection("ListRoomUser").document(preferenceManager.getString(Constants.KEY_USED_ID)).collection("ListRoom").document(roomChat.getId())
                        .update(
                                "senderName", preferenceManager.getString(Constants.KEY_NAME),
                                "lastMessage", binding.inputMessage.getText().toString(),
                                Constants.KEY_TIMESTAMP, new Date()
                        );
        //sendnotification
        getTokenbeforeSendNoti( binding.inputMessage.getText().toString());

        binding.inputMessage.setText(null);
    }
    private void getTokenbeforeSendNoti(String mess){
        database.collection("Participants").document(roomChat.id).collection("Users").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            List<String> usersID = new ArrayList<>();
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                String userId = document.getId();
                                // Sử dụng documentId ở đây (ví dụ: in ra, thêm vào danh sách, v.v.)
                                usersID.add(userId);
                                if (preferenceManager.getString(Constants.KEY_USED_ID).equals(userId)){
                                    continue;
                                }
                                database.collection(Constants.KEY_COLLECTION_USERS).document(userId).get().addOnCompleteListener(task1 ->{
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot user = task1.getResult();
                                        if (user.exists()) {
                                            if (user.getString(Constants.KEY_FCM_TOKEN) != null){
                                                String token = user.getString(Constants.KEY_FCM_TOKEN);
                                                // Sử dụng fieldValue ở đây (ví dụ: in ra, gán giá trị cho một biến, v.v.)
                                                tokens.put(token);
                                                try {
                                                    JSONArray tokens = new JSONArray();
                                                    tokens.put(token);

                                                    JSONObject data = new JSONObject();

                                                    data.put("id", roomChat.id);
                                                    data.put(Constants.KEY_NAME, roomChat.name);
                                                    data.put(Constants.KEY_MESSAGE, preferenceManager.getString(Constants.KEY_NAME)+ ": " + mess);
                                                    data.put("Type", "Group");

                                                    JSONObject body = new JSONObject();
                                                    body.put(Constants.REMOTE_MSG_DATA,data);
                                                    body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);

                                                    sendNotification(body.toString());
                                                }catch (Exception e){
                                                    showToast(e.getMessage());
                                                }
                                            }
                                        } else {
                                            Log.d("Firestore", "Document not found");
                                        }
                                    } else {
                                        Log.d("Firestore", "Error getting user: " + task.getException());
                                    }
                                });
                            }


                        }
                    } else {
                        Log.d("Firestore", "Error getting list users: " + task.getException());
                    }
                });
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
    private  void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private  void showToash(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenMessages(){
        database.collection("RoomChat").document(roomChat.getId()).collection("messages")
                .addSnapshotListener(eventListener);

    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null){
            return;
        }
        if (value != null){
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()){
                if (documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.type = documentChange.getDocument().getString(Constants.TYPE_MESSAGES_SEND);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessage.imageSender = documentChange.getDocument().getString(Constants.KEY_IMAGE);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0){
                chatGroupAdapter.notifyDataSetChanged();
            }else {
                chatGroupAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);

    };

    private  void loadReceiverDetails() {
        database = FirebaseFirestore.getInstance();
        roomChat = (RoomChat) getIntent().getSerializableExtra(Constants.KEY_ROOM);
        binding.textName.setText(roomChat.name);
        // user tao group

        database.collection("RoomChat")
                .whereEqualTo("id", roomChat.id)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() != null){
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            idUserCreate = queryDocumentSnapshot.getString("idUserCreate");

                        }
                    }
                    else {

                    }
                });

    }

    private void setListeners(){

        binding.imageBack.setOnClickListener(view -> onBackPressed());
        binding.layoutSend.setOnClickListener(view -> {
            if(!binding.inputMessage.getText().toString().trim().isEmpty()){
                sendMessage();
            }else{
                showToash("Type message first!!");
            }
        });
        binding.layoutSendImage.setOnClickListener(v ->{
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        binding.imageInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Toast.makeText(getApplicationContext(), idUserCreate, Toast.LENGTH_SHORT).show();
                if (idUserCreate.equals(preferenceManager.getString(Constants.KEY_USED_ID))) {
                    String[] items = {"Add member", "Delete member","Group information", "Leave group"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(ChatGroupActivity.this);
                    builder.setTitle("Select")
                            .setItems(items, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == 0) {
                                        // them thanh vien
                                        Intent intent = new Intent(getApplicationContext(), AddMemberActivity.class);
                                        intent.putExtra(Constants.KEY_ROOM, roomChat);
                                        startActivity(intent);
                                    } else if (which == 1) {
                                        // xoa thanh vien
                                        Intent intent = new Intent(getApplicationContext(), DeleteMemberActivity.class);
                                        intent.putExtra(Constants.KEY_ROOM, roomChat);
                                        startActivity(intent);
                                    }else if (which == 2) {
                                        // thong tin
                                        Intent intent = new Intent(getApplicationContext(), SettingGroupActivity.class);
                                        intent.putExtra(Constants.KEY_ROOM, roomChat);
                                        startActivity(intent);


                                    } else if (which == 3) {
                                        // roi khoi nhom
                                        database.collection("ListRoomUser").document(preferenceManager.getString(Constants.KEY_USED_ID)).collection("ListRoom").document(roomChat.getId())
                                                .delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        //Toast.makeText(getApplicationContext(), String.valueOf(roomChat.getId()), Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {

                                                    }
                                                });
                                        database.collection("Participants").document(String.valueOf(roomChat.getId())).collection("Users").document(preferenceManager.getString(Constants.KEY_USED_ID))
                                                .delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        onBackPressed();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {

                                                    }
                                                });
                                        onBackPressed();
                                    }
                                }
                            });
                    builder.create();
                    builder.show();
                }
                else{

                    String[] items = {"Add member","Change group name ", "Leave group"};

                    AlertDialog.Builder builder = new AlertDialog.Builder(ChatGroupActivity.this);
                    builder.setTitle("Select")
                            .setItems(items, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == 0) {
                                        // them thanh vien
                                        Intent intent = new Intent(getApplicationContext(), AddMemberActivity.class);
                                        intent.putExtra(Constants.KEY_ROOM, roomChat);
                                        startActivity(intent);
                                    }
                                    else if (which == 1) {
                                        // thong tin
                                        Intent intent = new Intent(getApplicationContext(), SettingGroupActivity.class);
                                        intent.putExtra(Constants.KEY_ROOM, roomChat);
                                        startActivity(intent);


                                    } else if (which == 2) {
                                        // roi khoi nhom
                                        database.collection("ListRoomUser").document(preferenceManager.getString(Constants.KEY_USED_ID)).collection("ListRoom").document(roomChat.getId())
                                                .delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        //Toast.makeText(getApplicationContext(), String.valueOf(roomChat.getId()), Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {

                                                    }
                                                });
                                        database.collection("Participants").document(String.valueOf(roomChat.getId())).collection("Users").document(preferenceManager.getString(Constants.KEY_USED_ID))
                                                .delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        onBackPressed();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {

                                                    }
                                                });
                                        onBackPressed();
                                    }
                                }
                            });
                    builder.create();
                    builder.show();
                }
            }

        });
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result->{
                if(result.getResultCode() == RESULT_OK){
                    LayoutInflater inflater = LayoutInflater.from(ChatGroupActivity.this);
                    View view = inflater.inflate(R.layout.send_image_in_chat,null);

                    AlertDialog alertDialog = new AlertDialog.Builder(ChatGroupActivity.this)
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
        String filepath = "ChatGroupImages/" +  System.currentTimeMillis();

        StorageReference reference = FirebaseStorage.getInstance().getReference(filepath);
        reference.putFile(Uri.parse(imgUrl)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> task = taskSnapshot.getMetadata().getReference().getDownloadUrl();

                task.addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        imgUrl = uri.toString();

                        HashMap<String, Object> message = new HashMap<>();
                        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USED_ID));
                        message.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                        message.put(Constants.KEY_RECEIVER_ID, roomChat.id);
                        message.put(Constants.KEY_MESSAGE, imgUrl);
                        message.put(Constants.KEY_TIMESTAMP, new Date());
                        message.put(Constants.TYPE_MESSAGES_SEND, "image");
                        message.put(Constants.KEY_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));

                        database.collection("RoomChat").document(roomChat.getId()).collection("messages").document(String.valueOf(cntMessage))
                                .set(message)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        initData();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                        database.collection("RoomChat").document(roomChat.getId())
                                .update(
                                        "lastMessage", "*Image",
                                        Constants.KEY_TIMESTAMP, new Date(),
                                        "senderName", preferenceManager.getString(Constants.KEY_NAME)
                                );
                        database.collection("ListRoomUser").document(preferenceManager.getString(Constants.KEY_USED_ID)).collection("ListRoom").document(roomChat.getId())
                                .update(
                                        "senderName", preferenceManager.getString(Constants.KEY_NAME),
                                        "lastMessage", "*Image",
                                        Constants.KEY_TIMESTAMP, new Date()
                                );
                    }
                });
            }
        });
    }

    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }


    @Override
    protected void onResume() {
        loadReceiverDetails();
        super.onResume();
    }

    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    public void onItemClick(ChatMessage chatMessage) {
        LayoutInflater inflater = LayoutInflater.from(ChatGroupActivity.this);
        View view = inflater.inflate(R.layout.send_image_in_chat,null);

        AlertDialog alertDialog = new AlertDialog.Builder(ChatGroupActivity.this)
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
                showToash("Permission Denied");
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
            showToash("Download successful");
        }
    }
}