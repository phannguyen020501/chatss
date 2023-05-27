package com.example.chatss.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatss.adapter.UsersGroupAdapter;
import com.example.chatss.databinding.ActivityCreateGroupBinding;
import com.example.chatss.listeners.UserListener;
import com.example.chatss.models.RoomChat;
import com.example.chatss.models.User;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class CreateGroupActivity extends AppCompatActivity implements UserListener {
    private ActivityCreateGroupBinding binding;
    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private PreferenceManager preferenceManager;
    private int cntRoomChat=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager((getApplicationContext()));
        Constants.userGroups.clear();
        initData();
        setListener();
        getUsers();
        createGroup();
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(!TextUtils.isEmpty(query.trim())){
                    searchUsers(query);
                }else{
                    getUsers();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(!TextUtils.isEmpty(newText.trim())){
                    searchUsers(newText);
                }else{
                    getUsers();
                }
                return false;
            }
        });
    }

    private void initData() {
        CollectionReference collection = database.collection("RoomChat");
        AggregateQuery countQuery = collection.count();
        countQuery.get(AggregateSource.SERVER).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AggregateQuerySnapshot snapshot = task.getResult();
                //Log.d("a", "Count: " + snapshot.getCount());
                cntRoomChat = (int) (snapshot.getCount()+1);
                Toast.makeText(getApplicationContext(), String.valueOf(cntRoomChat), Toast.LENGTH_SHORT).show();
            } else {
                Log.d("a", "Count failed: ", task.getException());
            }
        });
        binding.edtCreateGroup.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.btnCreateGroup.setEnabled(charSequence.length() > 0 && checkCheckBoxisCheck());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void createGroup() {
        binding.btnCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!binding.edtCreateGroup.getText().toString().equals("")) {
                    loading(true);

                    HashMap<String, Object> roomChat = new HashMap<>();
                    roomChat.put("id", String.valueOf(cntRoomChat));
                    roomChat.put("name", binding.edtCreateGroup.getText().toString());
                    roomChat.put("lastMessage", "");
                    roomChat.put("idUserCreate", preferenceManager.getString(Constants.KEY_USED_ID));
                    RoomChat roomChat1 = new RoomChat();
                    roomChat1.id =  String.valueOf(cntRoomChat);
                    roomChat1.name = binding.edtCreateGroup.getText().toString();
                    roomChat1.lastMessage = "";
                    binding.edtCreateGroup.getText().clear();
                    // tao phong chat
                    database.collection("RoomChat").document(String.valueOf(cntRoomChat))
                            .set(roomChat)
                            .addOnSuccessListener(documentReference -> {
                                for(int i=0;i<Constants.userGroups.size();i++){
                                    if(Constants.userGroups.get(i).checked.equals("1")){
                                        // them roomChat cho cac user duoc moi tham gia
                                        int finalI = i;
                                        database.collection("ListRoomUser").document(Constants.userGroups.get(i).id).collection("ListRoom").document(String.valueOf(cntRoomChat))
                                                .set(roomChat)
                                                .addOnSuccessListener(aVoid -> {
                                                    // nguoi trong nhom
                                                    database.collection("Participants").document(String.valueOf(cntRoomChat)).collection("Users").document(Constants.userGroups.get(finalI).id)
                                                            .set(Constants.userGroups.get(finalI))
                                                            .addOnSuccessListener(aVoid1 -> database.collection("ListRoomUser").document(preferenceManager.getString(Constants.KEY_USED_ID)).collection("ListRoom").document(String.valueOf(cntRoomChat))
                                                                    .set(roomChat)
                                                                    .addOnSuccessListener(aVoid11 -> database.collection("Participants").document(String.valueOf(cntRoomChat)).collection("Users").document(Constants.userCurrent.getId())
                                                                            .set(Constants.userCurrent)
                                                                            .addOnSuccessListener(aVoid111 -> {
                                                                                Intent intent = new Intent(getApplicationContext(), ChatGroupActivity.class);
                                                                                intent.putExtra(Constants.KEY_ROOM, roomChat1);
                                                                                startActivity(intent);
                                                                                finish();
                                                                            })
                                                                            .addOnFailureListener(e -> {

                                                                            }))
                                                                    .addOnFailureListener(e -> {

                                                                    }))
                                                            .addOnFailureListener(e -> {

                                                            });
                                                })
                                                .addOnFailureListener(e -> {
                                                });
                                    }
                                }
                                loading(false);
                            })
                            .addOnFailureListener(exception -> {
                                loading(false);
                                showToast(exception.getMessage());
                            });
                }
            }
        });
    }
    private Boolean checkCheckBoxisCheck(){
        for(int i=0;i<Constants.userGroups.size();i++) {
            if (Constants.userGroups.get(i).checked.equals("1")) {
                return true;
            }
        }
        return false;
    }
    private void showToast (String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();;
    }
    private void setListener(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());

    }
    private void getUsers(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
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
                            if (queryDocumentSnapshot.getLong(Constants.KEY_AVAILABILITY)!= null){
                                user.availability = Objects.requireNonNull(queryDocumentSnapshot.getLong(Constants.KEY_AVAILABILITY)).intValue();
                            }
                            users.add(user);
                        }
                        if(users.size() > 0){
                            UsersGroupAdapter usersGroupAdapter = new UsersGroupAdapter(users,this);
                            binding.usersRecyclerView.setAdapter(usersGroupAdapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }

    private void showErrorMessage() {
        binding.textErrorMessage.setText(String.format("%s", "No user available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(boolean isLoading) {
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
    private void searchUsers(String s){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        Task<QuerySnapshot> x = database.collection(Constants.KEY_COLLECTION_USERS)
                .orderBy("name").startAt(s).endAt(s+"\uf8ff").get();
        Task<QuerySnapshot> y = database.collection(Constants.KEY_COLLECTION_USERS)
                .orderBy("email").startAt(s).endAt(s+"\uf8ff").get();

        Tasks.whenAllSuccess(x, y).addOnSuccessListener(task ->{
            loading(false);
            String currentUserId = preferenceManager.getString(Constants.KEY_USED_ID);
            if((x.isSuccessful() && x.getResult() != null) || (y.isSuccessful() && y.getResult() != null)){
                List<User> users = new ArrayList<>();
                for(QueryDocumentSnapshot queryDocumentSnapshot : x.getResult()){
                    if(currentUserId.equals(queryDocumentSnapshot.getId())){
                        continue;
                    }
                    User user = new User();
                    user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                    user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                    user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                    user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                    user.id = queryDocumentSnapshot.getId();
                    if (queryDocumentSnapshot.getLong(Constants.KEY_AVAILABILITY)!= null){
                        user.availability = Objects.requireNonNull(queryDocumentSnapshot.getLong(Constants.KEY_AVAILABILITY)).intValue();
                    }
                    users.add(user);
                }

                for(QueryDocumentSnapshot queryDocumentSnapshot : y.getResult()){
                    if(currentUserId.equals(queryDocumentSnapshot.getId())){
                        continue;
                    }
                    User user = new User();
                    user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                    user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                    user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                    user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                    user.id = queryDocumentSnapshot.getId();
                    if (queryDocumentSnapshot.getLong(Constants.KEY_AVAILABILITY)!= null){
                        user.availability = Objects.requireNonNull(queryDocumentSnapshot.getLong(Constants.KEY_AVAILABILITY)).intValue();
                    }
                    users.add(user);
                }

                if(users.size() > 0){
                    UsersGroupAdapter usersGroupAdapter = new UsersGroupAdapter(users,this);
                    binding.usersRecyclerView.setAdapter(usersGroupAdapter);
                    binding.usersRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    binding.usersRecyclerView.setVisibility(View.INVISIBLE);
                }
            } else {
                binding.usersRecyclerView.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    @Override
    public void onUserClicked(User user) {

    }
}