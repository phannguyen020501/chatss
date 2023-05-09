package com.example.chatss.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.SearchView;

import com.example.chatss.R;
import com.example.chatss.adapter.UsersAdapter;
import com.example.chatss.databinding.ActivityUsersBinding;
import com.example.chatss.listeners.UserListener;
import com.example.chatss.models.User;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsersActivity extends AppCompatActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager((getApplicationContext()));
        setListener();
        getUsers();
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
                            UsersAdapter usersAdapter = new UsersAdapter(users,this);
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
}