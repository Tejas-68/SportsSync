package com.project.sportssync;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.UUID;

public class QRGeneratorActivity extends AppCompatActivity {

    private ImageView ivQRCode;
    private Button btnGenerateQR;
    private String currentQRValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_generator);

        ivQRCode = findViewById(R.id.ivQRCode);
        btnGenerateQR = findViewById(R.id.btnGenerateQR);

        btnGenerateQR.setOnClickListener(v -> generateQRCode());
        
        // Generate initial QR code
        generateQRCode();
    }

    private void generateQRCode() {
        currentQRValue = UUID.randomUUID().toString();
        
        try {
            Bitmap bitmap = generateQRBitmap(currentQRValue, 512, 512);
            ivQRCode.setImageBitmap(bitmap);
            Toast.makeText(this, "QR Code Generated", Toast.LENGTH_SHORT).show();
        } catch (WriterException e) {
            Toast.makeText(this, "Error generating QR: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap generateQRBitmap(String content, int width, int height) throws WriterException {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height
        );

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return bitmap;
    }
}
