package com.sportssync.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.sportssync.app.R;
import java.util.List;

public class QRScannerActivity extends AppCompatActivity {

    private DecoratedBarcodeView barcodeScanner;
    private TextView tvScanInstruction;
    private String scanType;
    private boolean isScanned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        scanType = getIntent().getStringExtra("scanType");

        initViews();
        setupToolbar();
        startScanning();
    }

    private void initViews() {
        barcodeScanner = findViewById(R.id.barcodeScanner);
        tvScanInstruction = findViewById(R.id.tvScanInstruction);

        if (scanType != null && scanType.equals("attendance")) {
            tvScanInstruction.setText("Scan attendance QR code");
        } else if (scanType != null && scanType.equals("registration")) {
            tvScanInstruction.setText("Scan registration QR code");
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void startScanning() {
        barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (isScanned) return;

                if (result.getText() != null) {
                    isScanned = true;
                    barcodeScanner.pause();
                    handleScanResult(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });
    }

    private void handleScanResult(String qrData) {
        if (scanType != null && scanType.equals("registration")) {
            if (qrData.equals("REGISTER_SPORTS_SYNC")) {
                Intent intent = new Intent();
                intent.putExtra("qrData", qrData);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(this, R.string.invalid_qr, Toast.LENGTH_SHORT).show();
                isScanned = false;
                barcodeScanner.resume();
            }
        } else if (scanType != null && scanType.equals("attendance")) {
            if (qrData.startsWith("ATTENDANCE_")) {
                Intent intent = new Intent();
                intent.putExtra("qrData", qrData);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(this, R.string.invalid_qr, Toast.LENGTH_SHORT).show();
                isScanned = false;
                barcodeScanner.resume();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeScanner.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeScanner.pause();
    }
}