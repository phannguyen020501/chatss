package com.example.chatss.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.provider.CalendarContract;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatss.activities.ChatActivity;
import com.example.chatss.databinding.ItemContainerRecentConversionBinding;
import com.example.chatss.listeners.ConversionListener;
import com.example.chatss.models.ChatMessage;
import com.example.chatss.models.User;
import com.example.chatss.utilities.Constants;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Objects;

public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.ConversiongViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final ConversionListener conversionListener;
    private FirebaseFirestore database;

    String conversationId;

    public RecentConversationsAdapter(List<ChatMessage> chatMessages, ConversionListener conversionListener) {
        this.chatMessages = chatMessages;
        this.conversionListener = conversionListener;
    }

    @NonNull
    @Override
    public ConversiongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversiongViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversiongViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }


    class ConversiongViewHolder extends RecyclerView.ViewHolder {

        ItemContainerRecentConversionBinding binding;

        public ConversiongViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(ChatMessage chatMessage) {
//            if (chatMessage.avaibility == 1){
//                binding.imageStatus.setColorFilter(Color.rgb(0,255,0));
//            }else {
//                binding.imageStatus.setColorFilter(Color.rgb(255,165,0));
//            }

            binding.setAvailable(chatMessage.avaibility);

            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
            binding.textName.setText(chatMessage.conversionName);
            binding.textRecentMessage.setText(chatMessage.message);

            database = FirebaseFirestore.getInstance();

            binding.getRoot().setOnClickListener(view -> {
                User user = new User();
                user.id = chatMessage.conversionId;
                user.name = chatMessage.conversionName;
                user.image = chatMessage.conversionImage;

                conversionListener.onConversionClicked(user);

            });
        }
    }

    private Bitmap getConversionImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0, bytes.length);
    }
}
