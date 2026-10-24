package com.aichat.mini;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.aichat.mini.api.ApiClient;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private ApiClient client;
    private RadioGroup rgModels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.settings);

        client = ApiClient.get(this);
        TextInputEditText etKey = findViewById(R.id.etApiKey);
        TextInputEditText etUrl = findViewById(R.id.etBaseUrl);
        Button btnSave = findViewById(R.id.btnSave);
        TextView tvModelResult = findViewById(R.id.tvModelResult);
        rgModels = findViewById(R.id.rgModels);

        etKey.setText(client.getApiKey());
        etUrl.setText(client.getBaseUrl());

        // 已有缓存模型则先填充
        if (!client.getModelsList().isEmpty()) {
            populateModels(client.getModelsList(), tvModelResult);
        }

        btnSave.setOnClickListener(v -> {
            client.setApiKey(etKey.getText().toString().trim());
            client.setBaseUrl(etUrl.getText().toString().trim());
            tvModelResult.setText(R.string.fetching_models);
            rgModels.removeAllViews();
            client.fetchModels(new ApiClient.ModelsCallback() {
                @Override public void onSuccess(List<String> models) {
                    runOnUiThread(() -> {
                        populateModels(models, tvModelResult);
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

        rgModels.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == -1) return;
            RadioButton rb = findViewById(checkedId);
            if (rb != null) {
                client.setSelectedModel(rb.getText().toString());
            }
        });
    }

    private void populateModels(List<String> models, TextView tvResult) {
        tvResult.setText(getString(R.string.models_found, models.size()));
        rgModels.removeAllViews();
        String selected = client.getSelectedModel();
        int idx = 0;
        for (String m : models) {
            RadioButton rb = new RadioButton(this);
            rb.setId(idx + 1);
            rb.setText(m);
            rgModels.addView(rb);
            if (m.equals(selected)) rgModels.check(rb.getId());
            idx++;
        }
    }
}