package com.example.chatss.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.chatss.fragment.GroupFragment;
import com.example.chatss.fragment.IndivisualFragment;

public class ChatViewPagerAdapter extends FragmentStateAdapter {
    public ChatViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 0:
                return new IndivisualFragment();

            case 1:
                return new GroupFragment();

            default:
                return new IndivisualFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
