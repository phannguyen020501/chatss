package com.example.chatss.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatss.R;
import com.example.chatss.databinding.ItemContainerUserGroupBinding;
import com.example.chatss.listeners.UserListener;
import com.example.chatss.models.User;
import com.example.chatss.models.UserGroup;
import com.example.chatss.utilities.Constants;

import java.util.List;

public class UsersGroupAdapter extends RecyclerView.Adapter<UsersGroupAdapter.UserViewHolder>{

    private  final List<User> users;
    private final UserListener userListener;

    public UsersGroupAdapter(List<User> users, UserListener userListener) {
        this.users = users;
        this.userListener = userListener;
    }

    @NonNull
    @Override
    public UsersGroupAdapter.UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserGroupBinding itemContainerUserGroupBinding = ItemContainerUserGroupBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UsersGroupAdapter.UserViewHolder(itemContainerUserGroupBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersGroupAdapter.UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        for(int i=0;i<users.size();i++)
        {
            Constants.userGroups.add(new UserGroup(users.get(i).id,"0",users.get(i).name,users.get(i).email,users.get(i).image,users.get(i).token));
        }
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder{
        ItemContainerUserGroupBinding binding;

        UserViewHolder(ItemContainerUserGroupBinding itemContainerUserGroupBinding){
            super(itemContainerUserGroupBinding.getRoot());
            binding = itemContainerUserGroupBinding;
        }

        void  setUserData(User user){
            binding.textName.setText(user.name);
            binding.textEmail.setText(user.email);
            binding.imageProfile.setImageBitmap(getUserImage(user.image));
            binding.getRoot().setOnClickListener(v -> {
                binding.checkboxMeat.setChecked(!binding.checkboxMeat.isChecked());
            });
            if (user.availability != null){
                if (user.availability == 1) binding.imageStatus.setBackgroundResource(R.drawable.background_online);
                else binding.imageStatus.setBackgroundResource(R.drawable.background_offline);
            }else {
                binding.imageStatus.setVisibility(View.GONE);
            }
            binding.checkboxMeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(b){
                        for(int i=0;i<users.size();i++){
                            if(user.id.equals(users.get(i).id)){
                                Constants.userGroups.set(i,new UserGroup(
                                        user.id,
                                        "1",
                                        users.get(i).name,
                                        users.get(i).email,
                                        users.get(i).image,
                                        users.get(i).token
                                        )
                                );
                            }
                        }

                        Toast.makeText(itemView.getContext(), "Bạn đã chọn", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        for(int i=0;i<users.size();i++){
                            if(user.id.equals(users.get(i).id)){
                                Constants.userGroups.set(i,new UserGroup(
                                        user.id,
                                        "0",
                                        users.get(i).name,
                                        users.get(i).email,
                                        users.get(i).image,
                                        users.get(i).token
                                        )
                                );
                            }
                        }
                        Toast.makeText(itemView.getContext(), "Bạn đã bỏ chọn", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private Bitmap getUserImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}

