package com.example.recipieproject.services;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import okhttp3.*;
import org.json.JSONObject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AIService {
    private static final String GEMINI_API_KEY = "AIzaSyBswCA-tmUntZ_Yj8SHCjyQ5UyuJ3HPBoI";
    private static final String HF_API_KEY = "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";
    private static final String HF_API_URL = "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-3-medium-diffusers";
    
    private final GenerativeModel geminiModel;
    private final OkHttpClient client;

    public AIService() {
        geminiModel = new GenerativeModel("gemini-pro", GEMINI_API_KEY);
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    public CompletableFuture<String> predictIngredients(String recipeName) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        System.out.println("AIService: Starting prediction for " + recipeName); // Debug log
        
        String prompt = "You are a cooking assistant. For '" + recipeName + 
                       "', list ONLY the basic ingredients needed. " +
                       "Format: simple comma-separated list. " +
                       "Example output: flour, eggs, milk, sugar";
        
        try {
            Content content = new Content.Builder()
                .addText(prompt)
                .build();

            System.out.println("AIService: Sending request to Gemini"); // Debug log
            
            geminiModel.generateContent(new Content[]{content}, new Continuation<GenerateContentResponse>() {
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(Object result) {
                    try {
                        System.out.println("AIService: Received response from Gemini"); // Debug log
                        if (result instanceof GenerateContentResponse) {
                            GenerateContentResponse response = (GenerateContentResponse) result;
                            String ingredients = response.getText().trim()
                                .replaceAll("\n", ", ")
                                .replaceAll("\\s*,\\s*", ", ")
                                .replaceAll("^[•\\-\\d\\.\\s]+", "")
                                .replaceAll("ingredients:|Ingredients:", "");
                            
                            System.out.println("AIService: Processed ingredients: " + ingredients); // Debug log
                            
                            if (!ingredients.isEmpty()) {
                                future.complete(ingredients);
                            } else {
                                future.completeExceptionally(new RuntimeException("Empty response from AI"));
                            }
                        } else {
                            System.out.println("AIService: Invalid response type: " + result.getClass()); // Debug log
                            future.completeExceptionally(new RuntimeException("Failed to predict ingredients"));
                        }
                    } catch (Exception e) {
                        System.out.println("AIService: Error processing response: " + e.getMessage()); // Debug log
                        future.completeExceptionally(e);
                    }
                }
            });
        } catch (Exception e) {
            System.out.println("AIService: Error sending request: " + e.getMessage()); // Debug log
            future.completeExceptionally(e);
        }
        
        return future;
    }

    public CompletableFuture<String> generateDescription(String ingredients) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            String prompt = "Create a cooking recipe using ONLY these ingredients: " + ingredients + 
                           "\n\nProvide your response in this EXACT format:" +
                           "\n1. Brief description (2-3 sentences)" +
                           "\n2. Step-by-step cooking instructions (numbered steps)" +
                           "\n3. Cooking time and difficulty level" +
                           "\n\nKeep it clear and concise.";
            
            Content content = new Content.Builder()
                .addText(prompt)
                .build();

            System.out.println("AIService: Sending description request for ingredients: " + ingredients);

            geminiModel.generateContent(new Content[]{content}, new Continuation<GenerateContentResponse>() {
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(Object result) {
                    try {
                        if (result instanceof GenerateContentResponse) {
                            GenerateContentResponse response = (GenerateContentResponse) result;
                            String description = response.getText().trim();
                            System.out.println("AIService: Received description: " + description);
                            
                            if (!description.isEmpty()) {
                                future.complete(description);
                            } else {
                                future.completeExceptionally(new RuntimeException("Empty response from AI"));
                            }
                        } else {
                            System.out.println("AIService: Invalid response type: " + result.getClass());
                            future.completeExceptionally(new RuntimeException("Failed to generate description"));
                        }
                    } catch (Exception e) {
                        System.out.println("AIService: Error processing description: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
            });
        } catch (Exception e) {
            System.out.println("AIService: Error creating description request: " + e.getMessage());
            future.completeExceptionally(e);
        }
        
        return future;
    }

    public CompletableFuture<Bitmap> generateImage(String ingredients) {
        CompletableFuture<Bitmap> future = new CompletableFuture<>();
        
        try {
            String prompt = "A professional food photography of a delicious dish made with these ingredients: " + ingredients;
            MediaType JSON = MediaType.get("application/json");
            JSONObject jsonBody = new JSONObject()
                .put("inputs", prompt)
                .put("wait_for_model", true)
                .put("parameters", new JSONObject()
                    .put("width", 1024)
                    .put("height", 1024)
                    .put("num_inference_steps", 50)
                    .put("guidance_scale", 7.5));

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

            Request request = new Request.Builder()
                .url(HF_API_URL)
                .addHeader("Authorization", "Bearer " + HF_API_KEY)
                .post(body)
                .build();

            System.out.println("AIService: Sending image request with prompt: " + prompt);
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    try {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "";
                            System.out.println("AIService: Error response: " + errorBody);
                            future.completeExceptionally(new RuntimeException("Failed: " + response.code() + " - " + errorBody));
                            return;
                        }

                        byte[] imageBytes = response.body().bytes();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        
                        if (bitmap != null) {
                            System.out.println("AIService: Image generated successfully");
                            future.complete(bitmap);
                        } else {
                            future.completeExceptionally(new RuntimeException("Failed to decode image"));
                        }
                    } catch (Exception e) {
                        System.out.println("AIService: Error processing image: " + e.getMessage());
                        future.completeExceptionally(e);
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }

                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    System.out.println("AIService: Network error: " + e.getMessage());
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            System.out.println("AIService: Error creating request: " + e.getMessage());
            future.completeExceptionally(e);
        }
        
        return future;
    }

    public CompletableFuture<String> generateRandomTitles() {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        String prompt = "Generate 10 unique and creative recipe names. Mix different cuisines and types. " +
                       "Format: numbered list 1-10, one per line. Be creative and specific. " +
                       "Example: 1. Moroccan Spiced Lamb Tagine with Apricots";
        
        Content content = new Content.Builder()
            .addText(prompt)
            .build();

        geminiModel.generateContent(new Content[]{content}, new Continuation<GenerateContentResponse>() {
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(Object result) {
                try {
                    if (result instanceof GenerateContentResponse) {
                        GenerateContentResponse response = (GenerateContentResponse) result;
                        String titles = response.getText().trim();
                        if (!titles.isEmpty()) {
                            future.complete(titles);
                        } else {
                            future.completeExceptionally(new RuntimeException("Empty response from AI"));
                        }
                    } else {
                        future.completeExceptionally(new RuntimeException("Failed to generate titles"));
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });
        
        return future;
    }
} 