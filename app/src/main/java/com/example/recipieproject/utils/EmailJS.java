package com.example.recipieproject.utils;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EmailJS {
    private static final String BASE_URL = "https://api.emailjs.com/api/v1.0/email/send";
    private static final OkHttpClient client = new OkHttpClient();
    private static String publicKey;

    public static void init(String key) {
        publicKey = key;
    }

    public static CompletableFuture<String> send(String serviceId, String templateId, 
                                                Map<String, String> templateParams, String privateKey) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("service_id", serviceId);
            jsonBody.put("template_id", templateId);
            jsonBody.put("user_id", publicKey);
            jsonBody.put("accessToken", privateKey);
            jsonBody.put("template_params", new JSONObject(templateParams));

            RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(BASE_URL)
                .post(body)
                .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    if (response.isSuccessful()) {
                        future.complete("Email sent successfully");
                    } else {
                        future.completeExceptionally(new RuntimeException("Failed to send email: " + response.code()));
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }
} 