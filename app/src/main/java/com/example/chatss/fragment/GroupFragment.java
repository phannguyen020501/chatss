package com.example.chatss.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.chatss.ECC.ECCc;
import com.example.chatss.activities.ChatActivity;
import com.example.chatss.activities.ChatGroupActivity;
import com.example.chatss.activities.CreateGroupActivity;
import com.example.chatss.activities.MainActivity;
import com.example.chatss.activities.ProfileActivity;
import com.example.chatss.activities.UsersActivity;
import com.example.chatss.adapter.ChatRoomAdapter;
import com.example.chatss.databinding.ActivityChatGroupMainBinding;
import com.example.chatss.databinding.FragmentIndivisualBinding;
import com.example.chatss.listeners.ConversionListener;
import com.example.chatss.listeners.RoomChatListener;
import com.example.chatss.models.ChatMessage;
import com.example.chatss.models.RoomChat;
import com.example.chatss.models.User;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.crypto.SecretKey;

public class GroupFragment extends Fragment implements RoomChatListener {

    private FragmentIndivisualBinding binding;
    private PreferenceManager preferenceManager;
    private ChatRoomAdapter chatRoomAdapter;
    private List<RoomChat> roomChats;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout using DataBindingUtil
        binding = FragmentIndivisualBinding.inflate(inflater, container, false);

        // Get the root view from the binding
        View rootView = binding.getRoot();

        // Perform any further operations with the binding
        preferenceManager = new PreferenceManager(getActivity());
        init();
        setListeners();
        getRooms();
        listenRooms();
        return rootView;
    }

    private void init() {

        roomChats = new ArrayList<>();
        chatRoomAdapter = new ChatRoomAdapter(roomChats,  this);
        binding.conversationRecyclerView.setAdapter(chatRoomAdapter);
    }

    private void setListeners(){
        binding.fabNewChat.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CreateGroupActivity.class));
        });
    }
    private void getRooms(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection("ListRoomUser").document(preferenceManager.getString(Constants.KEY_USED_ID)).collection("ListRoom")
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if(task.isSuccessful() && task.getResult() != null){
                        List<RoomChat> roomChats = new ArrayList<>();
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){

                            RoomChat roomChat = new RoomChat();
                            roomChat.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            roomChat.id = queryDocumentSnapshot.getString("id");
                            roomChat.lastMessage = queryDocumentSnapshot.getString("lastMessage");
                            roomChat.dateObject = queryDocumentSnapshot.getDate(Constants.KEY_TIMESTAMP);
                            roomChat.senderName = queryDocumentSnapshot.getString("senderName");
                            //Toast.makeText(getApplicationContext(),roomChat.lastMessage, Toast.LENGTH_SHORT).show();
                            roomChats.add(roomChat);
                        }
                        if(roomChats.size() > 0){
                            ChatRoomAdapter chatRoomAdapter = new ChatRoomAdapter(roomChats,  this);
                            binding.conversationRecyclerView.setAdapter(chatRoomAdapter);
                            binding.conversationRecyclerView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        showToast("Error when get list group");
                    }
                });
    }
    private void listenRooms(){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection("ListRoomUser").document(preferenceManager.getString(Constants.KEY_USED_ID)).collection("ListRoom")
                .addSnapshotListener((value, error) -> {

                    if (error != null){
                        return;
                    }
                    if (value != null){

                        for(DocumentChange documentChange: value.getDocumentChanges()){
                            if (documentChange.getType() == DocumentChange.Type.ADDED) {
                                RoomChat roomChat = new RoomChat();
                                roomChat.name = documentChange.getDocument().getString(Constants.KEY_NAME);
                                roomChat.id = documentChange.getDocument().getString("id");
                                roomChat.lastMessage = documentChange.getDocument().getString("lastMessage");
                                roomChat.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                                roomChat.senderName = documentChange.getDocument().getString("senderName");
                                //Toast.makeText(getApplicationContext(),roomChat.lastMessage, Toast.LENGTH_SHORT).show();
                                roomChats.add(roomChat);
                            }
//                            else if (documentChange.getType() == DocumentChange.Type.MODIFIED){
//                                for (int i = 0; i < roomChats.size(); i++){
//                                    if (roomChats.get(i).id.equals(documentChange.getDocument().getString("id"))){
//
//                                        roomChats.get(i).lastMessage = documentChange.getDocument().getString("lastMessage");
//                                        roomChats.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
//                                        roomChats.get(i).senderName = documentChange.getDocument().getString("senderName");
//                                        break;
//                                    }
//                                }
//                            }
                        }
                        Collections.sort(roomChats, (obj1, obj2)->{
                            if (obj1.dateObject == null && obj2.dateObject == null) {
                                return 0; // Cả hai đều null, coi như bằng nhau
                            } else if (obj1.dateObject == null) {
                                return 1; // obj1 là null, đặt obj2 trước
                            } else if (obj2.dateObject == null) {
                                return -1; // obj2 là null, đặt obj1 trước
                            } else {
                                return obj2.dateObject.compareTo(obj1.dateObject);
                            }
                        });
                        chatRoomAdapter.notifyDataSetChanged();
                        binding.conversationRecyclerView.smoothScrollToPosition(0);
                        binding.conversationRecyclerView.setAdapter(chatRoomAdapter);
                        binding.conversationRecyclerView.setVisibility(View.VISIBLE);
                        binding.progressBar.setVisibility(View.GONE);

                    } else {
                        showToast("Error when get list group");
                    }
                });
        database.collection("ListRoomUser").document(preferenceManager.getString(Constants.KEY_USED_ID)).collection("ListRoom")
                        .get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()){
                                for (DocumentSnapshot documentSnapshot : task.getResult()){
                                    database.collection("RoomChat").document(documentSnapshot.getId())
                                            .get().addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()){
                                                    Boolean isSame = false;
                                                    DocumentSnapshot documentSnapshot1 = task1.getResult();
                                                    RoomChat roomChat = new RoomChat();
                                                    roomChat.name = documentSnapshot1.getString(Constants.KEY_NAME);
                                                    roomChat.id = documentSnapshot1.getString("id");
                                                    roomChat.lastMessage = documentSnapshot1.getString("lastMessage");
                                                    roomChat.dateObject = documentSnapshot1.getDate(Constants.KEY_TIMESTAMP);
                                                    roomChat.senderName = documentSnapshot1.getString("senderName");
                                                    //Toast.makeText(getApplicationContext(),roomChat.lastMessage, Toast.LENGTH_SHORT).show();
                                                    for (int i = 0; i < roomChats.size(); i++){
                                                        if (roomChats.get(i).id.equals(roomChat.id)){
                                                            isSame = true;
                                                            break;
                                                        }
                                                    }
                                                    if (!isSame){
                                                        roomChats.add(roomChat);
                                                        Collections.sort(roomChats, (obj1, obj2)->{
                                                            if (obj1.dateObject == null && obj2.dateObject == null) {
                                                                return 0; // Cả hai đều null, coi như bằng nhau
                                                            } else if (obj1.dateObject == null) {
                                                                return 1; // obj1 là null, đặt obj2 trước
                                                            } else if (obj2.dateObject == null) {
                                                                return -1; // obj2 là null, đặt obj1 trước
                                                            } else {
                                                                return obj2.dateObject.compareTo(obj1.dateObject);
                                                            }
                                                        });
                                                        chatRoomAdapter.notifyDataSetChanged();
                                                        binding.conversationRecyclerView.smoothScrollToPosition(0);
                                                        binding.conversationRecyclerView.setAdapter(chatRoomAdapter);
                                                        binding.conversationRecyclerView.setVisibility(View.VISIBLE);
                                                        binding.progressBar.setVisibility(View.GONE);
                                                    }


                                                }
                                            });
                                }
                            }else {
                                showToast("Error when get list group 1");
                            }
                });
        database.collection("RoomChat")
                .addSnapshotListener((value, error) -> {

                    if (error != null){
                        return;
                    }
                    if (value != null){

                        for(DocumentChange documentChange: value.getDocumentChanges()){
//                            if (documentChange.getType() == DocumentChange.Type.ADDED) {
//                                RoomChat roomChat = new RoomChat();
//                                roomChat.name = documentChange.getDocument().getString(Constants.KEY_NAME);
//                                roomChat.id = documentChange.getDocument().getString("id");
//                                roomChat.lastMessage = documentChange.getDocument().getString("lastMessage");
//                                roomChat.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
//                                roomChat.senderName = documentChange.getDocument().getString("senderName");
//                                //Toast.makeText(getApplicationContext(),roomChat.lastMessage, Toast.LENGTH_SHORT).show();
//                                roomChats.add(roomChat);
//                            }else
                                if (documentChange.getType() == DocumentChange.Type.MODIFIED){
                                for (int i = 0; i < roomChats.size(); i++){
                                    if (roomChats.get(i).id.equals(documentChange.getDocument().getString("id"))){

                                        roomChats.get(i).lastMessage = documentChange.getDocument().getString("lastMessage");
                                        roomChats.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                                        roomChats.get(i).senderName = documentChange.getDocument().getString("senderName");
                                        roomChats.get(i).image = documentChange.getDocument().getString("image");
                                        roomChats.get(i).name = documentChange.getDocument().getString("name");
                                        break;
                                    }
                                }
                            }
                        }
                        Collections.sort(roomChats, (obj1, obj2)->{
                            if (obj1.dateObject == null && obj2.dateObject == null) {
                                return 0; // Cả hai đều null, coi như bằng nhau
                            } else if (obj1.dateObject == null) {
                                return 1; // obj1 là null, đặt obj2 trước
                            } else if (obj2.dateObject == null) {
                                return -1; // obj2 là null, đặt obj1 trước
                            } else {
                                return obj2.dateObject.compareTo(obj1.dateObject);
                            }
                        });
                        chatRoomAdapter.notifyDataSetChanged();
                        binding.conversationRecyclerView.smoothScrollToPosition(0);
                        binding.conversationRecyclerView.setAdapter(chatRoomAdapter);
                        binding.conversationRecyclerView.setVisibility(View.VISIBLE);
                        binding.progressBar.setVisibility(View.GONE);

                    } else {
                        showToast("Error when get list group");
                    }
                });
    }
    private void showToast(String message){
        Toast.makeText(getActivity(),message, Toast.LENGTH_SHORT).show();
    }
    private void loading(boolean isLoading) {
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clean up the binding
        binding = null;
    }

    @Override
    public void onRoomChatClicked(RoomChat roomChat) {
        Intent intent = new Intent(getActivity(), ChatGroupActivity.class);
        intent.putExtra(Constants.KEY_ROOM, roomChat);
        startActivity(intent);
    }
}
