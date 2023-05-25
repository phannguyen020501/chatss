package com.example.chatss.activities;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.SearchView;

import com.example.chatss.adapter.UsersAdapter;
import com.example.chatss.databinding.ActivityUsersBinding;
import com.example.chatss.listeners.UserListener;
import com.example.chatss.models.User;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class UsersActivity extends BaseActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;

    private ListenerRegistration registrationUserDataChange;
    private List<User> users;
    private UsersAdapter usersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager((getApplicationContext()));
        db = FirebaseFirestore.getInstance();

        setListener();
        getUsers();
        listenUserDataChange();
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
                        users = new ArrayList<>();
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
                            user.publicKey = queryDocumentSnapshot.getString(Constants.KEY_PUBLIC_KEY);
                            if (queryDocumentSnapshot.getLong(Constants.KEY_AVAILABILITY)!= null){
                                user.availability = Objects.requireNonNull(queryDocumentSnapshot.getLong(Constants.KEY_AVAILABILITY)).intValue();
                            }
                            users.add(user);
                        }
                        if(users.size() > 0){
                            usersAdapter = new UsersAdapter(users,this);
                            binding.usersRecyclerView.setAdapter(usersAdapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }


    private void listenUserDataChange(){
        registrationUserDataChange = db.collection(Constants.KEY_COLLECTION_USERS)
                .addSnapshotListener(eventListener);
    }

    private  final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null){
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                String userId = documentChange.getDocument().getId();
                if (preferenceManager.getString(Constants.KEY_USED_ID) != null){
                    if (preferenceManager.getString(Constants.KEY_USED_ID).equals(userId)){
                        break;
                    }
                }

//                if (documentChange.getType() == DocumentChange.Type.ADDED) {
//                    User user = new User();
//                    user.name = documentChange.getDocument().getString(Constants.KEY_NAME);
//                    user.email = documentChange.getDocument().getString(Constants.KEY_EMAIL);
//                    user.image = documentChange.getDocument().getString(Constants.KEY_IMAGE);
//                    user.token = documentChange.getDocument().getString(Constants.KEY_FCM_TOKEN);
//                    user.id = documentChange.getDocument().getId();
//                    if (documentChange.getDocument().getLong(Constants.KEY_AVAILABILITY)!= null){
//                        user.availability = Objects.requireNonNull(documentChange.getDocument().getLong(Constants.KEY_AVAILABILITY)).intValue();
//                    }
//                    users.add(user);
//                    usersAdapter.notifyItemInserted(users.size() - 1);
//                }else
                if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < users.size(); i++) {
                        if (users.get(i).id.equals(userId)) {

                            if (documentChange.getDocument().getLong(Constants.KEY_AVAILABILITY) != null){
                                users.get(i).availability = Objects.requireNonNull(
                                        documentChange.getDocument().getLong(Constants.KEY_AVAILABILITY)
                                ).intValue();

                            }
                            if (documentChange.getDocument().getString(Constants.KEY_IMAGE)!= null){
                                users.get(i).image = documentChange.getDocument().getString(Constants.KEY_IMAGE);
                            }
                            if (documentChange.getDocument().getString(Constants.KEY_NAME)!= null){
                                users.get(i).name = documentChange.getDocument().getString(Constants.KEY_NAME);
                            }
                            usersAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }

            }

        }
    };
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

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
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
                    user.publicKey = queryDocumentSnapshot.getString(Constants.KEY_PUBLIC_KEY);
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
                    user.publicKey = queryDocumentSnapshot.getString(Constants.KEY_PUBLIC_KEY);
                    if (queryDocumentSnapshot.getLong(Constants.KEY_AVAILABILITY)!= null){
                        user.availability = Objects.requireNonNull(queryDocumentSnapshot.getLong(Constants.KEY_AVAILABILITY)).intValue();
                    }
                    users.add(user);
                }

                if(users.size() > 0){
                    UsersAdapter usersAdapter = new UsersAdapter(users,this);
                    binding.usersRecyclerView.setAdapter(usersAdapter);
                    binding.usersRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    binding.usersRecyclerView.setVisibility(View.INVISIBLE);
                }
            } else {
                binding.usersRecyclerView.setVisibility(View.INVISIBLE);
            }
        });



//        y.get()
//                .addOnCompleteListener(task -> {
//                    loading(false);
//                    String currentUserId = preferenceManager.getString(Constants.KEY_USED_ID);
//                    if(task.isSuccessful() && task.getResult() != null){
//                        List<User> users = new ArrayList<>();
//                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
//                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
//                                continue;
//                            }
//                            User user = new User();
//                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
//                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
//                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
//                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
//                            user.id = queryDocumentSnapshot.getId();
//                            users.add(user);
//                        }
//                        if(users.size() > 0){
//                            UsersAdapter usersAdapter = new UsersAdapter(users,this);
//                            binding.usersRecyclerView.setAdapter(usersAdapter);
//                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
//                        } else {
//                            binding.usersRecyclerView.setVisibility(View.INVISIBLE);
//                        }
//                    } else {
//                        binding.usersRecyclerView.setVisibility(View.INVISIBLE);
//                    }
//                });


    }

    @Override
    protected void onPause() {
        super.onPause();
        registrationUserDataChange.remove();
    }
}