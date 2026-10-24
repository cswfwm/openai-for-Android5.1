package com.aichat.mini.ui;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aichat.mini.R;
import com.aichat.mini.api.model.ChatModels;
import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {
    private final List<ChatModels.Message> data;
    private final List<String> usageText = new ArrayList<>();

    public MessageAdapter(List<ChatModels.Message> data) { this.data = data; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        ChatModels.Message m = data.get(pos);
        h.tvContent.setText(m.content);
        String role = m.role.equals("user") ? "You" : (m.role.equals("assistant") ? "AI" : "System");
        h.tvRole.setText(role);
        String u = pos < usageText.size() ? usageText.get(pos) : null;
        if (!TextUtils.isEmpty(u)) {
            h.tvUsage.setText(u);
            h.tvUsage.setVisibility(View.VISIBLE);
        } else {
            h.tvUsage.setVisibility(View.GONE);
        }
    }
    @Override public int getItemCount() { return data.size(); }

    public void setUsage(int pos, String text) {
        while (usageText.size() <= pos) usageText.add(null);
        usageText.set(pos, text);
        notifyItemChanged(pos);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvRole, tvContent, tvUsage;
        VH(View v) { super(v); tvRole = v.findViewById(R.id.tvRole); tvContent = v.findViewById(R.id.tvContent); tvUsage = v.findViewById(R.id.tvUsage); }
    }
}