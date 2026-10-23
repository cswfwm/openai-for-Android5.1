package com.aichat.mini;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.aichat.mini.api.ApiClient;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.settings);

        ApiClient client = ApiClient.get(this);
        TextInputEditText etKey = findViewById(R.id.etApiKey);
        TextInputEditText etUrl = findViewById(R.id.etBaseUrl);
        Button btnSave = findViewById(R.id.btnSave);

        etKey.setText(client.getApiKey());
        etUrl.setText(client.getBaseUrl());

        btnSave.setOnClickListener(v -> {
            client.setApiKey(etKey.getText().toString().trim());
            client.setBaseUrl(etUrl.getText().toString().trim());
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}