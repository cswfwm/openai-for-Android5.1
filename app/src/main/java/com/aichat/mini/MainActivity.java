package com.aichat.mini;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.aichat.mini.api.ApiClient;
import com.aichat.mini.api.model.ChatModels;
import com.aichat.mini.api.model.Role;
import com.aichat.mini.ui.MessageAdapter;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<ChatModels.Message> messages = new ArrayList<>();
    private ApiClient apiClient;
    private TextView tvRole, tvStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiClient = ApiClient.get(this);
        rvMessages = findViewById(R.id.rvMessages);
        adapter = new MessageAdapter(messages);
        rvMessages.setAdapter(adapter);

        tvStats = findViewById(R.id.tvStats);
        tvRole = findViewById(R.id.tvRole);

        refreshStats();

        tvRole.setOnClickListener(v -> showRolePicker());

        findViewById(R.id.btnMenu).setOnClickListener(v -> showSettingsMenu());

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());

        // 未配置 key -> 自动跳设置
        if (TextUtils.isEmpty(apiClient.getApiKey())) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
    }

    private void refreshStats() {
        int today = apiClient.getTodayTokens();
        int total = apiClient.getTotalTokens();
        tvStats.setText(getString(R.string.role_today, String.valueOf(today), String.valueOf(total)));
    }

    private void showRolePicker() {
        List<Role> roles = Role.all(this);
        String[] names = new String[roles.size()];
        for (int i = 0; i < roles.size(); i++) names[i] = roles.get(i).emoji + " " + roles.get(i).name;
        new AlertDialog.Builder(this)
                .setTitle(R.string.roles)
                .setItems(names, (d, which) -> {
                    Role r = roles.get(which);
                    Role.setCurrentRoleId(this, r.id);
                    updateRoleLabel(r);
                })
                .show();
    }

    private void updateRoleLabel(Role r) {
        if (r != null) tvRole.setText(r.emoji + " " + r.name);
    }

    private void showSettingsMenu() {
        String[] items = {
            "⚙️  " + getString(R.string.roles),
            "🤖  " + getString(R.string.model_choose),
            "🌐  " + getString(R.string.api_settings),
            "🔑  " + getString(R.string.label_api_key)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_settings)
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0: startActivity(new Intent(this, RoleActivity.class)); break;
                        case 1: showModelPicker(); break;
                        case 2: startActivity(new Intent(this, SettingsActivity.class)); break;
                        case 3: startActivity(new Intent(this, SettingsActivity.class)); break;
                    }
                })
                .show();
    }

    private void showModelPicker() {
        List<String> models = apiClient.getModelsList();
        if (models.isEmpty()) {
            Toast.makeText(this, R.string.no_models, Toast.LENGTH_SHORT).show();
            return;
        }
        int selected = models.indexOf(apiClient.getSelectedModel());
        if (selected < 0) selected = 0;
        new AlertDialog.Builder(this)
                .setTitle(R.string.model_choose)
                .setSingleChoiceItems(models.toArray(new String[0]), selected, (d, which) -> {
                    apiClient.setSelectedModel(models.get(which));
                    d.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStats();
        updateRoleLabel(Role.find(this, Role.getCurrentRoleId(this)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_roles) {
            startActivity(new Intent(this, RoleActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendMessage() {
        EditText et = findViewById(R.id.etInput);
        String text = et.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        if (TextUtils.isEmpty(apiClient.getApiKey())) {
            Toast.makeText(this, R.string.error_no_key, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        ChatModels.Message userMsg = new ChatModels.Message("user", text);
        messages.add(userMsg);
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);
        et.setText("");

        ChatModels.ChatRequest req = new ChatModels.ChatRequest();
        String sel = apiClient.getSelectedModel();
        if (!TextUtils.isEmpty(sel)) {
            req.model = sel;
        }
        Role role = Role.find(this, Role.getCurrentRoleId(this));
        List<ChatModels.Message> sendMsgs = new ArrayList<>();
        if (role != null && !TextUtils.isEmpty(role.systemPrompt)) {
            sendMsgs.add(new ChatModels.Message("system", role.systemPrompt));
        }
        sendMsgs.addAll(messages);
        req.messages = sendMsgs;

        String auth = "Bearer " + apiClient.getApiKey();
        String url = apiClient.buildChatUrl();

        apiClient.getApi().chatCompletions(url, auth, req).enqueue(new Callback<ChatModels.ChatResponse>() {
            @Override
            public void onResponse(Call<ChatModels.ChatResponse> call, Response<ChatModels.ChatResponse> resp) {
                runOnUiThread(() -> {
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().choices.isEmpty()) {
                        ChatModels.Message reply = resp.body().choices.get(0).message;
                        messages.add(reply);
                        int idx = messages.size() - 1;
                        adapter.notifyItemInserted(idx);
                        rvMessages.scrollToPosition(idx);

                        if (resp.body().usage != null) {
                            int p = resp.body().usage.prompt_tokens;
                            int c = resp.body().usage.completion_tokens;
                            int t = resp.body().usage.total_tokens;
                            apiClient.addUsage(t);
                            adapter.setUsage(idx, getString(R.string.token_usage, p, c, t));
                            refreshStats();
                        }
                    } else {
                        showError("API Error: " + resp.code());
                    }
                });
            }
            @Override
            public void onFailure(Call<ChatModels.ChatResponse> call, Throwable t) {
                runOnUiThread(() -> showError("Network: " + t.getMessage()));
            }
        });
    }

    private void showError(String msg) {
        messages.add(new ChatModels.Message("system", "[Error] " + msg));
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);
    }
}