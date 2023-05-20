package com.example.chatss.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatss.databinding.ItemContainerReceivedMessageBinding;
import com.example.chatss.databinding.ItemContainerSentMessageBinding;
import com.example.chatss.models.ChatMessage;

import java.util.List;

public class ChatGroupAdapter extends  RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private  final List<ChatMessage> chatMessages;
    private final String senderId;


    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;


    public ChatGroupAdapter(List<ChatMessage> chatMessages, String senderId) {
        this.chatMessages = chatMessages;
        this.senderId = senderId;

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            return new ChatGroupAdapter.SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }else {
            return new ChatGroupAdapter.ReceivedMessageViewHolder(
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
            ((ChatGroupAdapter.SentMessageViewHolder) holder).setData(chatMessages.get(position));
        }else {
            ((ChatGroupAdapter.ReceivedMessageViewHolder) holder).setData(chatMessages.get(position));
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

    static class SentMessageViewHolder extends RecyclerView.ViewHolder{
        private final ItemContainerSentMessageBinding binding;

        public SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessage chatMessage){
            if(chatMessage!=null){
                if(chatMessage.type.equals("text")){
                    binding.textMessage.setText(chatMessage.message);
                    binding.textDateTime.setText(chatMessage.dateTime);
                } else if(chatMessage.type.equals("image")){

                    binding.textMessage.setVisibility(View.GONE);
                    binding.imgChat.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext()).load(chatMessage.message).into(binding.imgChat);
                    binding.textDateTime.setText(chatMessage.dateTime);
                }
            }
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder{
        private final ItemContainerReceivedMessageBinding binding;

        public ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }


        void setData(ChatMessage chatMessage){
            if(chatMessage.imageSender!=null){
                binding.imageProfile.setImageBitmap(getBitmapFromEncodedString(chatMessage.imageSender));
            }
            binding.textDateTime.setText(chatMessage.dateTime);
            if(chatMessage.type.equals("text")){
                binding.textMessage.setText(chatMessage.message);
            } else if(chatMessage.type.equals("image")){
                binding.textMessage.setVisibility(View.GONE);
                binding.imgChat.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(chatMessage.message).into(binding.imgChat);
                binding.textDateTime.setText(chatMessage.dateTime);
            }
        }
    }
    private static Bitmap getBitmapFromEncodedString(String encodedImage){
        if(encodedImage != null){
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        } else {
            return null;
        }
    }
}
