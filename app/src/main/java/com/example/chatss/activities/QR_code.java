package com.example.chatss.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.chatss.databinding.ActivityQrCodeBinding;
import com.example.chatss.utilities.Constants;
import com.example.chatss.utilities.PreferenceManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONException;
import org.json.JSONObject;

public class QR_code extends AppCompatActivity {

    private ActivityQrCodeBinding binding;
    public static String STR ;

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrCodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ImageView imageView = binding.myImage;
        preferenceManager = new PreferenceManager((getApplicationContext()));

        // Tạo dữ liệu key-value
        JSONObject dataObject = new JSONObject();
        try {
            dataObject.put("email", preferenceManager.getString(Constants.KEY_EMAIL));
            dataObject.put("privateKey", preferenceManager.getString(Constants.KEY_PRIVATE_KEY));

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Fail when put data to JSON object", Toast.LENGTH_SHORT).show();
        }

        // Chuyển đổi dữ liệu thành định dạng JSON
        STR = dataObject.toString();

        try {
            // Tính toán kích thước ảnh QR dựa trên kích thước màn hình
            int qrCodeSize = getQRCodeSize();
            //Tạo qr
            Bitmap bitmap = encodeAsBitmap(STR, qrCodeSize);
            // Hiển thị lên imv
            imageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Fail when gen QR", Toast.LENGTH_SHORT).show();
        }
        binding.okBtn.setOnClickListener(v ->{
            onBackPressed();
        });
    }


    private int getQRCodeSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        int smallerDimension = Math.min(screenWidth, screenHeight);
        return (int) (smallerDimension * 0.8); // Sử dụng tỷ lệ 80% của kích thước màn hình
    }
    Bitmap encodeAsBitmap(String str, int qrCodeSize) throws WriterException {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        Bitmap bitmap = barcodeEncoder.encodeBitmap(str, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize);
        return bitmap;
    }
}