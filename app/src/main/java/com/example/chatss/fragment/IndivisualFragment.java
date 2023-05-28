package com.example.chatss.fragment;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.chatss.activities.ChatGroupMainActivity;
import com.example.chatss.activities.UsersActivity;
import com.example.chatss.adapter.RecentConversationsAdapter;
import com.example.chatss.databinding.ActivityMainBinding;
import com.example.chatss.databinding.FragmentIndivisualBinding;
import com.example.chatss.listeners.ConversionListener;
import com.example.chatss.models.ChatMessage;
import com.example.chatss.models.User;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.crypto.SecretKey;

public class IndivisualFragment extends Fragment implements ConversionListener {

    public static final int MY_REQUEST_NOTI_CODE = 0;
    private FragmentIndivisualBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;
    private ListenerRegistration registration;
    private String priKeyStr;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout using DataBindingUtil
        binding = FragmentIndivisualBinding.inflate(inflater, container, false);

        // Get the root view from the binding
        View rootView = binding.getRoot();

        // Perform any further operations with the binding
        preferenceManager = new PreferenceManager(this.getActivity());
        priKeyStr = preferenceManager.getString(Constants.KEY_PRIVATE_KEY);
        if (priKeyStr == null){
            binding.progressBar.setVisibility(View.GONE);
            return rootView;
        }
        init();
        setListeners();
        listenConversations();
        listenUserOnline();
        return rootView;
    }
    public void init(){
        conversations = new ArrayList<>();
        conversationsAdapter = new RecentConversationsAdapter(conversations, this);
        binding.conversationRecyclerView.setAdapter(conversationsAdapter);
        database = FirebaseFirestore.getInstance();
    }
    private void setListeners(){
        binding.fabNewChat.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), UsersActivity.class));
        });
    }


    private void listenUserOnline(){
        registration = database.collection(Constants.KEY_COLLECTION_USERS)
                .addSnapshotListener(eventListener1);
    }

    private  final EventListener<QuerySnapshot> eventListener1 = (value, error) -> {
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

                if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < conversations.size(); i++) {
                        if (conversations.get(i).conversionId.equals(userId)) {

                            if (documentChange.getDocument().getLong(Constants.KEY_AVAILABILITY) != null){
                                conversations.get(i).avaibility = Objects.requireNonNull(
                                        documentChange.getDocument().getLong(Constants.KEY_AVAILABILITY)
                                ).intValue();

                            }
                            if (documentChange.getDocument().getString(Constants.KEY_IMAGE)!= null){
                                conversations.get(i).conversionImage = documentChange.getDocument().getString(Constants.KEY_IMAGE);
                            }
                            if (documentChange.getDocument().getString(Constants.KEY_NAME)!= null){
                                conversations.get(i).conversionName = documentChange.getDocument().getString(Constants.KEY_NAME);
                            }
                            conversationsAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }

        }
    };


    private void listenConversations(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USED_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USED_ID))
                .addSnapshotListener(eventListener);

    }

    private  final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null){
            return;
        }
        if (value != null){
            for (DocumentChange documentChange: value.getDocumentChanges()){

                String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                if (documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;
                    if (preferenceManager.getString(Constants.KEY_USED_ID).equals(senderId)){
                        getInitStatusUser(receiverId ,chatMessage);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversionPublicKey = documentChange.getDocument().getString(Constants.KEY_RECEIVER_PUBLIC_KEY);

                    }else {
                        getInitStatusUser(senderId ,chatMessage);
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        chatMessage.conversionPublicKey = documentChange.getDocument().getString(Constants.KEY_SENDER_PUBLIC_KEY);

                    }

                    try {
                        PrivateKey priKey = ECCc.stringToPrivateKey(priKeyStr);
                        PublicKey pubKey = ECCc.stringToPublicKey(chatMessage.conversionPublicKey);
                        SecretKey secretKey = ECCc.generateSharedSecret(priKey, pubKey);
                        chatMessage.message = ECCc.decryptString(secretKey,documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                    } catch (Exception e) {
                        e.printStackTrace();
                        chatMessage.message = "";
                    }

                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                }else if (documentChange.getType() == DocumentChange.Type.MODIFIED){
                    for (int i = 0; i < conversations.size(); i++){
                        if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)){
                            try {
                                PrivateKey priKey = ECCc.stringToPrivateKey(priKeyStr);
                                PublicKey pubKey = ECCc.stringToPublicKey(conversations.get(i).conversionPublicKey);
                                SecretKey secretKey = ECCc.generateSharedSecret(priKey, pubKey);
                                conversations.get(i).message = ECCc.decryptString(secretKey,documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                            } catch (Exception e) {
                                e.printStackTrace();
                                conversations.get(i).message = "";
                            }
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            Collections.sort(conversations, (obj1, obj2)->obj2.dateObject.compareTo(obj1.dateObject));
            conversationsAdapter.notifyDataSetChanged();
            binding.conversationRecyclerView.smoothScrollToPosition(0);
            binding.conversationRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };

    private void getInitStatusUser(String userId, ChatMessage chatMessage){
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                if (document.getLong(Constants.KEY_AVAILABILITY) != null) {
                                    chatMessage.avaibility = Objects.requireNonNull(document.getLong(Constants.KEY_AVAILABILITY)).intValue();
                                }
                                if (document.getString(Constants.KEY_IMAGE) != null){
                                    chatMessage.conversionImage = document.getString(Constants.KEY_IMAGE);
                                }
                                if (document.getString(Constants.KEY_NAME) != null){
                                    chatMessage.conversionName = document.getString(Constants.KEY_NAME);
                                }
                                conversationsAdapter.notifyDataSetChanged();
                            } else {
                                Log.d("TAG", "Tài liệu không tồn tại.");
                            }
                        } else {
                            Log.d("TAG", "Lỗi khi đọc tài liệu: ", task.getException());
                        }
                    }
                });
    }
    @Override
    public void onConversionClicked(User user) {
        Intent it = new Intent(getActivity(), ChatActivity.class);
        it.putExtra(Constants.KEY_USER,user);
        startActivity(it);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clean up the binding
        binding = null;
        if (registration != null ) registration.remove();
    }
}
