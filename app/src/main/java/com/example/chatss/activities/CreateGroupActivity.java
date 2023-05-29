package com.example.chatss.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatss.adapter.UsersGroupAdapter;
import com.example.chatss.databinding.ActivityCreateGroupBinding;
import com.example.chatss.listeners.UserListener;
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
                //Toast.makeText(getApplicationContext(), String.valueOf(cntRoomChat), Toast.LENGTH_SHORT).show();
            } else {
                Log.d("a", "Count failed: ", task.getException());
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
                    roomChat.put("image", "/9j/4AAQSkZJRgABAQAAAQABAAD/4gIoSUNDX1BST0ZJTEUAAQEAAAIYAAAAAAIQAABtbnRyUkdCIFhZWiAAAAAAAAAAAAAAAABhY3NwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAA9tYAAQAAAADTLQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAlkZXNjAAAA8AAAAHRyWFlaAAABZAAAABRnWFlaAAABeAAAABRiWFlaAAABjAAAABRyVFJDAAABoAAAAChnVFJDAAABoAAAAChiVFJDAAABoAAAACh3dHB0AAAByAAAABRjcHJ0AAAB3AAAADxtbHVjAAAAAAAAAAEAAAAMZW5VUwAAAFgAAAAcAHMAUgBHAEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFhZWiAAAAAAAABvogAAOPUAAAOQWFlaIAAAAAAAAGKZAAC3hQAAGNpYWVogAAAAAAAAJKAAAA+EAAC2z3BhcmEAAAAAAAQAAAACZmYAAPKnAAANWQAAE9AAAApbAAAAAAAAAABYWVogAAAAAAAA9tYAAQAAAADTLW1sdWMAAAAAAAAAAQAAAAxlblVTAAAAIAAAABwARwBvAG8AZwBsAGUAIABJAG4AYwAuACAAMgAwADEANv/bAEMAEAsMDgwKEA4NDhIREBMYKBoYFhYYMSMlHSg6Mz08OTM4N0BIXE5ARFdFNzhQbVFXX2JnaGc+TXF5cGR4XGVnY//bAEMBERISGBUYLxoaL2NCOEJjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY//AABEIAFIAlgMBIgACEQEDEQH/xAAbAAABBQEBAAAAAAAAAAAAAAAAAgMEBQYBB//EADkQAAIBAwIDBQYFAwMFAAAAAAECAAMEERIhBTFRBhNBYXEUIjKBkaEzQlKx0RXB8IKS4TRDU3Lx/8QAGQEAAwEBAQAAAAAAAAAAAAAAAQIDAAQF/8QAIxEAAgICAgICAwEAAAAAAAAAAAECEQMxEiETQQQiMjNRcf/aAAwDAQACEQMRAD8A9AhE6h1gGB5TGFQhOTGOwiHqBfM9Iy1Vj/xCkCyTCQxUVj8QJ9YrOnkcQ8TWSoRgVGHnHVcMNoGjWKnCQOZxG6tTSMLz/aRySTknMKjYSQa6Dlk+k6KqnwIkX5E+kWrAgbj6w8UAkhgeRipEYjGIqnW0nDEkdekHH+BJMJwHIyJ2KYIQhMYaDbZ0MCZGa6cOwCgAHxk2Vz/jN/7GLJlMaTfY57XU6LA3VQjksRoTqPr/AMxLgDGP8+8W2VSi/Q/TbWufGB8VwSP3labmqjMFbAz0h7XX/X9hKLIq7A/jSbtE9VKtnBP2nWqBVLMCAOsr/a6/6/sI7Uqmrw1y27A4J+YjQkpOiWXDLHHkxxuI0Q2wZgfED+YocQoAbOQfQypVQ3NgvrmK7sf+RPv/ABOt4oLo4fJItPbrcj4/sYuoxUbfWUpGDzzNRoT9I+knkShVFccnO7IIYnGVPnOkkn4Sd+cmaE/Sv0iKyKKZIAB8pLkmVIVWutFQXz73h4xr2+nn4W9ZHu9T3ZUbnIUCNd1UxnQfi0/PpKqK9iuTLKlxOkmxV8eg/mTqFxTuF1U2zjn5TPFGXVkfCcH1/wAEsuC/97/T/eJOCStGTdlpCEJEcJAr0XV2bSSpOcyfOHBByMiBqxoy4srArEZCnHpDQ36T9JI093lNyM5HWGoDnkTKFod5mnoqKm1RgepjiOVt3yMjIUbDAzvn12jtxaVDVLIAwY55xo2tYKSU2G/MROLR2eSEkuxNwSa9QkY947dI6iluG1QP1Z/aIW1rMoYJkEZG4llZ0DStylTBLEkj+0bHalZH5Di8fFMo1Yry/cxXenp9z/MuTbUAxxRTn+mRrm3RkK06YDEjGF5Tv8kW9Hk+OSRWk5bJmjb/AKpN/wAjbfMSJb0KNNl1Iu35iPGTtaZ+IfWQzSUqSLYouN2RKCgC0xUBxq8eexkqt+EYrUg/MPrG6zqaZAYH0Mktlm7KS9BF0+RjOMfSMS5ZEf4lDY6jMYrWyHQyIBpYEgDmJ0qRNxK2WnBR+MfT+8X3NLwpKf8ATJtFEpUwFUL1x1iTnaoyjTHITmYSA52IqtoQmLjN0SKJI8JjGNvv6hf9obilTvatvRtyme7qeQIGnI56jvvy8pobUmlZ0VqO9RlQAuw3bA5mM186++KrqwBqVcHAJIB8tz9THkrd/RHgzbHEpxn3/DOUOl7HUqjI22j1SolKm1SowVFBLFjgAeZjagKAB4RVwivb1EqKGVlIIIyDNpC++jCXXaPiHG+LW1rwUtbKre4pYAuRvlvDAA+Hf57T0DUcSiteFWb8Qt7oqEqWw001XAB2OBjHhknbx64l5BGSkrGnHi6M72p7R1eEVre2tKaVbir7zBgTheQwB4k5+nLeR+G8e4rX4na23ELGjRS4VyGGQ2wzyJ/znKrteWv+0qUbGi73NtTActgqfzDY7YGfHnnGOtle3lSl2h4S5tA9LvCq1lbbL+5vttjOceMLlTSMoXFyNJrA/Kc9cRasBtg46Yiyh5gbRHeJq06t84htCxTYsknGfCYvtLw65ueL1aiPrXSNKHORgDYfPw6kzVXdyaZFOn8Z5npGrhBUr0nYsCG2CnAzzz9ppKVfUaLjdMTwFblOD263ned9g57w+9zOM58sSbWqJRotVqEBEGok+AG8RRrF6z02xkDIkLtEb3+k1FsKRqu5COFXUQhyDgfQehPrGYEUdx2i4m9X2m2oaLVWzpdQdQ8+mc+Bmt4VfJxHh1G8VGQVBupxsQcH7iYe89oocEVKtFqVQKqMG2KjrjzH7zVdi661uz1JFUg0XZG8znVkf7hI3asecVGqLkMB1+kI7CAUI3cJ3lCog5spEZS+pucKlQ7gchzPzkbiHEKi8PqPaUHeoQQpYgBfMnwA3/bxmQGQKNzpGmpuPAybaBHXNPkDgDEydfjL29d6NW1w9NirDvM4I+U0vBLlqnDKV4tPSlQtqXOcYJHP5TryOLX1I41JP7IsAhB94YmWvK9W5vuJXhqP3NkrU6CE7BwvvHHXnz6+U2KPTrplSCP2mW47RpU1ubRrmnaU6jd5q3YlWOTtnxbPynNytUdMFUuzLm9rcV4jardkMneKulRgYJAP1no1sxaiM8xtMPY8Cf2uhcW93QrUabq5IJzsc8t5urSmxt1OMesfJKPJKOgtPjbKq9s6FtdXV2gYVbhFFTfbbYY6SHa941zTWnnVq2lvximBSHvDUQduoG8r+EVlo36axs4056E8v885yT7md+B1gbSLmm1xUHdaO7xsz9fSSPZ0C4QYx0jsJVdHnN2UNPSHrPcHDpvv64iTcrUr09J9xSST8tpN4pSWmwuQQCCAwJxmZ6r2g4fURajd8ugkd3o3f0IOMfOW5OWjRSWy6twrcRTByGBzg+R/iS7tytdaanCaeQlV2ZvKPFLi4q00dBQYYBwcg5wfsdvTcy04lRqNUSrTJBHQZHziZnehsCXLsj3NCnd2zW9dddJ+a5I8cx7hFNLZfZaK6KKL7q9N/wDmR3av7ugJ55Bk3h6NlqjDmMD+8jFu6OjIkosnQhCUOQj9zSx+En+0TPdtEWlwuiaahCa4GVGPytCEMdmZiiSTknJ85vuxW/Z5Adx3jQhKT0BFtbooYEKAcHcDzmG7Rb9qLhTuC2CPLukhCDBsbJsqqjspXSxGkVCMHljOJ6kNsAbf/IQjZtgWir4sT/UaIzyT+ZTvs5x1hCcMvyPW+P8ArX+GxHKdhCdB5JWdoCRwmsQcEFd/mJ5wzsETDHZHI35YziEI8DPRq+w3w3h8SaefP3Zp7r8Fvl+8IRZhjtEKTbP8I+sISUdnRl/EkQhCUOY//9k=");
                    binding.edtCreateGroup.getText().clear();
                    // tao phong chat
                    database.collection("RoomChat").document(String.valueOf(cntRoomChat))
                            .set(roomChat)
                            .addOnSuccessListener(documentReference -> {
                                for(int i=0;i<Constants.userGroups.size();i++){
                                    if(Constants.userGroups.get(i).checked.equals("1")){
                                        // them roomChat cho cac user duoc moi tham gia
                                        database.collection("ListRoomUser").document(Constants.userGroups.get(i).id).collection("ListRoom").document(String.valueOf(cntRoomChat))
                                                .set(roomChat)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        // nguoi trong nhom

                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {

                                                    }
                                                });
                                        database.collection("Participants").document(String.valueOf(cntRoomChat)).collection("Users").document(Constants.userGroups.get(i).id)
                                                .set(Constants.userGroups.get(i))
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {

                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {

                                                    }
                                                });
                                    }
                                }
                                database.collection("ListRoomUser").document(preferenceManager.getString(Constants.KEY_USED_ID)).collection("ListRoom").document(String.valueOf(cntRoomChat))
                                        .set(roomChat)
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

                                database.collection("Participants").document(String.valueOf(cntRoomChat)).collection("Users").document(Constants.userCurrent.getId())
                                        .set(Constants.userCurrent)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {

                                            }
                                        });
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
        List<String> listEmail = new ArrayList<>();

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
                    users.add(user);
                    listEmail.add(user.email);
                }

                for(QueryDocumentSnapshot queryDocumentSnapshot : y.getResult()){
                    if(currentUserId.equals(queryDocumentSnapshot.getId())){
                        continue;
                    }
                    if(!listEmail.contains(queryDocumentSnapshot.getString(Constants.KEY_EMAIL))){
                        User user = new User();
                        user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                        user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                        user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                        user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                        user.id = queryDocumentSnapshot.getId();
                        users.add(user);
                    }

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
        Intent intent = new Intent(getApplicationContext(), ChatGroupActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
    }
}