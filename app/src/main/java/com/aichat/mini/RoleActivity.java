package com.aichat.mini;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.aichat.mini.api.model.Role;
import java.util.ArrayList;
import java.util.List;

public class RoleActivity extends AppCompatActivity {
    private RecyclerView rvRoles;
    private RoleAdapter adapter;
    private List<Role> allRoles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role);
        setTitle(R.string.roles);

        rvRoles = findViewById(R.id.rvRoles);
        rvRoles.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnNewRole).setOnClickListener(v -> showEditDialog(null));

        reload();
    }

    private void reload() {
        allRoles = Role.all(this);
        adapter = new RoleAdapter(this, allRoles);
        rvRoles.setAdapter(adapter);
    }

    private void showEditDialog(Role role) {
        boolean isEdit = role != null;
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_role_edit, null);
        EditText etName = form.findViewById(R.id.etRoleName);
        EditText etPrompt = form.findViewById(R.id.etRolePrompt);
        if (isEdit) {
            etName.setText(role.name);
            etPrompt.setText(role.systemPrompt);
        }
        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "编辑角色" : getString(R.string.new_role))
                .setView(form)
                .setPositiveButton("保存", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String prompt = etPrompt.getText().toString().trim();
                    if (name.isEmpty()) { Toast.makeText(this, "请输入角色名称", Toast.LENGTH_SHORT).show(); return; }
                    List<Role> custom = Role.custom(this);
                    if (isEdit) {
                        for (Role r : custom) {
                            if (r.id.equals(role.id)) { r.name = name; r.systemPrompt = prompt; }
                        }
                    } else {
                        custom.add(new Role(Role.newId(), name, "🧩", prompt));
                    }
                    Role.saveCustom(this, custom);
                    reload();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showRoleMenu(Role role) {
        boolean custom = role.id.startsWith("c");
        new AlertDialog.Builder(this)
                .setTitle(role.emoji + " " + role.name)
                .setItems(custom ? new String[]{"编辑", getString(R.string.delete_role)} : new String[]{"编辑"}, (d, w) -> {
                    if (w == 0) showEditDialog(role);
                    else if (w == 1 && custom) {
                        List<Role> customList = Role.custom(this);
                        customList.removeIf(r -> r.id.equals(role.id));
                        Role.saveCustom(this, customList);
                        reload();
                    }
                })
                .show();
    }

    static class RoleAdapter extends RecyclerView.Adapter<RoleAdapter.VH> {
        private final List<Role> roles;
        private final RoleActivity act;

        RoleAdapter(RoleActivity act, List<Role> roles) { this.act = act; this.roles = roles; }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_role, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Role r = roles.get(pos);
            h.emoji.setText(r.emoji);
            h.name.setText(r.name);
            h.prompt.setText(r.systemPrompt);
            h.itemView.setOnClickListener(v -> {
                Role.setCurrentRoleId(act, r.id);
                Toast.makeText(act, "已选择 " + r.name, Toast.LENGTH_SHORT).show();
                act.finish();
            });
            h.itemView.setOnLongClickListener(v -> { act.showRoleMenu(r); return true; });
        }
        @Override public int getItemCount() { return roles.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView emoji, name, prompt;
            VH(View v) { super(v); emoji = v.findViewById(R.id.tvRoleEmoji); name = v.findViewById(R.id.tvRoleName); prompt = v.findViewById(R.id.tvRolePrompt); }
        }
    }
}