package com.aichat.mini.api;

import android.content.Context;
import android.content.SharedPreferences;
import com.aichat.mini.api.model.ChatModels;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String PREFS = "config";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_BASE_URL = "base_url";
    private static final String DEFAULT_BASE_URL = "https://api.hcnsec.cn";
    private static final String KEY_MODELS = "models";
    private static final String KEY_TOTAL_TOKENS = "total_tokens";
    private static final String KEY_TODAY_TOKENS = "today_tokens";
    private static final String KEY_TODAY_DATE = "today_date";

    private static ApiClient instance;
    private final OpenAiApi api;
    private final SharedPreferences prefs;
    private final OkHttpClient client;

    private ApiClient(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
        api = retrofit.create(OpenAiApi.class);
    }

    public static synchronized ApiClient get(Context ctx) {
        if (instance == null) instance = new ApiClient(ctx.getApplicationContext());
        return instance;
    }

    public OpenAiApi getApi() { return api; }
    public String getApiKey() { return prefs.getString(KEY_API_KEY, ""); }
    public void setApiKey(String key) { prefs.edit().putString(KEY_API_KEY, key).apply(); }
    public String getBaseUrl() {
        String url = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
        return url.endsWith("/") ? url : url + "/";
    }
    public void setBaseUrl(String url) { prefs.edit().putString(KEY_BASE_URL, url).apply(); }
    public String buildChatUrl() { return getBaseUrl() + "v1/chat/completions"; }
    public String buildModelsUrl() { return getBaseUrl() + "v1/models"; }

    public interface ModelsCallback {
        void onSuccess(List<String> models);
        void onFailure(String error);
    }

    public void fetchModels(ModelsCallback cb) {
        getApi().models(buildModelsUrl(), "Bearer " + getApiKey())
            .enqueue(new Callback<ChatModels.ModelList>() {
                @Override public void onResponse(Call<ChatModels.ModelList> call, Response<ChatModels.ModelList> resp) {
                    if (resp.isSuccessful() && resp.body() != null && resp.body().data != null) {
                        List<String> ids = new ArrayList<>();
                        for (ChatModels.Model m : resp.body().data) ids.add(m.id);
                        setModelsList(ids);
                        if (cb != null) cb.onSuccess(ids);
                    } else if (cb != null) {
                        cb.onFailure("HTTP " + resp.code());
                    }
                }
                @Override public void onFailure(Call<ChatModels.ModelList> call, Throwable t) {
                    if (cb != null) cb.onFailure(t.getMessage());
                }
            });
    }

    public List<String> getModelsList() {
        List<String> list = new ArrayList<>();
        String json = prefs.getString(KEY_MODELS, "");
        if (json == null || json.isEmpty()) return list;
        try {
            com.google.gson.reflect.TypeToken<java.util.List<String>> tt =
                new com.google.gson.reflect.TypeToken<java.util.List<String>>(){};
            list = new com.google.gson.Gson().fromJson(json, tt.getType());
        } catch (Exception ignored) {}
        return list == null ? new ArrayList<>() : list;
    }

    public void setModelsList(List<String> ids) {
        prefs.edit().putString(KEY_MODELS, new com.google.gson.Gson().toJson(ids)).apply();
    }

    public void addUsage(int tokens) {
        if (tokens <= 0) return;
        int total = prefs.getInt(KEY_TOTAL_TOKENS, 0) + tokens;
        prefs.edit().putInt(KEY_TOTAL_TOKENS, total).apply();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String saved = prefs.getString(KEY_TODAY_DATE, "");
        int todayTokens;
        if (saved.equals(today)) {
            todayTokens = prefs.getInt(KEY_TODAY_TOKENS, 0) + tokens;
        } else {
            todayTokens = tokens;
            prefs.edit().putString(KEY_TODAY_DATE, today).apply();
        }
        prefs.edit().putInt(KEY_TODAY_TOKENS, todayTokens).apply();
    }

    public int getTotalTokens() { return prefs.getInt(KEY_TOTAL_TOKENS, 0); }
    public int getTodayTokens() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String saved = prefs.getString(KEY_TODAY_DATE, "");
        if (!saved.equals(today)) return 0;
        return prefs.getInt(KEY_TODAY_TOKENS, 0);
    }
}