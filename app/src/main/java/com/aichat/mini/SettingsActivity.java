package com.aichat.mini;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.aichat.mini.api.ApiClient;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;

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
        TextView tvModelResult = findViewById(R.id.tvModelResult);

        etKey.setText(client.getApiKey());
        etUrl.setText(client.getBaseUrl());

        btnSave.setOnClickListener(v -> {
            client.setApiKey(etKey.getText().toString().trim());
            client.setBaseUrl(etUrl.getText().toString().trim());
            tvModelResult.setText(R.string.fetching_models);
            // 输入 API 和 key 后自动识别可用模型
            client.fetchModels(new ApiClient.ModelsCallback() {
                @Override public void onSuccess(List<String> models) {
                    runOnUiThread(() -> {
                        StringBuilder sb = new StringBuilder("可用模型 " + models.size() + " 个：\n");
                        for (String m : models) sb.append("· ").append(m).append("\n");
                        tvModelResult.setText(sb.toString());
                        Toast.makeText(SettingsActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                    });
                }
                @Override public void onFailure(String error) {
                    runOnUiThread(() -> {
                        tvModelResult.setText(R.string.no_models + " (" + error + ")");
                        Toast.makeText(SettingsActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }
}