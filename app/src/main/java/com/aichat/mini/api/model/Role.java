package com.aichat.mini.api.model;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Role {
    public String id;
    public String name;
    public String emoji;
    public String systemPrompt;

    public Role(String id, String name, String emoji, String systemPrompt) {
        this.id = id;
        this.name = name;
        this.emoji = emoji;
        this.systemPrompt = systemPrompt;
    }

    private static final String PREFS = "config";
    private static final String KEY_CUSTOM_ROLES = "custom_roles";
    private static final String KEY_CURRENT_ROLE = "current_role";
    private static final String DEFAULT_ROLE_ID = "assistant";

    public static final List<Role> PRESET = new ArrayList<>();
    static {
        PRESET.add(new Role(DEFAULT_ROLE_ID, "通用助手", "🤖",
                "你是一个乐于助人的 AI 助手，回答简洁清晰、准确友好。"));
        PRESET.add(new Role("translator", "翻译官", "🌍",
                "你是一名专业翻译，把用户输入翻译成中文，保留原意与语气，必要时补充注释。"));
        PRESET.add(new Role("code", "代码专家", "💻",
                "你是一名资深程序员，精通 Java、Python 等语言。给出可运行的代码与简明解释。"));
        PRESET.add(new Role("writer", "写作助手", "✍️",
                "你是一名写作助手，善于撰写文章、文案、润色文字，语言优美、逻辑清晰。"));
        PRESET.add(new Role("teacher", "学习导师", "🎓",
                "你是一名耐心细致的学习导师，用通俗易懂的方式讲解知识，循序渐进，举一反三。"));
        PRESET.add(new Role("friend", "知心朋友", "💬",
                "你是一个温暖贴心的朋友，倾听用户心声，用平实的语言回应与陪伴。"));
        PRESET.add(new Role("poet", "诗人", "🪶",
                "你是一位诗人，用凝练优美、富有意象的语言作诗，注意韵律与意境。"));
    }

    public static List<Role> all(Context ctx) {
        List<Role> all = new ArrayList<>(PRESET);
        all.addAll(custom(ctx));
        return all;
    }

    public static List<Role> custom(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_CUSTOM_ROLES, "");
        if (json.isEmpty()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<Role>>(){}.getType();
            List<Role> list = new Gson().fromJson(json, type);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void saveCustom(Context ctx, List<Role> roles) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_CUSTOM_ROLES, new Gson().toJson(roles)).apply();
    }

    public static String getCurrentRoleId(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_CURRENT_ROLE, DEFAULT_ROLE_ID);
    }

    public static void setCurrentRoleId(Context ctx, String id) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_CURRENT_ROLE, id).apply();
    }

    public static Role find(Context ctx, String id) {
        for (Role r : all(ctx)) {
            if (r.id.equals(id)) return r;
        }
        return PRESET.get(0);
    }

    public static String newId() {
        return "c" + System.currentTimeMillis();
    }
}