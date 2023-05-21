package com.example.chatss.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.chatss.databinding.ActivityChatGroupBinding;
import com.example.chatss.databinding.ActivitySettingGroupBinding;

public class SettingGroupActivity extends BaseActivity {
    private ActivitySettingGroupBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}