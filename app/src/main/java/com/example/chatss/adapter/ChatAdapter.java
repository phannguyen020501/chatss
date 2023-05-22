package com.example.chatss.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatss.databinding.ItemContainerReceivedMessageBinding;
import com.example.chatss.databinding.ItemContainerSentMessageBinding;
import com.example.chatss.listeners.DownloadImageListener;
import com.example.chatss.models.ChatMessage;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ChatAdapter extends  RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private  final List<ChatMessage> chatMessages;
    private final String senderId;
    private Bitmap receiverProfileImage;
    private int size;
    private FirebaseFirestore database = FirebaseFirestore.getInstance();

    private final DownloadImageListener downloadImageListener;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public void setReceiverProfileImage(Bitmap bitmap){
        receiverProfileImage = bitmap;
    }

    public ChatAdapter(List<ChatMessage> chatMessages, String senderId, Bitmap receiverProfileImage, DownloadImageListener downloadImageListener) {
        this.chatMessages = chatMessages;
        this.senderId = senderId;
        this.receiverProfileImage = receiverProfileImage;
        this.downloadImageListener = downloadImageListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }else {
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT){
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position), position);

        }else {
            ((ReceivedMessageViewHolder) holder).setData(chatMessages.get(position), position, receiverProfileImage);

        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).senderId.equals(senderId)){
            return VIEW_TYPE_SENT;
        }else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    class SentMessageViewHolder extends RecyclerView.ViewHolder{
        private final ItemContainerSentMessageBinding binding;

        public SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessage chatMessage, int position){
            size = chatMessages.size();
            if(chatMessage!=null){
                //Log.d("aaaaP", chatMessage.message + "   " + position + "   vt:" + chatMessages.size());
                binding.textDateTime.setVisibility(View.GONE);
                binding.textSeen.setVisibility(View.GONE);

                if(chatMessage.type.equals("image")){
                    binding.textMessage.setVisibility(View.GONE);
                    binding.imgChat.setVisibility(View.VISIBLE);
                    binding.imgChat.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(binding.textDateTime.getVisibility() == View.VISIBLE){
                                binding.textDateTime.setVisibility(View.GONE);
                            } else {
                                binding.textDateTime.setVisibility(View.VISIBLE);
                                binding.textDateTime.setText(chatMessage.dateTime);

                            }
                        }
                    });

                    if(position == chatMessages.size() -1) {
                        isSeen(chatMessage, position);
                    }
                    else {
                        binding.textDateTime.setVisibility(View.GONE);
                        binding.textSeen.setVisibility(View.GONE);
                    }


                    Picasso.get().load(Uri.parse(chatMessage.message)).into(binding.imgChat);
                    binding.imgChat.setOnLongClickListener(view -> {
                        downloadImageListener.onItemClick(chatMessage);
                        return true;
                    });
                }
                else {
                    binding.textMessage.setVisibility(View.VISIBLE);
                    binding.imgChat.setVisibility(View.GONE);
                    binding.textMessage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(binding.textDateTime.getVisibility() == View.VISIBLE){
                                binding.textDateTime.setVisibility(View.GONE);
                            } else {
                                binding.textDateTime.setVisibility(View.VISIBLE);
                                binding.textDateTime.setText(chatMessage.dateTime);

                            }
                        }
                    });

                    if(position == chatMessages.size() -1){
                        isSeen(chatMessage, position);
                    }
                    else {
                        binding.textDateTime.setVisibility(View.GONE);
                        binding.textSeen.setVisibility(View.GONE);
                    }

                    binding.textMessage.setText(chatMessage.message);
                }
            }

        }
        private void isSeen(ChatMessage chat, int position){
            database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).whereEqualTo(Constants.MESS_SENDER_ID, chat.senderId)
                    .whereEqualTo(Constants.MESS_RECEIVER_ID, chat.receiverId)
                    .addSnapshotListener(
                            (value, error) -> {
                                if (error != null) {
                                    return;
                                }
                                if (value != null) {
                                    for (DocumentChange documentChange : value.getDocumentChanges()) {

                                        if (getAdapterPosition() == position) {
                                            if (Boolean.TRUE.equals(documentChange.getDocument().getBoolean(Constants.isSeen))) {
                                                binding.textDateTime.setVisibility(View.VISIBLE);
                                                binding.textDateTime.setText(chat.dateTime);
                                                binding.textSeen.setVisibility(View.VISIBLE);
                                                binding.textSeen.setText("Seen");

                                            } else {
                                                binding.textDateTime.setVisibility(View.VISIBLE);
                                                binding.textDateTime.setText(chat.dateTime);
                                                binding.textSeen.setVisibility(View.VISIBLE);
                                                binding.textSeen.setText("Delivered");
                                            }
                                        }
                                    }
                                }
                            }
                    );
//                    .get()
//                    .addOnCompleteListener(task -> {
//                        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0 ) {
//                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
//                            if(getAdapterPosition()==position)
//                            {
//                                if(documentSnapshot.getBoolean(Constants.isSeen)) {
//                                    binding.textDateTime.setVisibility(View.VISIBLE);
//                                    binding.textDateTime.setText(chat.dateTime);
//                                    binding.textSeen.setVisibility(View.VISIBLE);
//                                    binding.textSeen.setText("Seen");
//
//                                } else{
//                                    binding.textDateTime.setVisibility(View.VISIBLE);
//                                    binding.textDateTime.setText(chat.dateTime);
//                                    binding.textSeen.setVisibility(View.VISIBLE);
//                                    binding.textSeen.setText("Delivered");
//                                }
//                            }
//                        }
//                    });

        }
    }

    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder{
        private final ItemContainerReceivedMessageBinding binding;

        public ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }


        void setData(ChatMessage chatMessage, int position, Bitmap receiverProfileImage){

            //Log.d("aaaaT", chatMessage.message + "   " + position + "   vt:" + chatMessages.size());
            size = chatMessages.size();

            if(receiverProfileImage!=null){
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }

            binding.textDateTime.setVisibility(View.GONE);
            binding.textSeen.setVisibility(View.GONE);

            if(position == size -1) isSeen(chatMessage, position);

            if(chatMessage.type.equals("image")){
                binding.textMessage.setVisibility(View.GONE);
                binding.imgChat.setVisibility(View.VISIBLE);
                binding.imgChat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(binding.textDateTime.getVisibility() == View.VISIBLE){
                            binding.textDateTime.setVisibility(View.GONE);
                        } else {
                            binding.textDateTime.setVisibility(View.VISIBLE);
                            binding.textDateTime.setText(chatMessage.dateTime);
                        }
                    }
                });
                Picasso.get().load(Uri.parse(chatMessage.message)).into(binding.imgChat);
                binding.imgChat.setOnLongClickListener(view -> {
                    downloadImageListener.onItemClick(chatMessage);
                    return true;
                });
            }
            else {
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.imgChat.setVisibility(View.GONE);
                binding.textMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(binding.textDateTime.getVisibility() == View.VISIBLE){
                            binding.textDateTime.setVisibility(View.GONE);
                        } else {
                            binding.textDateTime.setVisibility(View.VISIBLE);
                            binding.textDateTime.setText(chatMessage.dateTime);
                        }
                    }
                });
                binding.textMessage.setText(chatMessage.message);
            }
        }

        private void isSeen(ChatMessage chat, int position){
            database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).whereEqualTo(Constants.MESS_SENDER_ID, chat.senderId)
                    .whereEqualTo(Constants.MESS_RECEIVER_ID, chat.receiverId).get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0 ) {
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                            if(getAdapterPosition()==position)
                            {
                                if(documentSnapshot.getBoolean(Constants.isSeen)) {
                                    binding.textDateTime.setVisibility(View.VISIBLE);
                                    binding.textDateTime.setText(chat.dateTime);
                                    binding.textSeen.setVisibility(View.VISIBLE);
                                    binding.textSeen.setText("Seen");

                                } else{
                                    binding.textDateTime.setVisibility(View.VISIBLE);
                                    binding.textDateTime.setText(chat.dateTime);
                                    binding.textSeen.setVisibility(View.VISIBLE);
                                    binding.textSeen.setText("Delivered");
                                }
                            }
                        }
                    });
        }
    }

}

