package com.aichat.mini;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.aichat.mini.api.ApiClient;
import com.aichat.mini.api.model.ChatModels;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        apiClient = ApiClient.get(this);
        rvMessages = findViewById(R.id.rvMessages);
        adapter = new MessageAdapter(messages);
        rvMessages.setAdapter(adapter);

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
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
        return super.onOptionsItemSelected(item);
    }

    private void sendMessage() {
        android.widget.EditText et = findViewById(R.id.etInput);
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
        req.messages = new ArrayList<>(messages);
        
        String auth = "Bearer " + apiClient.getApiKey();
        String url = apiClient.buildChatUrl();

        apiClient.getApi().chatCompletions(url, auth, req).enqueue(new Callback<ChatModels.ChatResponse>() {
            @Override
            public void onResponse(Call<ChatModels.ChatResponse> call, Response<ChatModels.ChatResponse> resp) {
                runOnUiThread(() -> {
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().choices.isEmpty()) {
                        ChatModels.Message reply = resp.body().choices.get(0).message;
                        messages.add(reply);
                        adapter.notifyItemInserted(messages.size() - 1);
                        rvMessages.scrollToPosition(messages.size() - 1);
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