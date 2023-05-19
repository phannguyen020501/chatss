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

import com.example.chatss.databinding.ItemContainerUserBinding;
import com.example.chatss.listeners.RoomChatListener;
import com.example.chatss.listeners.UserListener;
import com.example.chatss.models.RoomChat;
import com.example.chatss.models.User;
import com.example.chatss.utilities.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.UserViewHolder>{

    private  final List<RoomChat> roomChats;
    private final RoomChatListener roomChatListener;

    public ChatRoomAdapter(List<RoomChat> roomChats, RoomChatListener roomChatListener) {
        this.roomChats = roomChats;
        this.roomChatListener = roomChatListener;
    }

    @NonNull
    @Override
    public ChatRoomAdapter.UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ChatRoomAdapter.UserViewHolder(itemContainerUserBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomAdapter.UserViewHolder holder, int position) {
        holder.setUserData(roomChats.get(position));
    }

    @Override
    public int getItemCount() {
        return roomChats.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder{
        ItemContainerUserBinding binding;

        UserViewHolder(ItemContainerUserBinding itemContainerUserBinding){
            super(itemContainerUserBinding.getRoot());
            binding = itemContainerUserBinding;
        }

        void  setUserData(RoomChat roomChat){
            FirebaseFirestore database = FirebaseFirestore.getInstance();
           
            binding.textName.setText(roomChat.name);
            binding.textEmail.setText(roomChat.id.toString());
            binding.getRoot().setOnClickListener(v -> roomChatListener.onRoomChatClicked(roomChat));
        }
    }

    private Bitmap getUserImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
