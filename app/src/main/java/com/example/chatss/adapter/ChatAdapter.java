package com.example.chatss.adapter;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatss.databinding.ItemContainerReceivedMessageBinding;
import com.example.chatss.databinding.ItemContainerSentMessageBinding;
import com.example.chatss.listeners.DownloadImageListener;
import com.example.chatss.models.ChatMessage;
import com.squareup.picasso.Picasso;
import java.util.List;

public class ChatAdapter extends  RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private  final List<ChatMessage> chatMessages;
    private final String senderId;
    private Bitmap receiverProfileImage;

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
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        }else {
            ((ReceivedMessageViewHolder) holder).setData(chatMessages.get(position), receiverProfileImage);
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

        void setData(ChatMessage chatMessage){
            if(chatMessage!=null){
                if(chatMessage.type.equals("image")){
                    binding.textMessage.setVisibility(View.GONE);
                    binding.imgChat.setVisibility(View.VISIBLE);
                    Picasso.get().load(Uri.parse(chatMessage.message)).into(binding.imgChat);
                    binding.textDateTime.setText(chatMessage.dateTime);
                    binding.imgChat.setOnClickListener(view -> {
                        downloadImageListener.onItemClick(chatMessage);
                    });
                }
                else {
                    binding.textMessage.setVisibility(View.VISIBLE);
                    binding.imgChat.setVisibility(View.GONE);
                    binding.textMessage.setText(chatMessage.message);
                    binding.textDateTime.setText(chatMessage.dateTime);
                }

            }
        }
    }

    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder{
        private final ItemContainerReceivedMessageBinding binding;

        public ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }


        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage){
            binding.textDateTime.setText(chatMessage.dateTime);
            if(receiverProfileImage!=null){
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
            if(chatMessage.type.equals("image")){
                binding.textMessage.setVisibility(View.GONE);
                binding.imgChat.setVisibility(View.VISIBLE);
                Picasso.get().load(Uri.parse(chatMessage.message)).into(binding.imgChat);
                binding.imgChat.setOnClickListener(view -> {
                    downloadImageListener.onItemClick(chatMessage);
                });
            }
            else {
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.imgChat.setVisibility(View.GONE);
                binding.textMessage.setText(chatMessage.message);
            }
        }
    }
}
